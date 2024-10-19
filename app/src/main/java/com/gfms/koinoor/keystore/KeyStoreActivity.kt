package com.gfms.koinoor.keystore

/**
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.gfms.koinoor.R

abstract class KeyStoreActivity: AppCompatActivity() {

    val biometricManager = BiometricManager.from(this)
    abstract var viewModel: KeyStoreViewModel
    abstract fun openMainModule()

    fun observeEvents() {
        viewModel.showNoSystemLockWarning.observe(this, Observer {
            warningView.isVisible = true
            waringText.setText(R.string.PIN_CONFIRM_DESC)
        })

        viewModel.showInvalidKeyWaring.observe(this, Observer {
            AlertDialogFragment.new(
                title=getString(R.string.ALERT_KEY_INAVLID_ALERT_TITLE),
                desc=getString(R.string.ALERT_KET_INVALID_ALERT_DESC),
                btnStr=getString(R.string.ALERT_OK),
                cancelable=false,
                listener = object: AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        viewModel.delegate.onCloseInvalidKeyWarning()
                    }

                    override fun onCancle() {
                        finishAffinity()
                    }
                }
            ).show(supportFragmentManager, "KEYS_INVALIDATED_ALERT")
        })

        viewModel.promptUserAuth.observe(this, Observer {
            showBiometricPrompt()
        })

        viewModel.openLaunchModule.observe(this, Observer {
            openMainModule()
        })

        viewModel.closeApplication.observer(this, Observer {
            finishAffinity()
        })
    }

    override fun onPause() {
        super.onPause()
        if (warningView.isVisible) {
            finishAffinity()
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.OS_BIOMETRIC_CONFIRM_TITLE))
            .setSubtitle(getString(R.string.OS_BIOMETRIC_SUBTITLE))
            .setDescription(getString(R.string.OS_BIOMETRIC_PROMPT_DESC))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.delegate.onAuthSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == ERROR_USER_CANCELED || errorCode == ERROR_NEGATIVE_BUTTON || errorCode == ERROR_CANCELED) {
                    viewModel.delegate.onAuthCanceled(errorCode, errString)
                }
                if (errorCode == ERROR_HW_NOT_PRESENT || errorCode == ERROR_HW_UNAVAILABLE || errorCode == ERROR_NO_DEVICE_CREDENTIAL) {
                    viewModel.delegate.onAuthError(errorCode, errString)
                }
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun isBiometricFeatureAvailable(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
}

 **/
