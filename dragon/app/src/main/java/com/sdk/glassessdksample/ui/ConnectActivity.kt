package com.sdk.glassessdksample.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sdk.glassessdksample.databinding.ActivityConnectBinding

class ConnectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        setOnClickListener(
            binding.connectButton
        ) {
            when (this) {
                binding.connectButton -> {
                    val intent = Intent(this@ConnectActivity, InstructionsActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}