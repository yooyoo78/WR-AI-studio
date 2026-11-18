package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities =
    [
      ChampionEntity::class,
      ItemEntity::class,
      RuneEntity::class,
      SpellEntity::class,
      MatchupRuleEntity::class,
      PatchNoteEntity::class,
      FavoriteChampionEntity::class,
      SavedMatchupEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun gameDao(): GameDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE champions ADD COLUMN winRatePct REAL NOT NULL DEFAULT 50.0")
          db.execSQL("ALTER TABLE champions ADD COLUMN pickRatePct REAL NOT NULL DEFAULT 5.0")
          db.execSQL("ALTER TABLE champions ADD COLUMN banRatePct REAL NOT NULL DEFAULT 2.0")
          db.execSQL("ALTER TABLE champions ADD COLUMN portraitUrl TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE items ADD COLUMN iconUrl TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE spells ADD COLUMN iconUrl TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE spells ADD COLUMN cooldownNote TEXT NOT NULL DEFAULT ''")
        }
      }

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "wild_rift_companion.db",
              )
              .addMigrations(MIGRATION_1_2)
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
