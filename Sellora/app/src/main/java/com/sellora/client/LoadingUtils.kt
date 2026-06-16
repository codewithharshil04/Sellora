package com.sellora.client

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible

/**
 * Utility class for loading states and micro-interactions
 * Provides consistent loading animations and state management
 */
object LoadingUtils {
    
    // Show loading state on a view
    fun showLoading(view: View, loadingText: String = "Loading...") {
        // Stop any previous animations
        hideLoading(view)

        view.alpha = 0.5f
        view.isEnabled = false
    }
    
    // Hide loading state on a view
    fun hideLoading(view: View) {
        view.animate().cancel() // This stops ViewPropertyAnimator
        view.alpha = 1.0f
        view.isEnabled = true
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        
        // Clear legacy animations if any
        view.clearAnimation()
    }
    
    // Fade in animation
    fun fadeIn(view: View, duration: Long = 300) {
        view.alpha = 0f
        view.isVisible = true
        
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    // Fade out animation
    fun fadeOut(view: View, duration: Long = 300) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { view.isVisible = false }
            .start()
    }
    
    // Scale animation for button press
    fun scalePress(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    // Scale animation for button release
    fun scaleRelease(view: View) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    // Shake animation for error
    fun shakeError(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 0f, 10f, 0f)
        animator.duration = 500
        animator.repeatCount = 2
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }
}
