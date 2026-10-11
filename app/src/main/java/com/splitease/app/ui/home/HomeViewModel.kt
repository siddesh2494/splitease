package com.splitease.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.app.data.local.entity.GroupEntity
import com.splitease.app.data.repository.SplitRepository
import com.splitease.app.domain.model.GroupType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: SplitRepository) : ViewModel() {

    val groups: StateFlow<List<GroupEntity>> = repo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { repo.ensureMe("Me") } }

    fun createGroup(name: String, type: GroupType, currency: String, members: List<String>) {
        viewModelScope.launch { repo.createGroup(name, type, currency, members) }
    }

    suspend fun join(code: String): String? = repo.joinByCode(code)
}
