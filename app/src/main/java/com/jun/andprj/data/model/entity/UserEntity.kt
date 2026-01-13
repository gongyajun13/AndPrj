package com.jun.andprj.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity - 数据库实体
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String
)














