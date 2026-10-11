package com.splitease.app.domain

import com.splitease.app.domain.model.SplitType
import com.splitease.app.domain.usecase.DebtSimplifier
import com.splitease.app.domain.usecase.SplitCalculator
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DebtSimplifierTest {

    private fun apply(balances: Map<String, Long>, transfers: List<com.splitease.app.domain.model.Transfer>): Map<String, Long> {
        val out = balances.toMutableMap()
        transfers.forEach {
            out.merge(it.from, it.amount, Long::plus)
            out.merge(it.to, -it.amount, Long::plus)
        }
        return out
    }

    @Test fun classicChainCollapses() {
        // A owes B 10, B owes C 10  => A pays C 10 directly.
        val bal = mapOf("A" to -1000L, "B" to 0L, "C" to 1000L)
        val t = DebtSimplifier.simplify(bal)
        assertEquals(1, t.size)
        assertEquals("A", t[0].from); assertEquals("C", t[0].to); assertEquals(1000L, t[0].amount)
    }

    @Test fun independentPairsUseOptimalSplit() {
        // Two independent zero-sum pairs need 2 transfers; greedy alone could use 3.
        val bal = mapOf("A" to 5000L, "B" to -5000L, "C" to 3000L, "D" to -3000L)
        assertEquals(2, DebtSimplifier.simplifyOptimal(bal).size)
    }

    @Test fun randomBalancesAlwaysSettleToZero() {
        val rnd = Random(42)
        repeat(300) {
            val n = rnd.nextInt(2, 12)
            val amounts = MutableList(n - 1) { rnd.nextLong(-100_000, 100_000) }
            amounts += -amounts.sum()
            val bal = amounts.mapIndexed { i, v -> "u$i" to v }.toMap()
            for (t in listOf(DebtSimplifier.simplify(bal), DebtSimplifier.simplifyOptimal(bal))) {
                assertTrue(apply(bal, t).values.all { it == 0L })
                assertTrue(t.size <= n - 1)
                assertTrue(t.all { it.amount > 0 })
            }
        }
    }
}

class SplitCalculatorTest {
    @Test fun equalSplitKeepsEveryCent() {
        val r = SplitCalculator.compute(10_000, SplitType.EQUAL, mapOf("a" to 1, "b" to 1, "c" to 1)).getOrThrow()
        assertEquals(10_000L, r.sumOf { it.amount })
        assertEquals(setOf(3333L, 3334L), r.map { it.amount }.toSet())
    }

    @Test fun percentMustTotal100() {
        assertTrue(SplitCalculator.compute(1000, SplitType.PERCENT, mapOf("a" to 6000, "b" to 3000)).isFailure)
        val ok = SplitCalculator.compute(1000, SplitType.PERCENT, mapOf("a" to 6000, "b" to 4000)).getOrThrow()
        assertEquals(listOf(600L, 400L), ok.sortedBy { it.memberId }.map { it.amount })
    }

    @Test fun sharesAreProportional() {
        val r = SplitCalculator.compute(900, SplitType.SHARES, mapOf("a" to 2, "b" to 1)).getOrThrow()
        assertEquals(listOf(600L, 300L), r.sortedBy { it.memberId }.map { it.amount })
    }

    @Test fun exactMustMatchTotal() {
        assertTrue(SplitCalculator.compute(1000, SplitType.EXACT, mapOf("a" to 400, "b" to 500)).isFailure)
        assertTrue(SplitCalculator.compute(1000, SplitType.EXACT, mapOf("a" to 400, "b" to 600)).isSuccess)
    }
}
