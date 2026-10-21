package com.example.instagramclone

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.core.content.ContextCompat

class EditProfileFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etDisplayName: EditText
    private lateinit var etBio: EditText
    private lateinit var btnSave: Button
    private lateinit var ivProfileImage: ImageView

    private var profileImageBase64 = ""

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_edit__profile, container, false)

        etUsername = view.findViewById(R.id.etUsername)
        etDisplayName = view.findViewById(R.id.etDisplayName)
        etBio = view.findViewById(R.id.etBio)
        btnSave = view.findViewById(R.id.btnSave)
        ivProfileImage = view.findViewById(R.id.ivProfileImage)

        loadUserData()

        btnSave.setOnClickListener {
            saveProfile()
        }

        ivProfileImage.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Profile Photo")
                .setItems(
                    arrayOf("Take Photo", "Choose From Gallery")
                ) { _, which ->

                    when (which) {
                        0 -> { if (
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                             } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                        }
                        1 -> { galleryLauncher.launch("image/*") }
                    }
                }.show()
        }

        return view
    }

    private fun loadUserData() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid).get()
            .addOnSuccessListener { document ->

                val user = document.toObject(User::class.java)

                user?.let {

                    etUsername.setText(it.username)
                    etDisplayName.setText(it.displayName)
                    etBio.setText(it.bio)
                    profileImageBase64 = it.profileImageUrl
                    if (it.profileImageUrl.isNotEmpty()){
                        ivProfileImage.setImageBitmap(base64ToBitmap(it.profileImageUrl))
                    }
                }
            }
    }

    private fun saveProfile() {

        val username = etUsername.text.toString().trim()
        val displayName = etDisplayName.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Enter Username"
            return
        }

        if (displayName.isEmpty()) {
            etDisplayName.error = "Enter Display Name"
            return
        }

        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "username" to username,
            "usernameLowercase" to username.lowercase(),
            "displayName" to displayName,
            "bio" to bio,
            "profileImageUrl" to profileImageBase64
        )

        firestore.collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }


    private val cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            bitmap?.let {
                ivProfileImage.setImageBitmap(it)
                profileImageBase64 = bitmapToBase64(it)
            }
        }

    private val cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(requireContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                ivProfileImage.setImageURI(uri)
                profileImageBase64 = imageUriToBase64(uri)
            }
    }

    private fun imageUriToBase64(uri: Uri): String {
        var inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}