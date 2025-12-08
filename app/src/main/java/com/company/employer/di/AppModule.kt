package com.company.employer.di

import com.company.employer.data.local.CacheManager
import com.company.employer.data.local.TokenManager
import com.company.employer.data.remote.ApiService
import com.company.employer.data.remote.HttpClientFactory
import com.company.employer.data.repository.*
import com.company.employer.domain.usecase.*
import com.company.employer.presentation.calendar.CalendarViewModel
import com.company.employer.presentation.login.LoginViewModel
import com.company.employer.presentation.notifications.NotificationBadgeViewModel
import com.company.employer.presentation.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local
    single { TokenManager(androidContext()) }
    single { CacheManager(androidContext()) }

    // Remote
    single { HttpClientFactory.create(androidContext(), get()) }
    single { ApiService(get()) }

    // Repositories - Now with cache manager
    single { AuthRepository(get(), get(), get()) }
    single { CalendarRepository(get(), get()) }
    single { NotificationRepository(get()) }
    single { ProfileRepository(get()) }

    // Use Cases
    single { LoginUseCase(get()) }
    single { GetCalendarEventsUseCase(get()) }

    // ViewModels
    viewModel { LoginViewModel(get(), get(), get(), androidContext()) }
    viewModel { CalendarViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { NotificationBadgeViewModel(get(), get()) }
}