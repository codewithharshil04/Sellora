package com.sellora.admin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Subtle physics-based motion system for Sellora Admin.
 * Animations are felt, not seen — minimal, fast, functional.
 * Inspired by Apple's spring-based micro-interactions.
 */
object MotionUtils {

    private const val PRESS_SCALE = 0.97f
    private const val RELEASE_SCALE = 1f
    private const val PRESS_DURATION = 80L
    private const val RELEASE_DURATION = 160L
    private const val ENTRY_DURATION = 220L
    private const val ENTRY_TRANSLATION_Y = 18f

    private val decelerate = DecelerateInterpolator()
    private val overshoot = OvershootInterpolator(1.2f)
    private val fastOutSlowIn = FastOutSlowInInterpolator()

    /** Physical press — feels like a real button compressing. */
    fun applyPressEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION).setInterpolator(decelerate).start()
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(RELEASE_SCALE).scaleY(RELEASE_SCALE)
                        .setDuration(RELEASE_DURATION).setInterpolator(overshoot).start()
                    false
                }
                else -> false
            }
        }
    }

    /** Subtle fade + slide entry for list items. Staggered by position. */
    fun animateItemEntry(view: View, position: Int) {
        val delay = (position * 30L).coerceAtMost(300L)
        view.alpha = 0f
        view.translationY = ENTRY_TRANSLATION_Y
        view.animate()
            .alpha(1f).translationY(0f)
            .setDuration(ENTRY_DURATION)
            .setStartDelay(delay)
            .setInterpolator(decelerate)
            .start()
    }

    /** Quick content fade swap — nearly invisible, just smooth. */
    fun fadeIn(view: View, duration: Long = 150L) {
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(duration).setInterpolator(decelerate).start()
    }

    /** Shake an input view on validation error — physical feedback. */
    fun shakeError(view: View) {
        val shake = android.animation.ObjectAnimator.ofFloat(view, "translationX",
            0f, -8f, 8f, -6f, 6f, -3f, 3f, 0f)
        shake.duration = 400
        shake.interpolator = fastOutSlowIn
        shake.start()
    }

    /** Toggle button micro-bounce on state change. */
    fun pulseToggle(view: View) {
        view.animate().scaleX(1.06f).scaleY(1.06f)
            .setDuration(100).setInterpolator(fastOutSlowIn)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(140).setInterpolator(overshoot)
                        .setListener(null).start()
                }
            }).start()
    }

    /** Cancel and reset all animations on a view. */
    fun clearAnimations(view: View) {
        view.animate().cancel()
        view.alpha = 1f; view.scaleX = 1f; view.scaleY = 1f
        view.translationX = 0f; view.translationY = 0f
    }
}
