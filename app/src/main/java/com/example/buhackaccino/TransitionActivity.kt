package com.example.buhackaccino

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.buhackaccino.databinding.ActivityTransitionBinding

class TransitionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "extra_target"

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

        lottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
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
        })
    }
}
