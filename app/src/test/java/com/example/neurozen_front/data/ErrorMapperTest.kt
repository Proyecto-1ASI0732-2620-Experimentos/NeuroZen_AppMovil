package com.example.neurozen_front.data

import com.example.neurozen_front.data.remote.ErrorMapper
import com.example.neurozen_front.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorMapperTest {

    @Test
    fun backendMessage_isShownAsIs() {
        val error = ErrorMapper.fromHttp(409, """{"message":"Ese horario ya fue reservado"}""")
        assertTrue(error is AppError.Conflict)
        assertEquals("Ese horario ya fue reservado", error.message)
    }

    @Test
    fun validationErrors_useFirstFieldMessageWhenNoMessage() {
        val error = ErrorMapper.fromHttp(400, """{"errors":{"Email":["El correo no es válido"]}}""")
        assertTrue(error is AppError.Validation)
        assertEquals("El correo no es válido", error.message)
        assertEquals(listOf("El correo no es válido"), (error as AppError.Validation).fieldErrors["Email"])
    }

    @Test
    fun statusCodes_mapToTypes() {
        assertTrue(ErrorMapper.fromHttp(401, null) is AppError.Unauthorized)
        assertTrue(ErrorMapper.fromHttp(403, "") is AppError.Forbidden)
        assertTrue(ErrorMapper.fromHttp(404, "<html>") is AppError.NotFound)
        assertTrue(ErrorMapper.fromHttp(503, null) is AppError.Server)
    }

    @Test
    fun exceptions_mapToNetworkOrTimeout() {
        assertTrue(ErrorMapper.fromThrowable(SocketTimeoutException()) is AppError.Timeout)
        assertTrue(ErrorMapper.fromThrowable(IOException()) is AppError.Network)
    }
}
