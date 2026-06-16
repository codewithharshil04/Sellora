package com.sellora.admin

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {

    fun setupBottomNavigation(
        activity: Activity,
        bottomNav: BottomNavigationView,
        currentId: Int
    ) {
        bottomNav.selectedItemId = currentId
        bottomNav.setOnItemSelectedListener { item ->
            val targetClass = when (item.itemId) {
                R.id.nav_dashboard -> DashboardActivity::class.java
                R.id.nav_orders -> OrdersActivity::class.java
                R.id.nav_services -> ServicesActivity::class.java
                R.id.nav_users -> UsersActivity::class.java
                else -> null
            }

            if (targetClass != null && activity::class.java == targetClass) return@setOnItemSelectedListener true

            targetClass?.let {
                val intent = Intent(activity, it)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
                false
            } ?: false
        }
    }

    fun navigateTo(activity: Activity, target: Class<*>) {
        val intent = Intent(activity, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        activity.startActivity(intent)
    }
}
