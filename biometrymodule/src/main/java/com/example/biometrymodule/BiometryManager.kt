package com.example.biometrymodule

import android.os.Handler
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.model.User
import com.google.gson.Gson

class BiometryManager(private val fragmentActivity: FragmentActivity) {
    private val TAG = "BiometricPromptUtils"

    private val sharedPrefs = BiometrySharedPrefs(fragmentActivity.applicationContext)
    private val cryptographyManager = CryptographyManager(sharedPrefs)

    private fun createBiometricPrompt(
        listenerCallback: (BiometryResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(fragmentActivity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                Log.d(TAG, "errCode is $errCode and errString is: $errString")
                listenerCallback(BiometryResult.BiometryError(errCode, errString))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "User biometric rejected.")
                listenerCallback(BiometryResult.BiometryFailed)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication was successful")
                listenerCallback(BiometryResult.BiometrySuccess(result))
            }
        }
        return BiometricPrompt(fragmentActivity, executor, callback)
    }

    private fun createPromptInfo(activity: FragmentActivity): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(activity.getString(R.string.prompt_info_title))
            setSubtitle(activity.getString(R.string.prompt_info_subtitle))
            setDescription(activity.getString(R.string.prompt_info_description))
            setConfirmationRequired(false)
            setNegativeButtonText(activity.getString(R.string.prompt_info_use_app_password))
        }.build()

    fun showBiometricPromptForEncryption(
        user: User,
        callback: (BiometryResult) -> Unit
    ) {
        if (canUseBiometry()) {
            val secretKeyName = "biometric_sample_encryption_key"
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                createBiometricPrompt {
                    if (it is BiometryResult.BiometrySuccess) {
                        encryptAndStoreServerToken(
                            it.result,
                            user,
                        )
                    }
                    callback(it)
                }
            biometricPrompt.cancelAuthentication()
            val promptInfo = createPromptInfo(fragmentActivity)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreServerToken(
        authResult: BiometricPrompt.AuthenticationResult,
        user: User,
    ) {
        authResult.cryptoObject?.cipher?.apply {
            user.fakeToken.let { token ->
                Log.d(TAG, "The token from server is $token")
                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
                sharedPrefs.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper
                )
            }
        }
    }

    fun showBiometricPromptForDecryption(
        callback: (BiometryResult, String) -> Unit

    ) {
        val ciphertextWrapper = sharedPrefs.getCiphertextWrapperFromSharedPrefs()
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = "biometric_sample_encryption_key"
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initializationVector
            )
            val biometricPrompt =
                createBiometricPrompt {
                    if (it is BiometryResult.BiometrySuccess) {
                        callback(
                            it, decryptServerTokenFromStorage(
                                it.result,
                                ciphertextWrapper,
                                cryptographyManager
                            )
                        )
                    } else {
                        callback(it, "")
                    }
                }
            biometricPrompt.cancelAuthentication()
            Handler(fragmentActivity.mainLooper).postDelayed({
                val promptInfo = createPromptInfo(fragmentActivity)
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }, 1000) // https://issuetracker.google.com/issues/131980596?pli=1
        }
    }

    private fun decryptServerTokenFromStorage(
        authResult: BiometricPrompt.AuthenticationResult,
        ciphertextWrapper: CiphertextWrapper?,
        cryptographyManager: CryptographyManager,
    ): String {
        return ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                cryptographyManager.decryptData(textWrapper.ciphertext, it)
            }
        } ?: ""
    }

    fun canUseBiometry() = BiometricManager.from(fragmentActivity.applicationContext)
        .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS

    fun saveUserEnabledBiometry(biometryEnabled: Boolean) {
        sharedPrefs.saveUserEnabledBiometry(biometryEnabled)
    }

    fun getUserEnabledBiometry() = sharedPrefs.getUserEnabledBiometry()

    fun saveAskAboutBiometry(askAboutBiometry: UserWasAskAboutBiometry) {
        sharedPrefs.saveAskAboutBiometry(askAboutBiometry)
    }

    fun getAskAboutBiometry() = sharedPrefs.getAskAboutBiometry()

    fun getCiphertextWrapperFromSharedPrefs(
    ): CiphertextWrapper? {
        return sharedPrefs.getCiphertextWrapperFromSharedPrefs()
    }


}