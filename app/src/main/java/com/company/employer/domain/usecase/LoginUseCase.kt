package com.company.employer.domain.usecase

import com.company.employer.data.repository.AuthRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val repository: AuthRepository) {
    operator fun invoke(username: String, password: String): Flow<Result<Boolean>> {
        return kotlinx.coroutines.flow.flow {
            repository.login(username, password).collect { result ->
                when (result) {
                    is Result.Loading -> emit(Result.Loading)
                    is Result.Success -> emit(Result.Success(true))
                    is Result.Error -> emit(Result.Error(result.message))
                }
            }
        }
    }
}