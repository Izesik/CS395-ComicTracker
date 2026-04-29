package com.moravian.comictracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromReadStatus(value: ReadStatus): String = value.name

    @TypeConverter
    fun toReadStatus(value: String): ReadStatus = ReadStatus.valueOf(value)
}
