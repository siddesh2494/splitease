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

    private val actor: String get() = me.value?.name ?: "Someone"

    suspend fun suggestedRate(currency: String, base: String): Double = repo.rateFor(base, currency)

    /**
     * Creates a new expense, or updates [existing] (same id and creation date) when editing.
     * Returns an error message, or null on success.
     */
    suspend fun saveExpense(
        title: String, amountMinor: Long, currency: String, rate: Double, category: ExpenseCategory,
        paidBy: String, splitType: SplitType, inputs: Map<String, Long>,
        recurrence: Recurrence, baseCurrency: String, existing: Expense? = null,
    ): String? {
        if (title.isBlank()) return "Enter a title"
        if (rate <= 0) return "Exchange rate must be positive"
        val splits = SplitCalculator.compute(amountMinor, splitType, inputs)
            .getOrElse { return it.message ?: "Invalid split" }
        if (currency != baseCurrency) repo.setManualRate(currency, baseCurrency, rate)
        val now = SplitRepository.now()
        val nextDue = when {
            recurrence == Recurrence.NONE -> null
            // Keep the existing schedule when the frequency didn't change.
            existing != null && existing.recurrence == recurrence && existing.nextDueAt != null -> existing.nextDueAt
            else -> repo.nextOccurrence(now, recurrence)
        }
        val expense = Expense(
            id = existing?.id ?: SplitRepository.newId(), groupId = groupId, title = title.trim(),
            amount = amountMinor, currency = currency, rateToBase = rate, category = category,
            paidBy = paidBy, splits = splits, splitType = splitType, recurrence = recurrence,
            nextDueAt = nextDue, createdAt = existing?.createdAt ?: now,
        )
        repo.saveExpense(expense, isEdit = existing != null, actorName = actor)
        return null
    }

    fun deleteExpense(e: Expense) = viewModelScope.launch {
        repo.deleteExpense(e.id, groupId, e.title, actor)
    }

    fun restoreExpense(e: Expense) = viewModelScope.launch {
        repo.restoreExpense(e.id, groupId, e.title, actor)
    }

    fun deleteGroup() {
        viewModelScope.launch { repo.deleteGroup(groupId) }
    }

    // ---- Members & group details ----

    suspend fun addMember(name: String): String? = repo.addMember(groupId, name, actor)

    fun renameMember(member: Member, newName: String) = viewModelScope.launch {
        repo.renameMember(groupId, member.id, member.name, newName, actor)
    }

    /** Returns an explanation if the member can't be removed yet, or null on success. */
    suspend fun removeMember(member: Member): String? =
        repo.removeMember(groupId, member.id, member.name, me.value?.id, actor)

    fun updateGroupDetails(name: String, type: GroupType) = viewModelScope.launch {
        repo.updateGroupDetails(groupId, name, type, actor)
    }

    fun settle(from: String, to: String, amount: Long, method: String) = viewModelScope.launch {
        repo.recordSettlement(groupId, from, to, amount, method, actor)
    }
}
