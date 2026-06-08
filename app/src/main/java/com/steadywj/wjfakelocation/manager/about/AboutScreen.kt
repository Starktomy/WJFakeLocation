// AboutScreen.kt
package com.steadywj.wjfakelocation.manager.about

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.steadywj.wjfakelocation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_about)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stringResource(id = R.string.about_version)} 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
