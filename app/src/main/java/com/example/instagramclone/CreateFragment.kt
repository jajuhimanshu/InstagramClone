
package com.example.instagramclone

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
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

    // Open the Android document picker for images.
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
        try {
            val appContext = requireContext()

            // Create a unique file in the app's private cache.
            val imageFile = File(
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
                imageFile.delete()
                throw IOException("The selected image file is empty.")
            }

            // Decode the local copy, not the original gallery URI.
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

            if (bitmap == null) {
                imageFile.delete()
                throw IOException("The selected file is not a readable image.")
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
            selectedImageFile = null

            showMessage(
                "Could not read image: " +
                        "${exception.javaClass.simpleName}: " +
                        "${exception.message ?: "Unknown error"}"
            )
        }
    }

    private fun sharePost() {
        if (isUploading) return

        val imageFile = selectedImageFile

        if (imageFile == null || !imageFile.exists() || imageFile.length() == 0L) {
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

        // First save the image in Firebase Realtime Database.
        imageStorageRepository.uploadImage(
            imageFile = imageFile,
            postId = postId,

            onSuccess = {
                if (!isAdded) return@uploadImage

                val post = Post(
                    postId = postId,
                    authorId = currentUser.uid,
                    // This is the key used to find the image in Realtime Database.
                    imageUrl = postId,
                    caption = captionInput.text.toString().trim(),
                    createdAt = System.currentTimeMillis(),
                    likeCount = 0L,
                    commentCount = 0L
                )

                // Save the post metadata only after the image upload succeeds.
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
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.GRAY
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
        // Do not delete the selected file here: an upload may still be running.
        super.onDestroyView()
    }
}
