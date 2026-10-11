package com.splitease.app.domain.usecase

import com.splitease.app.domain.model.Expense
import com.splitease.app.domain.model.Settlement
import kotlin.math.roundToLong

/** Computes net balances in the group's base currency (minor units). Positive = owed money. */
object BalanceCalculator {

    fun compute(
        memberIds: Collection<String>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): Map<String, Long> {
        val balances = memberIds.associateWith { 0L }.toMutableMap()

        for (e in expenses) {
            // Convert each share to base currency, then reconcile rounding so the payer's credit
            // exactly equals what the participants owe (keeps total balance == 0).
            val converted = e.splits.map { it.memberId to (it.amount * e.rateToBase).roundToLong() }
            val totalBase = converted.sumOf { it.second }
            balances.merge(e.paidBy, totalBase, Long::plus)
            converted.forEach { (id, amt) -> balances.merge(id, -amt, Long::plus) }
        }
        for (s in settlements) {
            balances.merge(s.fromMemberId, s.amount, Long::plus)   // payer's debt reduces
            balances.merge(s.toMemberId, -s.amount, Long::plus)    // receiver is owed less
        }
        return balances
    }
}
