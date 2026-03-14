package com.sdk.glassessdksample.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sdk.glassessdksample.databinding.ActivityInstructionsBinding

class InstructionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        setOnClickListener(
            binding.nextStepButton
        ) {
            when (this) {
                binding.nextStepButton -> {
                    val intent = Intent(this@InstructionsActivity, DeviceBindActivity::class.java)
                    startActivity(intent)
                    // startKtxActivity<DeviceBindActivity>()
                }
            }
        }
    }
}