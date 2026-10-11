package com.splitease.app.domain.usecase

import com.splitease.app.domain.model.Split
import com.splitease.app.domain.model.SplitType

/**
 * Converts a split definition into exact per-member amounts (minor units) that always sum to
 * the total. Remainder cents are distributed one at a time (largest-remainder method) so no
 * money is created or lost.
 */
object SplitCalculator {

    /**
     * @param inputs memberId -> raw input. Meaning depends on [type]:
     *  EQUAL: ignored (every key participates); EXACT: amount in minor units;
     *  PERCENT: percentage * 100 (50.25% -> 5025, must total 10000); SHARES: share count.
     */
    fun compute(total: Long, type: SplitType, inputs: Map<String, Long>): Result<List<Split>> {
        if (inputs.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one person"))
        if (total <= 0) return Result.failure(IllegalArgumentException("Amount must be positive"))
        return when (type) {
            SplitType.EQUAL -> Result.success(weighted(total, inputs.keys.associateWith { 1L }))
            SplitType.SHARES -> {
                if (inputs.values.any { it < 0 } || inputs.values.sum() == 0L)
                    Result.failure(IllegalArgumentException("Shares must be positive"))
                else Result.success(weighted(total, inputs))
            }
            SplitType.PERCENT -> {
                if (inputs.values.sum() != 10_000L)
                    Result.failure(IllegalArgumentException("Percentages must add up to 100%"))
                else Result.success(weighted(total, inputs))
            }
            SplitType.EXACT -> {
                val sum = inputs.values.sum()
                if (sum != total) Result.failure(
                    IllegalArgumentException("Amounts differ from total by ${total - sum}")
                ) else Result.success(inputs.filterValues { it > 0 }.map { Split(it.key, it.value) })
            }
        }
    }

    private fun weighted(total: Long, weights: Map<String, Long>): List<Split> {
        val totalWeight = weights.values.sum()
        val ids = weights.keys.sorted()
        val base = ids.associateWith { total * weights.getValue(it) / totalWeight }
        var remainder = total - base.values.sum()
        // Largest fractional remainder first; ties by id for determinism.
        val order = ids.sortedWith(
            compareByDescending<String> { (total * weights.getValue(it)) % totalWeight }.thenBy { it }
        )
        val extra = mutableMapOf<String, Long>()
        var i = 0
        while (remainder > 0) { extra.merge(order[i % order.size], 1L, Long::plus); remainder--; i++ }
        return ids.map { Split(it, base.getValue(it) + (extra[it] ?: 0L)) }.filter { it.amount > 0 }
    }
}
