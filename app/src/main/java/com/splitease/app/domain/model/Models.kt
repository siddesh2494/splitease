package com.splitease.app.domain.model

enum class GroupType { TRIP, HOME, COUPLE, PROJECT }

enum class SplitType { EQUAL, EXACT, PERCENT, SHARES }

enum class ExpenseCategory { FOOD, TRAVEL, UTILITIES, ENTERTAINMENT, RENT, SHOPPING, OTHER }

enum class Recurrence { NONE, WEEKLY, MONTHLY, YEARLY }

enum class ActivityType { EXPENSE_ADDED, EXPENSE_EDITED, EXPENSE_DELETED, SETTLED, MEMBER_JOINED, GROUP_CREATED }

/** All money is stored as Long minor units (e.g. paise / cents) to avoid floating-point drift. */
data class Member(val id: String, val name: String)

data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val baseCurrency: String,
    val inviteCode: String,
    val members: List<Member>,
)

/** One person's share of an expense, in the expense's own currency (minor units). */
data class Split(val memberId: String, val amount: Long)

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Long,
    val currency: String,
    /** Units of base currency per 1 unit of [currency]. 1.0 when same as base. */
    val rateToBase: Double,
    val category: ExpenseCategory,
    val paidBy: String,
    val splits: List<Split>,
    val splitType: SplitType,
    val recurrence: Recurrence,
    val nextDueAt: Long?,
    val createdAt: Long,
)

data class Settlement(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    /** Amount in group base currency minor units. */
    val amount: Long,
    val method: String, // CASH, UPI, BANK, OTHER
    val createdAt: Long,
)

/** A suggested payment: [from] pays [to] [amount] (base currency minor units). */
data class Transfer(val from: String, val to: String, val amount: Long)

data class ActivityItem(
    val id: String,
    val groupId: String,
    val type: ActivityType,
    val actorName: String,
    val description: String,
    val createdAt: Long,
)
