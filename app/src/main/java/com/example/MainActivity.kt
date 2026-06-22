package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SavingsViewModel
import com.example.widget.SavingsWidgetProvider

class MainActivity : ComponentActivity() {
  
  private val viewModel: SavingsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Force widgets to refresh with current package session binding
    SavingsWidgetProvider.updateAllWidgets(this)

    setContent {
      MyApplicationTheme {
        DashboardScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    SavingsWidgetProvider.updateAllWidgets(this)
  }
}

