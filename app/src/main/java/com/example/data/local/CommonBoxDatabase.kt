package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CommonBoxDao
import com.example.data.local.entities.CashCheckEntity
import com.example.data.local.entities.HostelGroupEntity
import com.example.data.local.entities.MemberEntity
import com.example.data.local.entities.TransactionEntity

@Database(
    entities = [
        HostelGroupEntity::class,
        MemberEntity::class,
        TransactionEntity::class,
        CashCheckEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CommonBoxDatabase : RoomDatabase() {
    abstract fun commonBoxDao(): CommonBoxDao

    companion object {
        @Volatile
        private var INSTANCE: CommonBoxDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add columns to hostel_groups
                db.execSQL("ALTER TABLE hostel_groups ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_hostel_groups_groupCode ON hostel_groups(groupCode)")

                // Safely add columns to hostel_members
                db.execSQL("ALTER TABLE hostel_members ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE hostel_members ADD COLUMN phone TEXT DEFAULT NULL")

                // Safely add columns to box_transactions
                db.execSQL("ALTER TABLE box_transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE box_transactions ADD COLUMN createdByUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE box_transactions ADD COLUMN updatedByUserId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_box_transactions_groupId_type ON box_transactions(groupId, type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_box_transactions_groupId_createdAt ON box_transactions(groupId, createdAt)")
            }
        }

        fun getDatabase(context: Context): CommonBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CommonBoxDatabase::class.java,
                    "common_box_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

