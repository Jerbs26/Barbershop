package com.example.barbershop.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    // Checks if the device actually supports biometric auth before we try to show the prompt
    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        // Accept both strong biometrics (fingerprint/face) and device PIN/pattern as fallback
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Shows the biometric prompt and fires the appropriate callback depending on the result
    fun showPrompt(
        activity   : FragmentActivity,
        title      : String = "Biometric Login",
        subtitle   : String = "Use your fingerprint to sign in",
        onSuccess  : () -> Unit,
        onFailed   : () -> Unit = {},
        onError    : (String) -> Unit = {}
    ) {
        // Biometric callbacks need to run on the main thread
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            // Auth succeeded
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            // The biometric scan didn't match
            override fun onAuthenticationFailed() {
                onFailed()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }

        // Build and show the prompt
        BiometricPrompt(activity, executor, callback)
            .authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
    }
}