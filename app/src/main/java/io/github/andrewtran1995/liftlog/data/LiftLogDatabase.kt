package io.github.andrewtran1995.liftlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class, SetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LiftLogDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun create(context: Context): LiftLogDatabase =
            Room.databaseBuilder(context, LiftLogDatabase::class.java, "liftlog.db").build()
    }
}
