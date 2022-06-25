package com.example.biometricsample.auth

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.biometricsample.R
import com.example.biometricsample.databinding.FragmentAuthBinding
import com.example.biometrymodule.BiometryManager
import com.example.biometrymodule.BiometryResult
import com.example.biometrymodule.UserWasAskAboutBiometry
import com.example.model.User

class AuthFragment : Fragment() {

    private val vm by viewModels<AuthViewModel>()
    private lateinit var biometryManager: BiometryManager
    private var userClickLater = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentAuthBinding.inflate(LayoutInflater.from(requireContext()), container, false).apply {
            vm = this@AuthFragment.vm
            lifecycleOwner = this@AuthFragment.viewLifecycleOwner
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.event.observe(viewLifecycleOwner) {
            when (it) {
                is Event.SuccessLogin -> {
                    if (it.askAboutBiometry && !userClickLater) {
                        askAboutUseBiometry(it.user)
                    } else {
                        successLogin()
                    }
                    vm.event.value = Event.None
                }
                Event.LoginViaBiometry -> {
                    showBiometryForDecryption()
                    vm.event.value = Event.None
                }
                Event.None -> {
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        biometryManager = BiometryManager(requireActivity())
        vm.biometryManager = biometryManager
    }

    private fun successLogin() {
        navigateToTheContent()
    }

    override fun onResume() {
        super.onResume()
        if (biometryManager.getCiphertextWrapperFromSharedPrefs() != null) {
            showBiometryForDecryption()
        }
    }


    private fun askAboutUseBiometry(user: User) {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(R.string.ask_about_use_biometry)
            .setNegativeButton(R.string.no) { _, _ ->
                biometryManager.saveUserEnabledBiometry(
                    false
                )
                biometryManager.saveAskAboutBiometry(UserWasAskAboutBiometry.ASKED_NO)
                successLogin()
            }
            .setPositiveButton(
                R.string.yes
            ) { _, _ ->
                showBiometryForEncryption(user)
            }
            .setNeutralButton(R.string.later) { _, _ ->
                biometryManager.saveAskAboutBiometry(UserWasAskAboutBiometry.ASKED_YES)
                successLogin()
                userClickLater = true
            }.show()

    }

    private fun showBiometryForEncryption(user: User) {
        biometryManager.showBiometricPromptForEncryption(user) { result ->
            when (result) {
                is BiometryResult.BiometryError -> {
                    if (result.errCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) { // here is a bug. When user clicked "later" for different languages we received a different errCodes
                        userClickLater = true
                        biometryManager.saveAskAboutBiometry(UserWasAskAboutBiometry.LATER)
                        successLogin()
                    }
                }
                BiometryResult.BiometryFailed -> {
                    //do nothing
                }
                is BiometryResult.BiometrySuccess -> {
                    biometryManager.saveUserEnabledBiometry(true)
                    biometryManager.saveAskAboutBiometry(UserWasAskAboutBiometry.ASKED_YES)
                    successLogin()
                }
            }

        }
    }

    private fun showBiometryForDecryption() {
        biometryManager.showBiometricPromptForDecryption { result, token -> //We save token for use it later for the network requests
            when (result) {
                is BiometryResult.BiometryError -> {
                    if (result.errCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) { // here is a bug. When user clicked "later" for different languages we received a different errCodes
                        userClickLater = true
                        biometryManager.saveAskAboutBiometry(UserWasAskAboutBiometry.LATER)
                    }
                }
                BiometryResult.BiometryFailed -> {
                    //do nothing
                }
                is BiometryResult.BiometrySuccess -> {
                    successLogin()
                }
            }

        }
    }

    private fun navigateToTheContent() {
        findNavController().navigate(R.id.action_authFragment_to_contentFragment)
    }

}