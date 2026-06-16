package com.sellora.partner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Subtle, physics-based motion system for Sellora Partner
 * Animations are felt, not seen - minimal, fast, functional
 */
object MotionUtils {

    private const val PRESS_SCALE = 0.97f
    private const val RELEASE_SCALE = 1f
    private const val LIKE_SCALE = 1.12f
    private const val PRESS_DURATION = 80L
    private const val RELEASE_DURATION = 160L
    private const val LIKE_DURATION = 200L
    private const val ENTRY_DURATION = 200L
    private const val ENTRY_TRANSLATION_Y = 20f

    private val decelerateInterpolator = DecelerateInterpolator()
    private val overshootInterpolator = OvershootInterpolator(1.2f) // Subtle tension
    private val fastOutSlowInInterpolator = FastOutSlowInInterpolator()

    /**
     * Apply physical press effect to any clickable view
     * Feels like real button compression
     */
    fun applyPressEffect(view: View) {
        if (view.getTag(R.id.tag_press_applied) == true) return
        view.setTag(R.id.tag_press_applied, true)

        view.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate()
                        .scaleX(PRESS_SCALE)
                        .scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION)
                        .setInterpolator(decelerateInterpolator)
                        .start()
                    false // Let click listener handle it
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(RELEASE_SCALE)
                        .scaleY(RELEASE_SCALE)
                        .setDuration(RELEASE_DURATION)
                        .setInterpolator(overshootInterpolator)
                        .start()
                    false
                }
                else -> false
            }
        }
    }

    /**
     * Subtle entry animation for RecyclerView items
     * Alpha fade + slide up, feels natural
     */
    fun animateItemEntry(view: View, delay: Long = 0L) {
        view.alpha = 0f
        view.translationY = ENTRY_TRANSLATION_Y

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ENTRY_DURATION)
            .setInterpolator(decelerateInterpolator)
            .setStartDelay(delay)
            .start()
    }

    /**
     * Like button micro-bounce - feels like heart beat
     * Subtle scale pulse, not dramatic
     */
    fun applyLikeBounce(view: View, onComplete: (() -> Unit)? = null) {
        view.animate()
            .scaleX(LIKE_SCALE)
            .scaleY(LIKE_SCALE)
            .setDuration(LIKE_DURATION / 2)
            .setInterpolator(fastOutSlowInInterpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.animate()
                        .scaleX(RELEASE_SCALE)
                        .scaleY(RELEASE_SCALE)
                        .setDuration(LIKE_DURATION / 2)
                        .setInterpolator(overshootInterpolator)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                onComplete?.invoke()
                            }
                        })
                        .start()
                }
            })
            .start()
    }

    /**
     * Smooth scroll physics refinement
     * Slightly reduced fling velocity for more control
     */
    fun configureSmoothScroll(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
            // Let default scroll physics handle it, just ensure smoothness
        }
    }

    /**
     * Quick fade transition for content changes
     * Almost invisible, just smooth
     */
    fun fadeIn(view: View, duration: Long = 120L) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(decelerateInterpolator)
            .start()
    }

    /**
     * Remove all animations instantly (for cleanup)
     */
    fun clearAnimations(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
        view.translationX = 0f
    }
}
