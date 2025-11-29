package com.company.employer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.model.User
import com.company.employer.data.repository.AuthRepository
import com.company.employer.data.repository.ProfileRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editEmail: String = "",
    val editFirstName: String = "",
    val editLastName: String = "",
    val editPhoneNumber: String = "",
    val showChangePasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null
)

sealed class ProfileEvent {
    data object LoadProfile : ProfileEvent()
    data object StartEditing : ProfileEvent()
    data object CancelEditing : ProfileEvent()
    data class EmailChanged(val email: String) : ProfileEvent()
    data class FirstNameChanged(val firstName: String) : ProfileEvent()
    data class LastNameChanged(val lastName: String) : ProfileEvent()
    data class PhoneNumberChanged(val phoneNumber: String) : ProfileEvent()
    data object SaveProfile : ProfileEvent()
    data object ShowChangePasswordDialog : ProfileEvent()
    data object DismissChangePasswordDialog : ProfileEvent()
    data class CurrentPasswordChanged(val password: String) : ProfileEvent()
    data class NewPasswordChanged(val password: String) : ProfileEvent()
    data class ConfirmPasswordChanged(val password: String) : ProfileEvent()
    data object ChangePassword : ProfileEvent()
    data object Logout : ProfileEvent()
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val tokenManager: com.company.employer.data.local.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.StartEditing -> startEditing()
            is ProfileEvent.CancelEditing -> cancelEditing()
            is ProfileEvent.EmailChanged -> {
                _state.value = _state.value.copy(editEmail = event.email)
            }
            is ProfileEvent.FirstNameChanged -> {
                _state.value = _state.value.copy(editFirstName = event.firstName)
            }
            is ProfileEvent.LastNameChanged -> {
                _state.value = _state.value.copy(editLastName = event.lastName)
            }
            is ProfileEvent.PhoneNumberChanged -> {
                _state.value = _state.value.copy(editPhoneNumber = event.phoneNumber)
            }
            is ProfileEvent.SaveProfile -> saveProfile()
            is ProfileEvent.ShowChangePasswordDialog -> {
                _state.value = _state.value.copy(
                    showChangePasswordDialog = true,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    passwordChangeError = null
                )
            }
            is ProfileEvent.DismissChangePasswordDialog -> {
                _state.value = _state.value.copy(
                    showChangePasswordDialog = false,
                    passwordChangeSuccess = false
                )
            }
            is ProfileEvent.CurrentPasswordChanged -> {
                _state.value = _state.value.copy(currentPassword = event.password)
            }
            is ProfileEvent.NewPasswordChanged -> {
                _state.value = _state.value.copy(newPassword = event.password)
            }
            is ProfileEvent.ConfirmPasswordChanged -> {
                _state.value = _state.value.copy(confirmPassword = event.password)
            }
            is ProfileEvent.ChangePassword -> changePassword()
            is ProfileEvent.Logout -> logout()
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
                        _state.value = _state.value.copy(
                            isLoading = false,
                            user = result.data,
                            editEmail = result.data.email,
                            editFirstName = result.data.firstName,
                            editLastName = result.data.lastName,
                            editPhoneNumber = result.data.phoneNumber ?: "",
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun startEditing() {
        val user = _state.value.user ?: return
        _state.value = _state.value.copy(
            isEditing = true,
            editEmail = user.email,
            editFirstName = user.firstName,
            editLastName = user.lastName,
            editPhoneNumber = user.phoneNumber ?: ""
        )
    }

    private fun cancelEditing() {
        _state.value = _state.value.copy(isEditing = false)
    }

    private fun saveProfile() {
        val currentState = _state.value
        viewModelScope.launch {
            profileRepository.updateProfile(
                email = currentState.editEmail,
                firstName = currentState.editFirstName,
                lastName = currentState.editLastName,
                phoneNumber = currentState.editPhoneNumber
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = currentState.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        _state.value = currentState.copy(
                            isLoading = false,
                            user = result.data,
                            isEditing = false,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _state.value = currentState.copy(
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

        if (currentState.newPassword.length < 6) {
            _state.value = currentState.copy(
                passwordChangeError = "Le mot de passe doit contenir au moins 6 caractères"
            )
            return
        }

        if (currentState.newPassword != currentState.confirmPassword) {
            _state.value = currentState.copy(
                passwordChangeError = "Les mots de passe ne correspondent pas"
            )
            return
        }

        viewModelScope.launch {
            profileRepository.changePassword(
                currentPassword = currentState.currentPassword,
                newPassword = currentState.newPassword
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = currentState.copy(isLoading = true)
                    }
                    is Result.Success -> {
                        _state.value = currentState.copy(
                            isLoading = false,
                            passwordChangeSuccess = true,
                            passwordChangeError = null
                        )
                    }
                    is Result.Error -> {
                        _state.value = currentState.copy(
                            isLoading = false,
                            passwordChangeError = result.message
                        )
                    }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutEvent.emit(Unit)
        }
    }
}