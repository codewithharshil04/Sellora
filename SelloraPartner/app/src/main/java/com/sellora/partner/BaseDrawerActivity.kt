package com.sellora.partner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.view.View
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.sellora.partner.repositories.AuthRepository

/**
 * Base activity to handle common navigation drawer logic across different screens.
 * This ensures a consistent navigation experience and reduces code duplication.
 */
abstract class BaseDrawerActivity : AppCompatActivity() {

    protected val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    abstract val drawerLayout: DrawerLayout
    abstract val closeDrawerButton: View
    abstract val navDashboard: View
    abstract val navAddService: View
    abstract val navProjects: View
    abstract val navProfile: View
    abstract val navLogout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBaseDrawer() {
        closeDrawerButton.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.END) }
        
        navDashboard.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            if (this !is DashboardActivity) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }

        navAddService.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, AddServiceActivity::class.java))
        }

        navProjects.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            if (this !is ProjectsActivity) {
                startActivity(Intent(this, ProjectsActivity::class.java))
            }
        }

        navProfile.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        navLogout.setOnClickListener {
            AuthRepository().logout()
            getSharedPreferences("sellora_partner_auth", MODE_PRIVATE).edit { clear() }
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    /**
     * Tints the drawer icons to white for better visibility on the primary color background.
     * Needed in code because some vector drawables don't support XML tinting reliably across API levels.
     */
    protected fun tintDrawerIcons(vararg views: View) {
        val white = android.graphics.Color.WHITE
        views.filterIsInstance<TextView>().forEach { tv ->
            tv.compoundDrawablesRelative.filterNotNull().forEach { it.setTint(white) }
        }
    }
}
