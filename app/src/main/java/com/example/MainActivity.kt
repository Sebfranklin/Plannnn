package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PlannerApp
import com.example.ui.PlannerViewModel
import com.example.ui.theme.MyApplicationTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Uncaught exception hook to capture off-thread startup crashes
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e("MainActivity", "Uncaught runtime exception on ${thread.name}", throwable)
      defaultHandler?.uncaughtException(thread, throwable)
    }

    var initError: Throwable? = null
    var viewModelInstance: PlannerViewModel? = null

    try {
      // Pre-instantiate the database or view model in normal Java/Kotlin scope to catch any initialization issues
      viewModelInstance = ViewModelProvider(this, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return PlannerViewModel(application) as T
        }
      })[PlannerViewModel::class.java]
    } catch (t: Throwable) {
      Log.e("MainActivity", "Exception during ViewModel creation", t)
      initError = t
    }

    setContent {
      var crashThrowable by remember { mutableStateOf(initError) }

      if (crashThrowable != null) {
        CrashRecoveryScreen(throwable = crashThrowable!!) {
          crashThrowable = null
        }
      } else {
        MyApplicationTheme {
          PlannerApp(viewModel = viewModelInstance!!)
        }
      }
    }
  }
}

@Composable
fun CrashRecoveryScreen(throwable: Throwable, onReset: () -> Unit) {
  val sw = StringWriter()
  throwable.printStackTrace(PrintWriter(sw))
  val stackTraceString = sw.toString()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF1C1B1F)) // Matches Bento palette dark shades
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "⚠️ Daily Planner Stopped",
        color = Color(0xFFB52A4A), // NeonRed / Crimson Berry accent
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Text(
        text = "An unexpected runtime issue occurred during application initialization. Diagnostic trace below:",
        color = Color(0xFF49454F), // BentoTextSecondary
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 280.dp)
          .background(Color(0xFFF3EDF7)) // BentoCardGray
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = stackTraceString,
          color = Color(0xFF21005D), // BentoTextPurple
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
      ) {
        Button(
          onClick = onReset,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)) // BentoDeepPurple
        ) {
          Text(text = "Reset & Retry", color = Color.White)
        }
      }
    }
  }
}

