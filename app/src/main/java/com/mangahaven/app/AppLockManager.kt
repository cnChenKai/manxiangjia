package com.mangahaven.app

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mangahaven.data.local.AppSettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
) : DefaultLifecycleObserver {

    private var backgroundMillis = 0L
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private val lockTimeoutMillis = 60_000L // 1分钟后台时间即锁定

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundMillis > 0) {
            val elapsed = SystemClock.elapsedRealtime() - backgroundMillis
            if (elapsed >= lockTimeoutMillis) {
                _isLocked.value = true
            }
        }
        backgroundMillis = 0L
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundMillis = SystemClock.elapsedRealtime()
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    suspend fun isPrivacyLockEnabled(): Boolean {
        return appSettingsDataStore.privacyLockEnabledFlow.first()
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Timber.e("Biometric error $errorCode: $errString")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    // Let user stay on blocked screen
                    onError()
                } else {
                    onError()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Timber.d("Biometric authentication successful")
                unlockApp()
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                Timber.w("Biometric authentication failed")
                // Framework UI typically handles this, but user could trigger error logic
            }
        })

        // Require either biometric or device credential (PIN/Pattern)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MangaHaven 隐私锁")
            .setSubtitle("请验证身份以恢复阅读")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }
}
