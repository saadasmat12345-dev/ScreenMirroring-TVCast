package com.saad.tvcast.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saad.tvcast.R
import com.saad.tvcast.core.designsystem.component.ScreenBackground
import com.saad.tvcast.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
            onFinished()
        }
    }
}

private data class OnboardingPage(val title: Int, val body: Int, val icon: ImageVector)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_cast_title, R.string.onboarding_cast_body, Icons.Outlined.Cast),
        OnboardingPage(R.string.onboarding_web_title, R.string.onboarding_web_body, Icons.Outlined.TravelExplore),
        OnboardingPage(R.string.onboarding_secure_title, R.string.onboarding_secure_body, Icons.Outlined.Security)
    )
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]

    ScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { viewModel.finish(onFinished) }) { Text(stringResource(R.string.skip)) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Card(
                    modifier = Modifier.size(150.dp),
                    shape = RoundedCornerShape(34.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(92.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(page.icon, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.onboarding_permission_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pageIndex) 20.dp else 8.dp, 8.dp)
                                .background(
                                    if (index == pageIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        )
                    }
                }
            }
            Column {
                Button(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    onClick = {
                        if (pageIndex == pages.lastIndex) viewModel.finish(onFinished) else pageIndex += 1
                    }
                ) {
                    Text(stringResource(if (pageIndex == pages.lastIndex) R.string.get_started else R.string.next))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
