package com.example.seally.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val mRepository: ProfileRepository,
    private val mAccountResetRepository: AccountResetRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = mRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(profile: UserProfile) {
        viewModelScope.launch {
            mRepository.update(profile)
        }
    }

    fun clear() {
        viewModelScope.launch(Dispatchers.IO) {
            mAccountResetRepository.resetEverything()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return ProfileViewModel(
                    mRepository = ProfileRepository(appContext),
                    mAccountResetRepository = AccountResetRepository(appContext),
                ) as T
            }
        }
    }
}

