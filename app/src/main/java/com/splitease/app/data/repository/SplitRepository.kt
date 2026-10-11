package com.splitease.app.data.repository

import com.splitease.app.data.local.AppDatabase
import com.splitease.app.data.local.dao.ExpenseWithSplits
import com.splitease.app.data.local.entity.*
import com.splitease.app.domain.model.*
import com.splitease.app.domain.usecase.BalanceCalculator
import com.splitease.app.domain.usecase.DebtSimplifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToLong
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the group screens need, derived purely from local Room data (offline-first). */
data class GroupState(
    val group: Group,
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
    val balances: Map<String, Long>,
    val transfers: List<Transfer>,
    val activity: List<ActivityItem>,
)

/** Lightweight per-group info for the Home screen. Balance is the current user's, in [currency]. */
data class GroupSummary(
    val id: String,
    val name: String,
    val type: GroupType,
    val currency: String,
    val memberNames: List<String>,
    val myBalance: Long,
    val expenseCount: Int,
    val totalSpent: Long,
)

@Singleton
class SplitRepository @Inject constructor(private val db: AppDatabase) {

    private val users = db.userDao()
    private val groups = db.groupDao()
    private val expenses = db.expenseDao()
    private val settlements = db.settlementDao()
    private val activity = db.activityDao()
    private val rates = db.exchangeRateDao()

    fun observeMe(): Flow<UserEntity?> = users.observeMe()
    fun observeGroups(): Flow<List<GroupEntity>> = groups.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeGroupSummaries(): Flow<List<GroupSummary>> =
        combine(groups.observeAll(), users.observeMe()) { list, me -> list to me }
            .flatMapLatest { (list, me) ->
                if (list.isEmpty()) flowOf(emptyList<GroupSummary>())
                else combine(list.map { observeGroupState(it.id) }) { states ->
                    states.filterNotNull().map { s ->
                        GroupSummary(
                            id = s.group.id,
                            name = s.group.name,
                            type = s.group.type,
                            currency = s.group.baseCurrency,
                            memberNames = s.group.members.map { it.name },
                            myBalance = s.balances[me?.id] ?: 0L,
                            expenseCount = s.expenses.size,
                            totalSpent = s.expenses.sumOf { (it.amount * it.rateToBase).roundToLong() },
                        )
                    }
                }
            }

    /** Deletes the group and everything inside it. NonCancellable so leaving the screen can't abort it. */
    suspend fun deleteGroup(groupId: String) = withContext(NonCancellable) {
        groups.deleteWithData(groupId)
    }

    // ---- Members & group details ----

