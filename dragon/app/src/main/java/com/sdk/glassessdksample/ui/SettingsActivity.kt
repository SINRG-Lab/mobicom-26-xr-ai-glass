package com.sdk.glassessdksample.ui

import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.sdk.glassessdksample.LLMManager
import com.sdk.glassessdksample.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val availableModels = listOf(
        "gpt-4o-mini",
        "claude-sonnet-4-20250514",
        "gemini-2.5-flash"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupModelSelector()
    }

    private fun setupModelSelector() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            availableModels
        )
        binding.modelSpinner.adapter = adapter

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedModel = prefs.getString("selected_model", availableModels.first())
        val selectedIndex = availableModels.indexOf(savedModel)
        if (selectedIndex >= 0) binding.modelSpinner.setSelection(selectedIndex)

        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val chosenModel = availableModels[position]
                LLMManager.getModelName()?.ModelName = chosenModel
                Log.d("Ossian", "llm model: ${LLMManager.getModelName()?.ModelName}")
                prefs.edit().putString("selected_model", chosenModel).apply()

                val prefs_model = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val savedModel = prefs_model.getString("selected_model", "gpt-4o-mini") // default fallback

                Log.d("Ossian", "Loaded model: $savedModel")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
