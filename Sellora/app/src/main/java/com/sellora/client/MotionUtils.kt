package com.sellora.client

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Subtle, physics-based motion system for Sellora
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
    private val overshootInterpolator = OvershootInterpolator(1.2f)
    private val fastOutSlowInInterpolator = FastOutSlowInInterpolator()

    private val TAG_PRESS_APPLIED = R.id.tag_press_applied

    /**
     * Apply physical press effect to any clickable view
     * Guarded by tag to prevent multiple listener attachments
     */
    fun applyPressEffect(view: View) {
        if (view.getTag(TAG_PRESS_APPLIED) == true) return
        view.setTag(TAG_PRESS_APPLIED, true)

        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(PRESS_SCALE)
                        .scaleY(PRESS_SCALE)
                        .setDuration(PRESS_DURATION)
                        .setInterpolator(decelerateInterpolator)
                        .start()
                    false
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate()
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

    fun configureSmoothScroll(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        // Reserved for future physics adjustments
    }

    fun clearAnimations(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
        view.translationX = 0f
    }
}
