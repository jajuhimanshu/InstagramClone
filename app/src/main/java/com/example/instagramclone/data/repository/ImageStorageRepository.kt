package com.example.instagramclone.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.File

class ImageStorageRepository {

    private val database = FirebaseDatabase
        .getInstance(
            "https://instagramclone-3fbc0-default-rtdb.firebaseio.com"
        )
        .reference

    fun uploadImage(
        imageFile: File,
        postId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("No signed-in user.")

            if (!imageFile.exists() || imageFile.length() == 0L) {
                throw Exception("The cached image does not exist or is empty.")
            }


            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(imageFile.absolutePath, bounds)

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw Exception("The selected file is not a valid image.")
            }


            var sampleSize = 1

            while (
                bounds.outWidth / sampleSize > 900 ||
                bounds.outHeight / sampleSize > 900
            ) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val bitmap = BitmapFactory.decodeFile(
                imageFile.absolutePath,
                options
            ) ?: throw Exception("Could not decode the cached image.")

            val output = ByteArrayOutputStream()

            try {
                val compressed = bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    60,
                    output
                )

                if (!compressed || output.size() == 0) {
                    throw Exception("Could not compress the image.")
                }
            } finally {
                bitmap.recycle()
            }

            val base64Image = Base64.encodeToString(
                output.toByteArray(),
                Base64.NO_WRAP
            )


            if (base64Image.length > 9_500_000) {
                throw Exception(
                    "Image is too large. Please choose a smaller photo."
                )
            }


            database.child("postImages")
                .child(uid)
                .child(postId)
                .setValue(base64Image)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }

        } catch (exception: Exception) {
            onFailure(exception)
        }
    }
}
