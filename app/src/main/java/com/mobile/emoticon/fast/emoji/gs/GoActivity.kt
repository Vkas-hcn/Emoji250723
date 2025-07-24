package com.mobile.emoticon.fast.emoji.gs


import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobile.emoticon.fast.emoji.MainActivity
import com.mobile.emoticon.fast.emoji.databinding.ActivityGoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.supportActionBar?.hide()
        onBackPressedDispatcher.addCallback {
        }
        startProgress()
    }
    private fun startProgress() {
        lifecycleScope.launch {
            while (true){
                binding.sP.incrementProgressBy(1)
                delay(20)
                if (binding.sP.progress == 100) {
                    jumpToMain()
                    break
                }
            }
        }
    }

    private fun jumpToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}