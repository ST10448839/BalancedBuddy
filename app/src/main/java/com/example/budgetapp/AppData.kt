package com.example.budgetapp

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest

/**
 * Stores app user credentials and account metadata.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val createdAtMillis: Long
)

/**
 * Stores user-defined spending categories.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String
)

/**
 * Stores individual expense entries with optional attached photo URI.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("categoryId"), Index("dateMillis")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val categoryId: Long,
    val amount: Double,
    val dateMillis: Long,
    val startTime: String,
    val endTime: String,
    val description: String,
    val photoUri: String?
)

/**
 * Stores min/max monthly spending goals per user.
 */
@Entity(
    tableName = "budget_goals",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"], unique = true)]
)
data class BudgetGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val minMonthly: Double,
    val maxMonthly: Double
)

/**
 * Stores spending caps for each category.
 */
@Entity(
    tableName = "category_budgets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "categoryId"], unique = true)]
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val categoryId: Long,
    val maxAmount: Double
)

/**
 * Stores earned user badges and when they were earned.
 */
@Entity(
    tableName = "badges",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId", "name"], unique = true)]
)
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val earnedAtMillis: Long
)

/**
 * Projection for category spending totals in a period.
 */
data class CategoryTotal(
    val categoryName: String,
    val total: Double
)

/**
 * Projection combining expense data with category name for UI display.
 */
data class ExpenseWithCategory(
    val id: Long,
    val amount: Double,
    val dateMillis: Long,
    val startTime: String,
    val endTime: String,
    val description: String,
    val photoUri: String?,
    val categoryName: String
)

/**
 * Room DAO containing all database operations used by the app.
 */
@Dao
interface BudgetDao {
    /** Inserts a newly registered user. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    /** Gets a user by username for login and duplicate checks. */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /** Gets a user by ID (used for account metadata retrieval). */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    /** Updates username and password hash for account edit flow. */
    @Query("UPDATE users SET username = :username, passwordHash = :passwordHash WHERE id = :userId")
    suspend fun updateUserCredentials(userId: Long, username: String, passwordHash: String)

    /** Deletes a user; related data is removed by cascade rules. */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    /** Inserts a new category for a user. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    /** Fetches all categories for the given user. */
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name")
    suspend fun getCategories(userId: Long): List<CategoryEntity>

    /** Inserts an expense row. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    /** Fetches expenses in a date range with category names for display. */
    @Query(
        """
        SELECT e.id, e.amount, e.dateMillis, e.startTime, e.endTime, e.description, e.photoUri, c.name AS categoryName
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId AND e.dateMillis BETWEEN :fromMillis AND :toMillis
        ORDER BY e.dateMillis DESC
        """
    )
    suspend fun getExpensesInRange(userId: Long, fromMillis: Long, toMillis: Long): List<ExpenseWithCategory>

    /** Aggregates category totals for a date range. */
    @Query(
        """
        SELECT c.name AS categoryName, COALESCE(SUM(e.amount), 0) AS total
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId AND e.dateMillis BETWEEN :fromMillis AND :toMillis
        WHERE c.userId = :userId
        GROUP BY c.id, c.name
        ORDER BY total DESC
        """
    )
    suspend fun getCategoryTotals(userId: Long, fromMillis: Long, toMillis: Long): List<CategoryTotal>

    /** Returns total money spent in a date range. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE userId = :userId AND dateMillis BETWEEN :fromMillis AND :toMillis")
    suspend fun getTotalSpent(userId: Long, fromMillis: Long, toMillis: Long): Double

    /** Inserts or replaces monthly goal configuration. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetGoal(goal: BudgetGoalEntity)

    /** Fetches monthly goals for a user. */
    @Query("SELECT * FROM budget_goals WHERE userId = :userId LIMIT 1")
    suspend fun getBudgetGoal(userId: Long): BudgetGoalEntity?

    /** Inserts or replaces per-category budget caps. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryBudget(categoryBudget: CategoryBudgetEntity)

    /** Fetches all category budget caps for a user. */
    @Query("SELECT * FROM category_budgets WHERE userId = :userId")
    suspend fun getCategoryBudgets(userId: Long): List<CategoryBudgetEntity>

    /** Inserts a badge if it does not already exist. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadge(badge: BadgeEntity)

    /** Fetches badge history for a user. */
    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY earnedAtMillis DESC")
    suspend fun getBadges(userId: Long): List<BadgeEntity>
}

/**
 * Main Room database holder.
 */
@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetGoalEntity::class,
        CategoryBudgetEntity::class,
        BadgeEntity::class
    ],
    version = 2
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun dao(): BudgetDao

    companion object {
        /** Cached singleton instance of the database. */
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        /** Returns a singleton database instance for the app. */
        fun get(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_app.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }

        /** Migration that adds account creation timestamp support. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE users ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}

/**
 * Hashes plaintext password using SHA-256 before storing in DB.
 */
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray())
    return bytes.joinToString("") { byte -> "%02x".format(byte) }
}
