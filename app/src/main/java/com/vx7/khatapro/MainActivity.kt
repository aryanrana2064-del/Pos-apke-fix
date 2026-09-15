package com.vx7.khatapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vx7.khatapro.ui.screens.LoginScreen
import com.vx7.khatapro.ui.screens.MainAppScaffold
import com.vx7.khatapro.ui.screens.SetupWizardScreen
import com.vx7.khatapro.ui.theme.KhataProTheme
import com.vx7.khatapro.ui.viewmodel.AppScreen
import com.vx7.khatapro.ui.viewmodel.KhataViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KhataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDark = when (settings.theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            KhataProTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isInitialized by viewModel.isInitialized.collectAsState()
                    val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    when {
                        !isInitialized -> {
                            SplashScreen()
                        }
                        !isSetupCompleted || currentScreen == AppScreen.SETUP -> {
                            SetupWizardScreen(viewModel = viewModel)
                        }
                        !isLoggedIn || currentScreen == AppScreen.LOGIN -> {
                            LoginScreen(viewModel = viewModel)
                        }
                        else -> {
                            MainAppScaffold(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_khata_logo),
                contentDescription = "VX7 Khata Pro",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                "VX7 Khata Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Smart Khata & Business Ledger",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
        }
    }
}
