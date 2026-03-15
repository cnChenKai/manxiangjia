package com.mangahaven.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangahaven.app.navigation.AppNavigation
import com.mangahaven.app.ui.theme.MangaHavenTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import com.mangahaven.data.local.AppSettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * 主 Activity，Hilt 注入入口。
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager
    
    @Inject
    lateinit var appSettingsDataStore: AppSettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
            var privacyLockEnabled by remember { mutableStateOf(false) }
            var isPrivacyLockLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                privacyLockEnabled = appSettingsDataStore.privacyLockEnabledFlow.first()
                isPrivacyLockLoaded = true
            }

            if (!isPrivacyLockLoaded) {
                // Wait until settings are loaded
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                return@setContent
            }

            MangaHavenTheme {
                if (isLocked && privacyLockEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("应用已锁定", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(onClick = {
                                    appLockManager.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = {},
                                        onError = {}
                                    )
                                }) {
                                    Text("验证以解锁")
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        appLockManager.authenticate(
                            activity = this@MainActivity,
                            onSuccess = {},
                            onError = {}
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
