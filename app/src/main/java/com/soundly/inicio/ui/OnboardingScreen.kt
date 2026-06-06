package com.soundly.inicio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundly.BuildConfig
import com.soundly.R
import com.soundly.inicio.viewmodel.OnboardingViewModel
import com.soundly.ui.components.SoundlyPrimaryButton
import com.soundly.ui.components.SoundlySecondaryButtonon
import com.soundly.ui.components.rememberLogoColor

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val extractedColor = rememberLogoColor()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SoundlySecondaryButtonon(
                    extractedColor = extractedColor,
                    onClick = { /* TODO: abrir Sobre Soundly */ }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Sobre Soundly",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Image(
                painter = painterResource(id = R.drawable.logo_soundly),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(180.dp))

            Text(
                text = stringResource(id = R.string.onboarding_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            SoundlyPrimaryButton(
                extractedColor = extractedColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                onClick = {
                    viewModel.completeOnboarding {
                        onFinished()
                    }
                },
                text = stringResource(id = R.string.onboarding_continue_button)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Soundly Music v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
