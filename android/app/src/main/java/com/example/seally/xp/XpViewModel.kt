package com.example.seally.xp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class XpViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = XpRepository(application)
    private val mProjectionRepository = XpProjectionRepository(application)

    val levelState: StateFlow<XpLevelState> = repo.observeLevelState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = XpLeveling.levelState(0),
        )

    val pendingTodayXp: StateFlow<Int> = mProjectionRepository.observeTodayPendingXp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    val expectedLevelState: StateFlow<XpLevelState> = combine(levelState, pendingTodayXp) { current, pending ->
        XpLeveling.levelState(current.totalXp + pending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = XpLeveling.levelState(0),
    )

    fun addXp(delta: Int) {
        viewModelScope.launch {
            repo.addXp(delta)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return XpViewModel(app) as T
            }
        }
    }
}
