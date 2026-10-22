package com.example.instagramclone.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.instagramclone.ui.activity.ActivityFragment
import com.example.instagramclone.ui.profile.ProfileFragment
import com.example.instagramclone.R
import com.example.instagramclone.ui.search.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigation = findViewById(R.id.bottomNavigation)


        bottomNavigation.isItemActiveIndicatorEnabled = false


        val selectedTab = savedInstanceState?.getInt(
            "selected_bottom_tab",
            R.id.nav_home
        ) ?: R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->

            val currentFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainer)

            val selectedFragmentClass = when (item.itemId) {
                R.id.nav_home -> HomeFragment::class.java
                R.id.nav_search -> SearchFragment::class.java
                R.id.nav_create -> CreateFragment::class.java
                R.id.nav_activity -> ActivityFragment::class.java
                R.id.nav_profile -> ProfileFragment::class.java
                else -> HomeFragment::class.java
            }


            if (currentFragment == null ||
                currentFragment::class.java != selectedFragmentClass
            ) {
                val selectedFragment: Fragment = when (item.itemId) {
                    R.id.nav_home -> HomeFragment()
                    R.id.nav_search -> SearchFragment()
                    R.id.nav_create -> CreateFragment()
                    R.id.nav_activity -> ActivityFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> HomeFragment()
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit()
            }

            true
        }

        bottomNavigation.selectedItemId = selectedTab
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(
            "selected_bottom_tab",
            bottomNavigation.selectedItemId
        )

        super.onSaveInstanceState(outState)
    }
}
