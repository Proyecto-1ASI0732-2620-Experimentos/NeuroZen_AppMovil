package com.example.neurozen_front

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurozen_front.neurozen.data.local.AppointmentEntity
import com.example.neurozen_front.neurozen.data.local.NeurozenDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de Integración Local para NeuroZen (sin Mocks / sin Mockito).
 * Valida la integración real entre las entidades de negocio, el DAO de Room
 * y la base de datos SQLite en memoria sobre el dispositivo Android.
 * 
 * Frente: Aplicación Móvil (Android Nativo con Room Database)
 * Responsable: Joao Castro
 */
@RunWith(AndroidJUnit4::class)
class RoomIntegrationTest {

    private lateinit var database: NeurozenDatabase

    @Before
    fun createDb() {
        // ARRANGE: Crear una instancia REAL de la base de datos Room en memoria (sin Mocks)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, NeurozenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    /**
     * Prueba de Integración 1: Inserción y Consulta de Citas en Base de Datos Real.
     * Valida la persistencia real del DAO sin simular con Mockito.
     */
    @Test
    fun roomIntegration_insertAndQueryAppointment_isSuccessful() = runBlocking {
        // Arrange: Objeto de dominio real
        val appointment = AppointmentEntity(
            id = 101L,
            psychologistId = "psy_202",
            psychologistName = "Dr. Carlos Rodríguez",
            psychologistSpecialty = "Psicología Clínica",
            dateMillis = 1760108400000L,
            status = "CONFIRMED"
        )

        // Act: Operación de persistencia real en la base de datos
        database.appointmentDao().insertAppointment(appointment)
        val appointments = database.appointmentDao().getAllAppointments().first()

        // Assert: Verificación de persistencia integra
        assertEquals(1, appointments.size)
        assertEquals("Dr. Carlos Rodríguez", appointments[0].psychologistName)
        assertEquals("CONFIRMED", appointments[0].status)
    }

    /**
     * Prueba de Integración 2: Eliminación de Registro en Base de Datos Real.
     * Valida la eliminación directa en la tabla de citas SQLite.
     */
    @Test
    fun roomIntegration_deleteAppointment_removesRecordFromDb() = runBlocking {
        // Arrange
        val appointment = AppointmentEntity(
            id = 102L,
            psychologistId = "psy_203",
            psychologistName = "Dra. Ana López",
            psychologistSpecialty = "Mindfulness",
            dateMillis = 1760263200000L,
            status = "PENDING"
        )
        database.appointmentDao().insertAppointment(appointment)

        // Act
        database.appointmentDao().deleteAppointment(102L)
        val appointmentsAfterDelete = database.appointmentDao().getAllAppointments().first()

        // Assert
        assertTrue(appointmentsAfterDelete.isEmpty())
    }
}
