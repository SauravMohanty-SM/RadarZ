package com.barracudas.radarz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.etebarian.meowbottomnavigation.MeowBottomNavigation

class MainActivityForUser : AppCompatActivity() {

    private lateinit var bottomNavigation: MeowBottomNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_for_user)


        initialization()
        addBottomIcon()

        bottomNavigation.setOnShowListener {
            var fragment: Fragment? = null

            when (it.id) {
                1 -> ChatFragment().also { fragment = it }
                2 -> GroupFragment().also { fragment = it }
                3 -> SearchOnMapFragment().also { fragment = it }
                4 -> ProfileFragment().also { fragment = it }
            }

            loadFragment(fragment)
        }

        bottomNavigation.show(1, true)

        bottomNavigation.setOnClickMenuListener {

    }
        bottomNavigation.setOnReselectListener {

        }
    }

    private fun initialization() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }
    private fun addBottomIcon() {
        bottomNavigation.add(MeowBottomNavigation.Model(1, R.drawable.ic_chat))
        bottomNavigation.add(MeowBottomNavigation.Model(2, R.drawable.ic_group))
        bottomNavigation.add(MeowBottomNavigation.Model(3, R.drawable.ic_location))
        bottomNavigation.add(MeowBottomNavigation.Model(4, R.drawable.ic_person))
    }
    private fun loadFragment(fragment: Fragment?) {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit()
        }
    }
}