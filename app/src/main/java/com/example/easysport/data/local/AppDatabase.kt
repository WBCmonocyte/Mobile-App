package com.example.easysport.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.easysport.data.local.dao.BookingDao
import com.example.easysport.data.local.dao.CourtDao
import com.example.easysport.data.local.dao.UserDao
import com.example.easysport.data.local.entity.BookingEntity
import com.example.easysport.data.local.entity.CourtEntity
import com.example.easysport.data.local.entity.UserEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courts ADD COLUMN latitude REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE courts ADD COLUMN longitude REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE courts ADD COLUMN sportType TEXT NOT NULL DEFAULT 'Bóng đá'")
        db.execSQL("ALTER TABLE courts ADD COLUMN indoor INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courts ADD COLUMN city TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE courts ADD COLUMN district TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [
        UserEntity::class,
        BookingEntity::class,
        CourtEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bookingDao(): BookingDao
    abstract fun courtDao(): CourtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "easysport_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
