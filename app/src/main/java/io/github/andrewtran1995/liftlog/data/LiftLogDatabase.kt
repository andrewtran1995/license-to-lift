package io.github.andrewtran1995.liftlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class, SetEntity::class, ExerciseTypeEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class LiftLogDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun create(context: Context): LiftLogDatabase =
            Room.databaseBuilder(context, LiftLogDatabase::class.java, "liftlog.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/**
 * v2 adds the exercise catalog. Every distinct name already logged becomes a catalog entry, and
 * existing exercise rows are linked to it, so nothing logged in v1 is lost.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_types` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`nameKey` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_types_nameKey` ON `exercise_types` (`nameKey`)")
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `typeId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_typeId` ON `exercises` (`typeId`)")
        db.execSQL(
            "INSERT OR IGNORE INTO exercise_types (id, name, nameKey, createdAt) " +
                "SELECT lower(hex(randomblob(16))), trim(name), lower(trim(name)), " +
                "CAST(strftime('%s','now') AS INTEGER) * 1000 FROM exercises GROUP BY lower(trim(name))"
        )
        db.execSQL(
            "UPDATE exercises SET typeId = " +
                "(SELECT t.id FROM exercise_types t WHERE t.nameKey = lower(trim(exercises.name)))"
        )
    }
}
