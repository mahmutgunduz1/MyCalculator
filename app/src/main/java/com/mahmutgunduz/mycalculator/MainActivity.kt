package com.mahmutgunduz.mycalculator

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.mahmutgunduz.mycalculator.databinding.ActivityMainBinding
import android.os.Build
import android.view.View
import android.view.WindowInsetsController

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentInput = ""
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Status bar ikonlarını siyah yap (arka plan beyaz kalır)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        viewModel.input.observe(this, Observer { input ->
            binding.tvInput.text = input
        })
        viewModel.result.observe(this, Observer { result ->
            binding.tvResult.text = if (result.isNotEmpty()) result else "0"
        })
        viewModel.error.observe(this, Observer { errorMsg ->
            errorMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        })

        val buttonValues = mapOf(
            binding.number0 to "0",
            binding.number1 to "1",
            binding.number2 to "2",
            binding.number3 to "3",
            binding.number4 to "4",
            binding.number5 to "5",
            binding.number6 to "6",
            binding.number7 to "7",
            binding.number8 to "8",
            binding.number9 to "9",
            binding.sumButton to "+",
            binding.removeButton to "-",
            binding.multiplyButton to "*",
            binding.divideButton to "/",
            binding.deleteButton to "DEL",
            binding.deleteAllButton to "C",
            binding.equalButton to "=",
            binding.dotButton to ".",
            binding.percentButton to "%"
        )

        for ((button, value) in buttonValues) {
            button.setOnClickListener {
                animateButton(button)
                viewModel.onButtonClick(value)
            }
        }
    }

    private fun animateButton(button: Button) {
        button.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).withEndAction {
            button.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
        }.start()
    }



}


