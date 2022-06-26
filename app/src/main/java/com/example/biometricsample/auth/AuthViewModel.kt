package com.example.biometricsample.auth

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.biometrymodule.BiometryManager
import com.example.biometrymodule.UserWasAskAboutBiometry
import com.example.model.User

class AuthViewModel : ViewModel() {

    internal lateinit var biometryManager: BiometryManager
    val passwordLiveData = MutableLiveData("")
    val emailLiveData = MutableLiveData("")
    internal val event = MutableLiveData<Event>(Event.None)


    fun onClickLogin(v: View) {
        makeLoginRequest(emailLiveData.value ?: "", passwordLiveData.value ?: "")
    }

    private fun makeLoginRequest(login: String, password: String) {
            if (login.isNotEmpty() || password.isNotEmpty()) {
                val token = java.util.UUID.randomUUID().toString()
                event.value = Event.SuccessLogin(
                    User(login, password, token),
                    biometryManager.getAskAboutBiometry() == UserWasAskAboutBiometry.LATER
                )
            }
        }

}

sealed class Event {
    data class SuccessLogin(val user: User, val askAboutBiometry: Boolean) : Event()
    object LoginViaBiometry : Event()
    object None : Event()
}