package com.example.neurozen_front.neurozen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppointmentEntity::class], version = 3, exportSchema = false)
abstract class NeurozenDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
}
