package com.example.neurozen_front.neurozen.data.network

import retrofit2.Response

class NeurozenRepository @javax.inject.Inject constructor(
    private val apiService: NeurozenApiService
) {

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                Result.failure(Exception("Error ${response.code()}: $errorMsg"))
            }
        } catch (e: Exception) {
            // Log the exception if needed
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<AuthSession> {
        return try {
            val response = apiService.signIn(SignInRequest(username = username, password = password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(AuthSession(token = body.token, userId = body.id, username = body.username, email = body.email ?: ""))
                } else {
                    Result.failure(Exception("Cuerpo de login vacío"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error de credenciales"
                Result.failure(Exception("Login fallido: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<SignUpResponse> =
        safeApiCall { apiService.signUp(SignUpRequest(username, password, email)) }

    suspend fun fetchDashboard(userId: String, bearerToken: String): Result<DashboardResponse> =
        safeApiCall { apiService.getDashboard(userId, bearerToken) }

    suspend fun getMeditations(token: String): Result<List<MeditationResource>> =
        safeApiCall { apiService.getMeditations(token) }

    suspend fun getProfessionals(token: String): Result<List<ProfessionalResource>> =
        safeApiCall { apiService.getProfessionals(token) }

    suspend fun createAppointment(appointment: AppointmentRequest, token: String): Result<AppointmentResponse> =
        safeApiCall { apiService.createAppointment(appointment, token) }

    suspend fun createSubscription(request: SubscriptionRequest, token: String): Result<SubscriptionResource> =
        safeApiCall { apiService.createSubscription(request, token) }

    suspend fun getPatientAppointments(patientId: String, token: String): Result<List<AppointmentResponse>> =
        safeApiCall { apiService.getPatientAppointments(patientId, token) }

    suspend fun createHealthMetric(request: HealthMetricRequest, token: String): Result<HealthMetricResource> =
        safeApiCall { apiService.createHealthMetric(request, token) }

    suspend fun getHealthHistory(userId: String, token: String): Result<List<HealthMetricResource>> =
        safeApiCall { apiService.getUserHealthMetrics(userId, token) }
}

