package com.company.employer.data.repository

import com.company.employer.data.model.ChangePasswordRequest
import com.company.employer.data.model.UpdateProfileRequest
import com.company.employer.data.model.User
import com.company.employer.data.remote.ApiService
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class ProfileRepository(private val apiService: ApiService) {

    fun updateProfile(
        email: String?,
        firstName: String?,
        lastName: String?,
        phoneNumber: String?
    ): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val request = UpdateProfileRequest(email, firstName, lastName, phoneNumber)
            val user = apiService.updateProfile(request)
            emit(Result.Success(user))
        } catch (e: Exception) {
            Timber.e(e, "Failed to update profile")
            emit(Result.Error(e.message ?: "Échec de mise à jour du profil"))
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            apiService.changePassword(request)
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to change password")
            emit(Result.Error(e.message ?: "Échec de changement du mot de passe"))
        }
    }
}