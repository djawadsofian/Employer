package com.company.employer.data.repository

import com.company.employer.data.local.CacheManager
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
    private val tokenManager: TokenManager,
    private val cacheManager: CacheManager
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
            // Try to fetch from network
            val user = apiService.getCurrentUser()

            // Cache the result
            cacheManager.cacheUser(user)

            emit(Result.Success(user))
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user from network")

            // Try to use cached data
            val cachedUser = cacheManager.getCachedUser()
            if (cachedUser != null) {
                Timber.d("Using cached user data (offline mode)")
                emit(Result.Success(cachedUser))
            } else {
                emit(Result.Error(e.message ?: "Échec de récupération du profil"))
            }
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
        cacheManager.clearCache()
    }

    fun getUsername(): Flow<String?> = tokenManager.getUsername()
}