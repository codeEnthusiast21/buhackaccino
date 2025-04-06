package com.example.buhackaccino

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.buhackaccino.databinding.ActivityTransitionBinding

class TransitionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "extra_target"
        // New constant for animation duration control
        const val ANIMATION_SPEED = 2.0f  // Higher values = faster animation
        const val ANIMATION_MAX_DURATION = 1500L // Maximum duration in milliseconds

        fun start(context: Context, target: Class<out Activity>, extras: Bundle? = null) {
            val intent = Intent(context, TransitionActivity::class.java)
            intent.putExtra(EXTRA_TARGET, target.name)
            extras?.let { intent.putExtras(it) }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)

        val lottie = findViewById<LottieAnimationView>(R.id.lottieView)

        // Set animation speed (higher = faster)
        lottie.speed = ANIMATION_SPEED

        // Optional: Force complete after maximum duration
        lottie.postDelayed({
            if (lottie.isAnimating) {
                lottie.cancelAnimation()
                navigateToTargetActivity()
            }
        }, ANIMATION_MAX_DURATION)

        lottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                navigateToTargetActivity()
            }
        })
    }

    private fun navigateToTargetActivity() {
        val targetClassName = intent.getStringExtra(EXTRA_TARGET)
        val clazz = Class.forName(targetClassName)
        val newIntent = Intent(this@TransitionActivity, clazz)

        // Forward extras
        val extras = intent.extras
        if (extras != null) {
            newIntent.putExtras(extras)
        }

        startActivity(newIntent)
        finish()
    }
}