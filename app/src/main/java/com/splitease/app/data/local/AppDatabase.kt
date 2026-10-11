package com.splitease.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.splitease.app.data.local.dao.*
import com.splitease.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class, GroupEntity::class, GroupMemberEntity::class,
        ExpenseEntity::class, ExpenseSplitEntity::class, SettlementEntity::class,
        ActivityEntity::class, ExchangeRateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun activityDao(): ActivityDao
    abstract fun exchangeRateDao(): ExchangeRateDao
}
