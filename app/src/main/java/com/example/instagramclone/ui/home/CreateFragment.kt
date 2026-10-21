package com.example.instagramclone.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.instagramclone.R
import com.example.instagramclone.data.model.Post
import com.example.instagramclone.data.repository.FirestoreRepository
import com.example.instagramclone.data.repository.ImageStorageRepository

class CreateFragment : Fragment(R.layout.fragment_create) {

    private lateinit var imagePreview: ImageView
    private lateinit var captionInput: EditText
    private lateinit var selectPhotoButton: Button
    private lateinit var shareTopButton: TextView
    private lateinit var sharePostButton: Button
    private lateinit var selectPhotoHint: TextView

    private var selectedImageFile: File? = null
    private var isUploading = false

    private val imageStorageRepository = ImageStorageRepository()
    private val firestoreRepository = FirestoreRepository()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null || !isAdded) {
                return@registerForActivityResult
            }

            copyImageToCache(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagePreview = view.findViewById(R.id.ivSelectedImage)
        captionInput = view.findViewById(R.id.etCaption)
        selectPhotoButton = view.findViewById(R.id.btnSelectPhoto)
        shareTopButton = view.findViewById(R.id.btnShareTop)
        sharePostButton = view.findViewById(R.id.btnSharePost)
        selectPhotoHint = view.findViewById(R.id.tvSelectPhotoHint)


        val root = view.findViewById<View>(R.id.createRoot)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val topInset = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            v.setPadding(
                v.paddingLeft,
                topInset,
                v.paddingRight,
                v.paddingBottom
            )

            insets
        }

        selectPhotoButton.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        shareTopButton.setOnClickListener {
            sharePost()
        }

        sharePostButton.setOnClickListener {
            sharePost()
        }

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun copyImageToCache(sourceUri: Uri) {
        var imageFile: File? = null

        try {
            val appContext = requireContext()

            imageFile = File(
                appContext.cacheDir,
                "selected_post_${System.currentTimeMillis()}.jpg"
            )

            val inputStream = appContext.contentResolver
                .openInputStream(sourceUri)
                ?: throw IOException("Android returned no image stream.")

            inputStream.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!imageFile.exists() || imageFile.length() == 0L) {
                throw IOException("The selected image file is empty.")
            }

            val bitmap = decodeCorrectlyOrientedImage(imageFile)
                ?: throw IOException("The selected file is not a readable image.")


            File(imageFile.absolutePath).outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                    throw IOException("Could not save the corrected image.")
                }
            }

            selectedImageFile?.let { previousFile ->
                if (previousFile != imageFile) {
                    previousFile.delete()
                }
            }

            selectedImageFile = imageFile

            imagePreview.setImageBitmap(bitmap)
            imagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
            imagePreview.setPadding(0, 0, 0, 0)
            imagePreview.imageTintList = null

            selectPhotoHint.visibility = View.GONE
            selectPhotoButton.text = "Change photo"

        } catch (exception: Exception) {
            imageFile?.delete()

            showMessage(
                "Could not read image: " +
                        "${exception.javaClass.simpleName}: " +
                        "${exception.message ?: "Unknown error"}"
            )
        }
    }

    private fun decodeCorrectlyOrientedImage(file: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: return null

        val orientation = ExifInterface(file.absolutePath)
            .getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.postRotate(90f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.postRotate(180f)

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.postRotate(270f)

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.preScale(-1f, 1f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                matrix.preScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
        }

        return if (matrix.isIdentity) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            ).also { correctedBitmap ->
                bitmap.recycle()
            }
        }
    }

    private fun sharePost() {
        if (isUploading) return

        val imageFile = selectedImageFile

        if (imageFile == null ||
            !imageFile.exists() ||
            imageFile.length() == 0L
        ) {
            showMessage("Please select a photo from the gallery first.")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            showMessage("Please log in before creating a post.")
            return
        }

        val appContext = context ?: return

        val inputManager = appContext.getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        inputManager.hideSoftInputFromWindow(captionInput.windowToken, 0)

        isUploading = true
        setShareButtonsEnabled(false)

        val postId = FirebaseFirestore.getInstance()
            .collection("posts")
            .document()
            .id

        imageStorageRepository.uploadImage(
            imageFile = imageFile,
            postId = postId,

            onSuccess = {
                if (!isAdded) return@uploadImage

                val post = Post(
                    postId = postId,
                    authorId = currentUser.uid,
                    imageUrl = postId,
                    caption = captionInput.text.toString().trim(),
                    createdAt = System.currentTimeMillis(),
                    likeCount = 0L,
                    commentCount = 0L
                )

                firestoreRepository.savePost(
                    post = post,

                    onSuccess = {
                        if (!isAdded) return@savePost

                        showMessage("Post shared successfully!")

                        selectedImageFile?.delete()
                        selectedImageFile = null

                        captionInput.text.clear()
                        resetImagePreview()

                        isUploading = false
                        setShareButtonsEnabled(true)
                    },

                    onFailure = { exception ->
                        if (!isAdded) return@savePost

                        isUploading = false
                        setShareButtonsEnabled(true)

                        showMessage(
                            "Image uploaded, but post details could not be saved: " +
                                    (exception.message ?: "Unknown error")
                        )
                    }
                )
            },

            onFailure = { exception ->
                if (!isAdded) return@uploadImage

                isUploading = false
                setShareButtonsEnabled(true)

                showMessage(
                    "Image upload failed: " +
                            "${exception.javaClass.simpleName}: " +
                            "${exception.message ?: "Unknown error"}"
                )
            }
        )
    }

    private fun resetImagePreview() {
        imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
        imagePreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imagePreview.setPadding(dp(90), dp(90), dp(90), dp(90))

        imagePreview.imageTintList =
            ColorStateList.valueOf(
                Color.GRAY
            )

        selectPhotoHint.visibility = View.VISIBLE
        selectPhotoButton.text = "Select photo from gallery"
    }

    private fun setShareButtonsEnabled(enabled: Boolean) {
        shareTopButton.isEnabled = enabled
        sharePostButton.isEnabled = enabled
        selectPhotoButton.isEnabled = enabled

        shareTopButton.text = if (enabled) "Share" else "Uploading..."
        sharePostButton.text = if (enabled) "Share post" else "Uploading..."
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showMessage(message: String) {
        context?.let { currentContext ->
            Toast.makeText(
                currentContext,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()
    }
}
