package com.example.biometrymodule

import android.content.Context
import com.google.gson.Gson


const val SHARED_PREFS_FILENAME = "biometric_prefs"
const val CIPHERTEXT_WRAPPER = "ciphertext_wrapper"
const val BIOMETRY_ENABLED_KEY = "biometryEnabled"
const val USER_WAS_ASKED_ABOUT_BIOMETRY_KEY = "user_was_asked_key"

enum class UserWasAskAboutBiometry {
    LATER, ASKED_YES, ASKED_NO
}

class BiometrySharedPrefs(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(SHARED_PREFS_FILENAME, Context.MODE_PRIVATE)

    fun persistCiphertextWrapperToSharedPrefs(
        ciphertextWrapper: CiphertextWrapper,
    ) {
        val json = Gson().toJson(ciphertextWrapper)
        sharedPreferences.edit().putString(CIPHERTEXT_WRAPPER, json).apply()
    }

    fun getCiphertextWrapperFromSharedPrefs(
    ): CiphertextWrapper? {
        val json = sharedPreferences.getString(CIPHERTEXT_WRAPPER, null)
        return Gson().fromJson(json, CiphertextWrapper::class.java)
    }

    fun saveUserEnabledBiometry(
        enabled: Boolean
    ) {
        sharedPreferences.edit().putBoolean(BIOMETRY_ENABLED_KEY, enabled)
            .apply()
    }

    fun getUserEnabledBiometry(
    ): Boolean =
        sharedPreferences.getBoolean(BIOMETRY_ENABLED_KEY, false)

    fun saveAskAboutBiometry(askAboutBiometry: UserWasAskAboutBiometry) {
        sharedPreferences.edit().putInt(USER_WAS_ASKED_ABOUT_BIOMETRY_KEY, askAboutBiometry.ordinal)
            .apply()
    }

    fun getAskAboutBiometry(): UserWasAskAboutBiometry =
        UserWasAskAboutBiometry.values()[sharedPreferences.getInt(
            USER_WAS_ASKED_ABOUT_BIOMETRY_KEY, 0
        )]
}