    /** Adds a new person to the group. Returns the new member's id, or null for a blank name. */
    suspend fun addMember(groupId: String, name: String, actorName: String): String? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val user = UserEntity(newId(), clean)
        users.upsert(user)
        groups.addMember(GroupMemberEntity(groupId, user.id))
        log(groupId, ActivityType.MEMBER_JOINED, actorName, "added $clean to the group")
        return user.id
    }

    suspend fun renameMember(groupId: String, userId: String, oldName: String, newName: String, actorName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean == oldName) return
        users.rename(userId, clean)
        log(groupId, ActivityType.GROUP_EDITED, actorName, "renamed $oldName to $clean")
    }

    /**
     * Removes a member. A person who still appears in an expense or payment can't be removed
     * (their history would lose its meaning), so the caller gets a message explaining what to
     * edit first. Returns null on success.
     */
    suspend fun removeMember(
        groupId: String, userId: String, name: String, myId: String?, actorName: String,
    ): String? {
        if (userId == myId) return "You can't remove yourself from the group."
        val expenseCount = expenses.countInvolving(groupId, userId)
        val settlementCount = settlements.countInvolving(groupId, userId)
        if (expenseCount > 0 || settlementCount > 0) {
            val parts = buildList {
                if (expenseCount > 0) add("$expenseCount expense${if (expenseCount == 1) "" else "s"}")
                if (settlementCount > 0) add("$settlementCount payment${if (settlementCount == 1) "" else "s"}")
            }
            return "$name is part of ${parts.joinToString(" and ")}. Edit or delete those first, then remove them."
        }
        groups.removeMember(groupId, userId)
        groups.deleteOrphanUsers()
        log(groupId, ActivityType.MEMBER_REMOVED, actorName, "removed $name from the group")
        return null
    }

    suspend fun updateGroupDetails(groupId: String, name: String, type: GroupType, actorName: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        groups.updateDetails(groupId, clean, type.name)
        log(groupId, ActivityType.GROUP_EDITED, actorName, "updated the group details")
    }

    suspend fun restoreExpense(expenseId: String, groupId: String, title: String, actorName: String) {
        expenses.restore(expenseId)
        log(groupId, ActivityType.EXPENSE_ADDED, actorName, "restored \"$title\"")
    }

    suspend fun ensureMe(name: String): UserEntity {
        users.getMe()?.let { return it }
        val me = UserEntity(id = newId(), name = name, isMe = true)
        users.upsert(me)
        return me
    }

    suspend fun createGroup(name: String, type: GroupType, currency: String, memberNames: List<String>): String {
        val me = users.getMe() ?: ensureMe("Me")
        val others = memberNames.filter { it.isNotBlank() }.map { UserEntity(newId(), it.trim()) }
        others.forEach { users.upsert(it) }
        val group = GroupEntity(
            id = newId(), name = name.trim(), type = type.name, baseCurrency = currency,
            inviteCode = newInviteCode(), createdAt = now(),
        )
        groups.createWithMembers(group, listOf(me.id) + others.map { it.id })
        log(group.id, ActivityType.GROUP_CREATED, me.name, "created the group \"${group.name}\"")
        return group.id
    }

    /** Joins a group by invite code if it exists locally (sync backends can extend this). */
    suspend fun joinByCode(code: String): String? {
        val g = groups.findByInviteCode(code.trim().uppercase()) ?: return null
        val me = users.getMe() ?: return null
        groups.addMember(GroupMemberEntity(g.id, me.id))
        log(g.id, ActivityType.MEMBER_JOINED, me.name, "joined the group")
        return g.id
    }

    fun observeGroupState(groupId: String): Flow<GroupState?> = combine(
        groups.observe(groupId),
        users.observeMembers(groupId),
        expenses.observeForGroup(groupId),
        settlements.observeForGroup(groupId),
        activity.observeForGroup(groupId),
    ) { g, members, exps, sets, acts ->
        if (g == null) return@combine null
        val group = Group(
            g.id, g.name, GroupType.valueOf(g.type), g.baseCurrency, g.inviteCode,
            members.map { Member(it.id, it.name) },
        )
        val expenseModels = exps.map { it.toDomain() }
        val settlementModels = sets.map {
            Settlement(it.id, it.groupId, it.fromUserId, it.toUserId, it.amount, it.method, it.createdAt)
        }
        val balances = BalanceCalculator.compute(members.map { it.id }, expenseModels, settlementModels)
        GroupState(
            group, expenseModels, settlementModels, balances,
            DebtSimplifier.simplifyOptimal(balances),
            acts.map { ActivityItem(it.id, it.groupId, ActivityType.valueOf(it.type), it.actorName, it.description, it.createdAt) },
        )
    }

    suspend fun saveExpense(expense: Expense, isEdit: Boolean, actorName: String) {
        val entity = ExpenseEntity(
            expense.id, expense.groupId, expense.title, expense.amount, expense.currency,
            expense.rateToBase, expense.category.name, expense.paidBy, expense.splitType.name,
            expense.recurrence.name, expense.nextDueAt, expense.createdAt,
        )
        expenses.save(entity, expense.splits.map { ExpenseSplitEntity(expense.id, it.memberId, it.amount) })
        log(
            expense.groupId,
            if (isEdit) ActivityType.EXPENSE_EDITED else ActivityType.EXPENSE_ADDED,
            actorName,
            "${if (isEdit) "edited" else "added"} \"${expense.title}\" (${expense.currency} ${expense.amount / 100.0})",
        )
    }

    suspend fun deleteExpense(expenseId: String, groupId: String, title: String, actorName: String) {
        expenses.softDelete(expenseId)
        log(groupId, ActivityType.EXPENSE_DELETED, actorName, "deleted \"$title\"")
    }

    suspend fun recordSettlement(groupId: String, from: String, to: String, amount: Long, method: String, actorName: String) {
        settlements.insert(SettlementEntity(newId(), groupId, from, to, amount, method, now()))
        log(groupId, ActivityType.SETTLED, actorName, "recorded a $method payment of ${amount / 100.0}")
    }

    // ---- Exchange rates (offline cache + manual overrides) ----
    suspend fun rateFor(base: String, quote: String): Double {
        if (base == quote) return 1.0
        rates.get(quote, base)?.let { return it.rate }          // 1 quote = rate base
        rates.get(base, quote)?.let { return 1.0 / it.rate }
        return 1.0
    }

    suspend fun setManualRate(quote: String, base: String, rate: Double) =
        rates.upsert(ExchangeRateEntity(quote, base, rate, manual = true, updatedAt = now()))

    /** Materializes due recurring expenses (called from a periodic worker). */
    suspend fun materializeRecurring(nowMs: Long = now()): Int {
        val due = expenses.dueRecurring(nowMs)
        due.forEach { row ->
            val e = row.expense
            val copy = e.copy(id = newId(), createdAt = nowMs, nextDueAt = null, recurrence = Recurrence.NONE.name)
            expenses.save(copy, row.splits.map { it.copy(expenseId = copy.id) })
            expenses.upsertExpense(e.copy(nextDueAt = nextOccurrence(e.nextDueAt!!, Recurrence.valueOf(e.recurrence))))
            log(e.groupId, ActivityType.EXPENSE_ADDED, "Recurring", "auto-added \"${e.title}\"")
        }
        return due.size
    }

    fun nextOccurrence(from: Long, r: Recurrence): Long {
        val c = Calendar.getInstance().apply { timeInMillis = from }
        when (r) {
            Recurrence.WEEKLY -> c.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> c.add(Calendar.MONTH, 1)
            Recurrence.YEARLY -> c.add(Calendar.YEAR, 1)
            Recurrence.NONE -> Unit
        }
        return c.timeInMillis
    }

    private suspend fun log(groupId: String, type: ActivityType, actor: String, text: String) =
        activity.insert(ActivityEntity(newId(), groupId, type.name, actor, text, now()))

    private fun ExpenseWithSplits.toDomain() = Expense(
        id = expense.id, groupId = expense.groupId, title = expense.title, amount = expense.amount,
        currency = expense.currency, rateToBase = expense.rateToBase,
        category = ExpenseCategory.valueOf(expense.category), paidBy = expense.paidBy,
        splits = splits.map { Split(it.userId, it.amount) },
        splitType = SplitType.valueOf(expense.splitType),
        recurrence = Recurrence.valueOf(expense.recurrence),
        nextDueAt = expense.nextDueAt, createdAt = expense.createdAt,
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
        fun now(): Long = System.currentTimeMillis()
        private val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        fun newInviteCode(): String = (1..6).map { ALPHABET.random() }.joinToString("")
    }
}
