package com.company.employer.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.employer.data.repository.AuthRepository
import com.company.employer.domain.usecase.LoginUseCase
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

sealed class LoginEvent {
    data class UsernameChanged(val username: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object LoginClicked : LoginEvent()
    data object ErrorDismissed : LoginEvent()
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UsernameChanged -> {
                _state.value = _state.value.copy(username = event.username, error = null)
            }
            is LoginEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password, error = null)
            }
            is LoginEvent.LoginClicked -> {
                login()
            }
            is LoginEvent.ErrorDismissed -> {
                _state.value = _state.value.copy(error = null)
            }
        }
    }

    private fun login() {
        val currentState = _state.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _state.value = currentState.copy(error = "Veuillez remplir tous les champs")
            return
        }

        viewModelScope.launch {
            loginUseCase(currentState.username, currentState.password).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _state.value = currentState.copy(isLoading = true, error = null)
                    }
                    is Result.Success -> {
                        _state.value = currentState.copy(
                            isLoading = false,
                            isSuccess = true,
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
}