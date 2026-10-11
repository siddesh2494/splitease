package com.splitease.app.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.app.data.local.entity.UserEntity
import com.splitease.app.data.repository.GroupState
import com.splitease.app.data.repository.SplitRepository
import com.splitease.app.domain.model.*
import com.splitease.app.domain.usecase.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Used by both GroupDashboard and SettleUp: type-safe nav stores args under their property names. */
@HiltViewModel
class GroupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SplitRepository,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    val state: StateFlow<GroupState?> = repo.observeGroupState(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val me: StateFlow<UserEntity?> = repo.observeMe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun suggestedRate(currency: String, base: String): Double = repo.rateFor(base, currency)

    /** Returns an error message, or null on success. */
    suspend fun addExpense(
        title: String, amountMinor: Long, currency: String, rate: Double, category: ExpenseCategory,
        paidBy: String, splitType: SplitType, inputs: Map<String, Long>,
        recurrence: Recurrence, baseCurrency: String,
    ): String? {
        if (title.isBlank()) return "Enter a title"
        if (rate <= 0) return "Exchange rate must be positive"
        val splits = SplitCalculator.compute(amountMinor, splitType, inputs)
            .getOrElse { return it.message ?: "Invalid split" }
        if (currency != baseCurrency) repo.setManualRate(currency, baseCurrency, rate)
        val now = SplitRepository.now()
        val expense = Expense(
            id = SplitRepository.newId(), groupId = groupId, title = title.trim(), amount = amountMinor,
            currency = currency, rateToBase = rate, category = category, paidBy = paidBy,
            splits = splits, splitType = splitType, recurrence = recurrence,
            // This expense is added now; the periodic worker materializes the next occurrence.
            nextDueAt = if (recurrence == Recurrence.NONE) null else repo.nextOccurrence(now, recurrence),
            createdAt = now,
        )
        repo.saveExpense(expense, isEdit = false, actorName = me.value?.name ?: "Someone")
        return null
    }

    fun deleteExpense(e: Expense) = viewModelScope.launch {
        repo.deleteExpense(e.id, groupId, e.title, me.value?.name ?: "Someone")
    }

    fun settle(from: String, to: String, amount: Long, method: String) = viewModelScope.launch {
        repo.recordSettlement(groupId, from, to, amount, method, me.value?.name ?: "Someone")
    }
}
