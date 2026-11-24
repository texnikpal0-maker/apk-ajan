package com.apkajan.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Bir günde bir uygulamada sayılan kısa video adedi.
 * Gün + paket birlikte birincil anahtardır; geçmiş günler silinmez, istatistik ekranı buradan okur.
 */
@Entity(tableName = "app_daily_count", primaryKeys = ["date", "packageName"])
data class AppDailyCount(
    val date: String,        // yyyy-MM-dd
    val packageName: String, // ör. com.instagram.android
    val count: Int
)

/** Grafik için gün başına toplam. */
data class DayTotal(val date: String, val total: Int)

@Dao
abstract class AppUsageDao {

    @Query("INSERT OR IGNORE INTO app_daily_count(date, packageName, count) VALUES(:date, :pkg, 0)")
    protected abstract suspend fun ensureRow(date: String, pkg: String)

    @Query("UPDATE app_daily_count SET count = count + 1 WHERE date = :date AND packageName = :pkg")
    protected abstract suspend fun incrementRow(date: String, pkg: String)

    /** Satır yoksa oluşturur, sonra +1 yapar (tek işlemde). */
    @Transaction
    open suspend fun increment(date: String, pkg: String) {
        ensureRow(date, pkg)
        incrementRow(date, pkg)
    }

    @Query("SELECT COALESCE(SUM(count), 0) FROM app_daily_count WHERE date = :date")
    abstract suspend fun totalFor(date: String): Int

    @Query("SELECT * FROM app_daily_count WHERE date = :date AND count > 0 ORDER BY count DESC")
    abstract fun observeDay(date: String): Flow<List<AppDailyCount>>

    @Query(
        "SELECT date AS date, SUM(count) AS total FROM app_daily_count " +
            "WHERE date >= :fromDate GROUP BY date ORDER BY date"
    )
    abstract fun observeDailyTotals(fromDate: String): Flow<List<DayTotal>>
}

@Database(entities = [AppDailyCount::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): AppUsageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apk_ajan.db"
                ).build().also { instance = it }
            }
    }
}
