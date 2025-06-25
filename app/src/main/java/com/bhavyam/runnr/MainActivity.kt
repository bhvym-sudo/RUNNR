package com.bhavyam.runnr

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bhavyam.runnr.player.PlayerManager
import com.bhavyam.runnr.service.MusicService
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var currentFragmentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        PlayerManager.initController(this) {}



        if (savedInstanceState == null) {
            switchFragment("home", HomeFragment())
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            hideFullPlayer()
            when (item.itemId) {
                R.id.nav_home -> switchFragment("home", HomeFragment())
                R.id.nav_search -> switchFragment("search", SearchFragment())
                R.id.nav_library -> switchFragment("library", LibraryFragment())
            }
            true
        }

        val playerBar = findViewById<View>(R.id.playerBar)
        playerBar.setOnClickListener {
            showFullPlayer()
        }
    }

    private fun switchFragment(tag: String, fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        val currentFragment = currentFragmentTag?.let {
            supportFragmentManager.findFragmentByTag(it)
        }

        if (currentFragment != null && currentFragment.isVisible) {
            transaction.hide(currentFragment)
        }

        if (existingFragment != null) {
            transaction.show(existingFragment)
        } else {
            transaction.add(R.id.fragment_container, fragment, tag)
        }

        currentFragmentTag = tag
        transaction.commit()
    }

    fun showFullPlayer() {
        findViewById<View>(R.id.fullPlayerContainer).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fullPlayerContainer, FullPlayerFragment())
            .commitNowAllowingStateLoss()
    }

    fun hideFullPlayer() {
        val container = findViewById<View>(R.id.fullPlayerContainer)
        val fullPlayer = supportFragmentManager.findFragmentById(R.id.fullPlayerContainer)
        if (fullPlayer != null) {
            supportFragmentManager.beginTransaction()
                .remove(fullPlayer)
                .commitNowAllowingStateLoss()
        }
        container.visibility = View.GONE
    }

    override fun onBackPressed() {
        val fullPlayer = supportFragmentManager.findFragmentById(R.id.fullPlayerContainer)
        if (fullPlayer is FullPlayerFragment) {
            hideFullPlayer()
        } else {
            super.onBackPressed()
        }
    }
}
