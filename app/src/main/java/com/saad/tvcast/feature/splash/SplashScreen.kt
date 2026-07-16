package com.saad.tvcast.feature.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.R
import com.saad.tvcast.core.designsystem.component.BrandMark
import com.saad.tvcast.core.designsystem.component.ScreenBackground
import com.saad.tvcast.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SplashViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val onboardingComplete = settingsRepository.settings
        .map { it.onboardingComplete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit,
    onNavigateOnboarding: () -> Unit
) {
    val complete by viewModel.onboardingComplete.collectAsStateWithLifecycle()

    LaunchedEffect(complete) {
        when (complete) {
            true -> onNavigateHome()
            false -> onNavigateOnboarding()
            null -> Unit
        }
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(size = 92.dp)
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(stringResource(R.string.splash_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            CircularProgressIndicator(modifier = Modifier.padding(top = 28.dp).size(28.dp))
        }
    }
}
