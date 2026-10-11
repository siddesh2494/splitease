package com.splitease.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String? = null,
    /** UPI id / payment handle for quick settle-up. */
    val upiId: String? = null,
    val isMe: Boolean = false,
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseCurrency: String,
    val inviteCode: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userId"],
    foreignKeys = [
        ForeignKey(GroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId")],
)
data class GroupMemberEntity(val groupId: String, val userId: String)

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(GroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId"), Index("nextDueAt")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    /** Minor units in [currency]. */
    val amount: Long,
    val currency: String,
    val rateToBase: Double,
    val category: String,
    val paidBy: String,
    val splitType: String,
    val recurrence: String,
    val nextDueAt: Long?,
    val createdAt: Long,
    val deleted: Boolean = false,
)

@Entity(
    tableName = "expense_splits",
    primaryKeys = ["expenseId", "userId"],
    foreignKeys = [ForeignKey(ExpenseEntity::class, ["id"], ["expenseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class ExpenseSplitEntity(val expenseId: String, val userId: String, val amount: Long)

@Entity(
    tableName = "settlements",
    foreignKeys = [ForeignKey(GroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId")],
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: Long,
    val method: String,
    val createdAt: Long,
)

@Entity(
    tableName = "activity",
    foreignKeys = [ForeignKey(GroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId")],
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val type: String,
    val actorName: String,
    val description: String,
    val createdAt: Long,
)

/** Offline exchange-rate cache; [manual] rates are user overrides and never auto-replaced. */
@Entity(tableName = "exchange_rates", primaryKeys = ["base", "quote"])
data class ExchangeRateEntity(
    val base: String,
    val quote: String,
    @ColumnInfo(name = "rate") val rate: Double,
    val manual: Boolean,
    val updatedAt: Long,
)
