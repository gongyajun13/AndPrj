package com.jun.andprj.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jun.andprj.data.local.database.dao.UserDao
import com.jun.andprj.data.model.entity.UserEntity

@Database(
    entities = [
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

