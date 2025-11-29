package com.company.employer.di

import com.company.employer.data.local.TokenManager
import com.company.employer.data.remote.ApiService
import com.company.employer.data.remote.HttpClientFactory
import com.company.employer.data.repository.*
import com.company.employer.domain.usecase.*
import com.company.employer.presentation.calendar.CalendarViewModel
import com.company.employer.presentation.login.LoginViewModel
import com.company.employer.presentation.notifications.NotificationsViewModel
import com.company.employer.presentation.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local
    single { TokenManager(androidContext()) }

    // Remote
    single { HttpClientFactory.create(androidContext(), get()) }
    single { ApiService(get()) }

    // Repositories
    single { AuthRepository(get(), get()) }
    single { CalendarRepository(get()) }
    single { NotificationRepository(get()) }
    single { ProfileRepository(get()) }

    // Use Cases
    single { LoginUseCase(get()) }
    single { GetCalendarEventsUseCase(get()) }
    single { GetNotificationsUseCase(get()) }
    single { GetUnreadCountUseCase(get()) }

    // ViewModels
    viewModel { LoginViewModel(get(), get()) }
    viewModel { CalendarViewModel(get(), get()) }
    viewModel { NotificationsViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
}