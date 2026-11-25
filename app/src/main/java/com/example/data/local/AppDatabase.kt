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
      CustomBuildEntity::class,
      AiCoachCacheEntity::class,
      MatchJournalEntity::class,
      PatchSnapshotEntity::class,
    ],
  version = 7,
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

    val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE runes ADD COLUMN iconUrl TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE runes ADD COLUMN tree TEXT NOT NULL DEFAULT ''")
        }
      }

    val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_builds (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              name TEXT NOT NULL,
              championId TEXT NOT NULL,
              lane TEXT NOT NULL,
              starterItemId TEXT NOT NULL,
              coreItemIds TEXT NOT NULL,
              bootsId TEXT NOT NULL,
              enchantId TEXT NOT NULL,
              keystoneId TEXT NOT NULL,
              minorRuneIds TEXT NOT NULL,
              spellIds TEXT NOT NULL,
              notes TEXT NOT NULL,
              updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
          )
        }
      }

    val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE champions ADD COLUMN laneOverrides TEXT NOT NULL DEFAULT '[]'")
        }
      }

    val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_coach_cache (
              cacheKey TEXT NOT NULL PRIMARY KEY,
              normalizedQuestion TEXT NOT NULL,
              championId TEXT NOT NULL,
              lane TEXT NOT NULL,
              patchVersion TEXT NOT NULL,
              languageCode TEXT NOT NULL,
              responseText TEXT NOT NULL,
              modelId TEXT NOT NULL,
              modelLabel TEXT NOT NULL,
              createdAt INTEGER NOT NULL,
              lastAccessedAt INTEGER NOT NULL,
              hitCount INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
          )
        }
      }

    val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS match_journal (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              playedAt INTEGER NOT NULL,
              championId TEXT NOT NULL,
              lane TEXT NOT NULL,
              isWin INTEGER NOT NULL,
              kills INTEGER NOT NULL,
              deaths INTEGER NOT NULL,
              assists INTEGER NOT NULL,
              goldEarned INTEGER NOT NULL,
              durationMinutes INTEGER NOT NULL,
              boughtItemIds TEXT NOT NULL,
              recommendedItemIds TEXT NOT NULL,
              enemyChampionIds TEXT NOT NULL,
              buildAccuracyPct INTEGER NOT NULL,
              mistakeTags TEXT NOT NULL,
              lessons TEXT NOT NULL,
              coachSummaryText TEXT NOT NULL,
              patchVersion TEXT NOT NULL,
              notes TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS patch_snapshots (
              id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              patchVersion TEXT NOT NULL,
              sourceLabel TEXT NOT NULL,
              createdAt INTEGER NOT NULL,
              payloadJson TEXT NOT NULL,
              isRolledBack INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
          )
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
              .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
