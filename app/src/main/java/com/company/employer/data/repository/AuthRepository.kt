package com.company.employer.data.repository

import com.company.employer.data.local.TokenManager
import com.company.employer.data.model.LoginResponse
import com.company.employer.data.model.User
import com.company.employer.data.remote.ApiService
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    fun login(username: String, password: String): Flow<Result<LoginResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.login(username, password)
            tokenManager.saveTokens(response.access, response.refresh)
            tokenManager.saveUsername(username)
            emit(Result.Success(response))
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            emit(Result.Error(e.message ?: "Échec de connexion"))
        }
    }

    fun getCurrentUser(): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val user = apiService.getCurrentUser()
            emit(Result.Success(user))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            emit(Result.Error(e.message ?: "Échec de récupération du profil"))
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }

    fun getUsername(): Flow<String?> = tokenManager.getUsername()
}