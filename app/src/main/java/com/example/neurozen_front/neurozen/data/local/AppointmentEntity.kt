package com.example.neurozen_front.neurozen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val psychologistId: String,
    val psychologistName: String,
    val psychologistSpecialty: String = "",
    val dateMillis: Long,
    val appointmentType: Int = 1,
    val notes: String = "",
    val status: String = "Scheduled",
    val createdAt: Long = System.currentTimeMillis()
)
