package com.company.employer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.model.User
import com.company.employer.data.repository.AuthRepository
import com.company.employer.data.repository.ProfileRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showChangePasswordDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null
)

sealed class ProfileEvent {
    data object LoadProfile : ProfileEvent()
    data object ShowChangePasswordDialog : ProfileEvent()
    data object DismissChangePasswordDialog : ProfileEvent()
    data class CurrentPasswordChanged(val password: String) : ProfileEvent()
    data class NewPasswordChanged(val password: String) : ProfileEvent()
    data class ConfirmPasswordChanged(val password: String) : ProfileEvent()
    data object ChangePassword : ProfileEvent()
    data object ShowLogoutDialog : ProfileEvent()
    data object DismissLogoutDialog : ProfileEvent()
    data object ConfirmLogout : ProfileEvent()
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val tokenManager: com.company.employer.data.local.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Use StateFlow instead of SharedFlow for more reliable logout triggering
    private val _shouldLogout = MutableStateFlow(false)
    val shouldLogout: StateFlow<Boolean> = _shouldLogout.asStateFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.ShowChangePasswordDialog -> {
                _state.value = _state.value.copy(
                    showChangePasswordDialog = true,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    passwordChangeError = null,
                    passwordChangeSuccess = false
                )
            }
            is ProfileEvent.DismissChangePasswordDialog -> {
                _state.value = _state.value.copy(
                    showChangePasswordDialog = false,
                    passwordChangeSuccess = false,
                    passwordChangeError = null,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = ""
                )
            }
            is ProfileEvent.CurrentPasswordChanged -> {
                _state.value = _state.value.copy(
                    currentPassword = event.password,
                    passwordChangeError = null
                )
            }
            is ProfileEvent.NewPasswordChanged -> {
                _state.value = _state.value.copy(
                    newPassword = event.password,
                    passwordChangeError = null
                )
            }
            is ProfileEvent.ConfirmPasswordChanged -> {
                _state.value = _state.value.copy(
                    confirmPassword = event.password,
                    passwordChangeError = null
                )
            }
            is ProfileEvent.ChangePassword -> changePassword()
            is ProfileEvent.ShowLogoutDialog -> {
                Timber.d("🚪 [ProfileVM] ShowLogoutDialog event received")
                _state.value = _state.value.copy(showLogoutDialog = true)
            }
            is ProfileEvent.DismissLogoutDialog -> {
                Timber.d("🚪 [ProfileVM] DismissLogoutDialog event received")
                _state.value = _state.value.copy(showLogoutDialog = false)
            }
            is ProfileEvent.ConfirmLogout -> {
                Timber.d("🚪 [ProfileVM] ConfirmLogout event received")
                logout()
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = _state.value.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        Timber.d("✅ Profile loaded successfully")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            user = result.data,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        Timber.e("❌ Failed to load profile: ${result.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun changePassword() {
        val currentState = _state.value

        // Client-side validation
        if (currentState.newPassword.length < 6) {
            _state.value = currentState.copy(
                passwordChangeError = "Le mot de passe doit contenir au moins 6 caractères",
                passwordChangeSuccess = false
            )
            return
        }

        if (currentState.newPassword != currentState.confirmPassword) {
            _state.value = currentState.copy(
                passwordChangeError = "Les mots de passe ne correspondent pas",
                passwordChangeSuccess = false
            )
            return
        }

        // Reset states before starting
        _state.value = currentState.copy(
            passwordChangeError = null,
            passwordChangeSuccess = false
        )

        viewModelScope.launch {
            profileRepository.changePassword(
                currentPassword = currentState.currentPassword,
                newPassword = currentState.newPassword
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        Timber.d("🔄 Changing password...")
                        _state.value = _state.value.copy(
                            isLoading = true,
                            passwordChangeError = null,
                            passwordChangeSuccess = false
                        )
                    }
                    is Result.Success -> {
                        Timber.d("✅ Password changed successfully")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            passwordChangeSuccess = true,
                            passwordChangeError = null,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = ""
                        )
                    }
                    is Result.Error -> {
                        Timber.e("❌ Password change failed: ${result.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            passwordChangeSuccess = false,
                            passwordChangeError = result.message
                        )
                    }
                }
            }
        }
    }

    private fun logout() {
        Timber.d("🚪 [ProfileVM] Starting logout process")

        // Close dialog first
        _state.value = _state.value.copy(showLogoutDialog = false)

        // Launch token clearing in background and immediately trigger logout
        viewModelScope.launch {
            try {
                Timber.d("🚪 [ProfileVM] Clearing tokens via repository (async)")
                authRepository.logout()
                Timber.d("🚪 [ProfileVM] Tokens cleared")
            } catch (e: Exception) {
                Timber.e(e, "🚪 [ProfileVM] Error clearing tokens")
            }
        }

        // Trigger logout immediately without waiting for token clearing
        Timber.d("🚪 [ProfileVM] Setting shouldLogout to true")
        _shouldLogout.value = true
        Timber.d("🚪 [ProfileVM] Logout triggered")
    }

    fun resetLogoutFlag() {
        _shouldLogout.value = false
    }
}