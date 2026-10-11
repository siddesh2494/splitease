package com.splitease.app.domain.usecase

import com.splitease.app.domain.model.Transfer
import java.util.PriorityQueue

/**
 * Debt simplification.
 *
 * Input: net balance per member in minor units. Positive = is owed money (creditor),
 * negative = owes money (debtor). Balances must sum to zero.
 *
 * Greedy max-creditor / max-debtor matching: repeatedly settle the largest debtor against the
 * largest creditor. Each step fully clears at least one participant, so the result has at most
 * (n - 1) transfers, and is deterministic (ties broken by member id).
 *
 * Note: finding the *provably* minimum number of transfers is NP-hard (it reduces to partitioning
 * balances into the maximum number of zero-sum subsets). The greedy result is the standard
 * practical choice and is optimal for the common small-group cases; [simplifyOptimal] below
 * additionally splits the group into zero-sum subsets for groups of up to 16 non-zero members.
 */
object DebtSimplifier {

    fun simplify(balances: Map<String, Long>): List<Transfer> {
        val creditors = PriorityQueue<Pair<String, Long>>(compareBy({ -it.second }, { it.first }))
        val debtors = PriorityQueue<Pair<String, Long>>(compareBy({ -it.second }, { it.first }))
        balances.forEach { (id, bal) ->
            if (bal > 0) creditors.add(id to bal) else if (bal < 0) debtors.add(id to -bal)
        }
        val result = mutableListOf<Transfer>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val (cId, cAmt) = creditors.poll()!!
            val (dId, dAmt) = debtors.poll()!!
            val pay = minOf(cAmt, dAmt)
            result += Transfer(from = dId, to = cId, amount = pay)
            if (cAmt > pay) creditors.add(cId to (cAmt - pay))
            if (dAmt > pay) debtors.add(dId to (dAmt - pay))
        }
        return result
    }

    /**
     * Tries to reduce transfer count further: partitions non-zero balances into the largest
     * number of zero-sum subsets (each subset of size k needs only k-1 transfers), then runs the
     * greedy matcher within each subset. Falls back to [simplify] for > 16 non-zero members.
     */
    fun simplifyOptimal(balances: Map<String, Long>): List<Transfer> {
        val entries = balances.filterValues { it != 0L }.toList().sortedBy { it.first }
        val n = entries.size
        if (n == 0) return emptyList()
        if (n > 16) return simplify(balances)

        val full = (1 shl n) - 1
        val sum = LongArray(1 shl n)
        for (mask in 1..full) {
            val low = Integer.numberOfTrailingZeros(mask)
            sum[mask] = sum[mask and (mask - 1)] + entries[low].second
        }
        // dp[mask] = max number of zero-sum groups that mask can be partitioned into
        val dp = IntArray(1 shl n)
        val choice = IntArray(1 shl n)
        for (mask in 1..full) {
            if (sum[mask] != 0L) { dp[mask] = Int.MIN_VALUE / 2; continue }
            // Must include lowest set bit in the chosen sub-group to avoid permutations.
            val low = mask and -mask
            var sub = mask
            var best = Int.MIN_VALUE / 2
            var bestSub = mask
            while (sub > 0) {
                if (sub and low != 0 && sum[sub] == 0L) {
                    val rest = mask xor sub
                    val cand = 1 + (if (rest == 0) 0 else dp[rest])
                    if (cand > best) { best = cand; bestSub = sub }
                }
                sub = (sub - 1) and mask
            }
            dp[mask] = best
            choice[mask] = bestSub
        }
        if (sum[full] != 0L) return simplify(balances) // unbalanced input; be forgiving

        val result = mutableListOf<Transfer>()
        var mask = full
        while (mask != 0) {
            val sub = choice[mask]
            val group = entries.filterIndexed { i, _ -> sub and (1 shl i) != 0 }.toMap()
            result += simplify(group)
            mask = mask xor sub
        }
        return result
    }
}
