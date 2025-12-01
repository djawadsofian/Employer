package com.company.employer.domain.usecase

import com.company.employer.data.model.Notification
import com.company.employer.data.repository.NotificationRepository
import com.company.employer.domain.util.Result
import kotlinx.coroutines.flow.Flow

//class GetNotificationsUseCase(private val repository: NotificationRepository) {
//    operator fun invoke(page: Int = 1): Flow<Result<List<Notification>>> {
//        return repository.getNotifications(page)
//    }
//}