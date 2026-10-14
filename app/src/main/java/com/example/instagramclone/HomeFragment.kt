
package com.example.instagramclone

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val textView = TextView(requireContext())

        textView.text = "Home"
        textView.textSize = 24f
        textView.gravity = android.view.Gravity.CENTER

        return textView
    }
}
