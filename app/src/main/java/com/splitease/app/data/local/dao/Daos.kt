package com.splitease.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.splitease.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class ExpenseWithSplits(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val splits: List<ExpenseSplitEntity>,
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("UPDATE users SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    fun observeMe(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    suspend fun getMe(): UserEntity?

    @Query("""SELECT u.* FROM users u JOIN group_members gm ON gm.userId = u.id
              WHERE gm.groupId = :groupId ORDER BY u.name""")
    fun observeMembers(groupId: String): Flow<List<UserEntity>>
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: GroupMemberEntity)

    @Query("SELECT * FROM groups WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observe(id: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE inviteCode = :code LIMIT 1")
    suspend fun findByInviteCode(code: String): GroupEntity?

    @Transaction
    suspend fun createWithMembers(group: GroupEntity, memberIds: List<String>) {
        upsert(group)
        memberIds.forEach { addMember(GroupMemberEntity(group.id, it)) }
    }

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("UPDATE groups SET name = :name, type = :type WHERE id = :id")
    suspend fun updateDetails(id: String, name: String, type: String)

    /** Foreign-key CASCADE removes the group's members, expenses, splits, settlements and activity. */
    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)

    /** People who only existed in the deleted group (never "me") are cleaned up too. */
    @Query("DELETE FROM users WHERE isMe = 0 AND id NOT IN (SELECT userId FROM group_members)")
    suspend fun deleteOrphanUsers()

    @Transaction
    suspend fun deleteWithData(id: String) {
        delete(id)
        deleteOrphanUsers()
    }
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun clearSplits(expenseId: String)

    @Query("UPDATE expenses SET deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE expenses SET deleted = 0 WHERE id = :id")
    suspend fun restore(id: String)

    /** How many live expenses in the group involve this person (as payer or in the split). */
    @Query("""SELECT COUNT(*) FROM expenses e WHERE e.groupId = :groupId AND e.deleted = 0
              AND (e.paidBy = :userId OR EXISTS (
                  SELECT 1 FROM expense_splits s WHERE s.expenseId = e.id AND s.userId = :userId))""")
    suspend fun countInvolving(groupId: String, userId: String): Int

    @Transaction
    suspend fun save(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        upsertExpense(expense)
        clearSplits(expense.id)
        upsertSplits(splits)
    }

    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND deleted = 0 ORDER BY createdAt DESC")
    fun observeForGroup(groupId: String): Flow<List<ExpenseWithSplits>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: String): ExpenseWithSplits?

    /** Recurring expenses whose next occurrence is due. */
    @Transaction
    @Query("""SELECT * FROM expenses WHERE deleted = 0 AND recurrence != 'NONE'
              AND nextDueAt IS NOT NULL AND nextDueAt <= :now""")
    suspend fun dueRecurring(now: Long): List<ExpenseWithSplits>
}

@Dao
interface SettlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(s: SettlementEntity)

    @Query("SELECT COUNT(*) FROM settlements WHERE groupId = :groupId AND (fromUserId = :userId OR toUserId = :userId)")
    suspend fun countInvolving(groupId: String, userId: String): Int

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeForGroup(groupId: String): Flow<List<SettlementEntity>>
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(a: ActivityEntity)

    @Query("SELECT * FROM activity WHERE groupId = :groupId ORDER BY createdAt DESC LIMIT 200")
    fun observeForGroup(groupId: String): Flow<List<ActivityEntity>>
}

@Dao
interface ExchangeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: ExchangeRateEntity)

    @Query("SELECT * FROM exchange_rates WHERE base = :base AND quote = :quote")
    suspend fun get(base: String, quote: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates")
    fun observeAll(): Flow<List<ExchangeRateEntity>>
}
