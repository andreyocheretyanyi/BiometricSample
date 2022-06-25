package com.example.biometrymodule

import androidx.biometric.BiometricPrompt

sealed class BiometryResult {
    class BiometrySuccess(val result: BiometricPrompt.AuthenticationResult) : BiometryResult()
    object BiometryFailed : BiometryResult()
    class BiometryError(val errCode: Int, val errString: CharSequence) : BiometryResult()
}
