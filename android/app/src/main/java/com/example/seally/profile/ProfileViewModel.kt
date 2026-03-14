package com.example.seally.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    fun save(profile: UserProfile) {
        viewModelScope.launch {
            repository.update(profile)
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(ProfileRepository(context.applicationContext)) as T
            }
        }
    }
}

