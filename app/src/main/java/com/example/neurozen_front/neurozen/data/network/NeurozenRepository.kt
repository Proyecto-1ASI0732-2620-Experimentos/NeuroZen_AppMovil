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
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<AuthSession> {
        return try {
            val response = apiService.signIn(SignInRequest(username = username, password = password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val parsedRole = if (body.role?.contains("PSYCHOLOGIST", ignoreCase = true) == true ||
                        body.role?.contains("PROFESSIONAL", ignoreCase = true) == true) {
                        UserRole.PSYCHOLOGIST
                    } else {
                        UserRole.CLIENT
                    }

                    Result.success(
                        AuthSession(
                            token = body.token,
                            userId = body.id,
                            username = body.username,
                            email = body.email ?: "",
                            role = parsedRole
                        )
                    )
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

    suspend fun register(username: String, email: String, password: String, role: String = "CLIENT"): Result<SignUpResponse> =
        safeApiCall { apiService.signUp(SignUpRequest(username, password, email, role)) }

    suspend fun createProfessional(request: CreateProfessionalRequest, token: String): Result<ProfessionalResource> =
        safeApiCall { apiService.createProfessional(request, token) }

    suspend fun fetchDashboard(userId: String, bearerToken: String): Result<DashboardResponse> =
        safeApiCall { apiService.getDashboard(userId, bearerToken) }

    suspend fun getMeditations(token: String): Result<List<MeditationResource>> =
        safeApiCall { apiService.getMeditations(token) }

    suspend fun getProfessionals(token: String): Result<List<ProfessionalResource>> =
        safeApiCall { apiService.getProfessionals(token) }

    suspend fun getProfessionalById(id: Int, token: String): Result<ProfessionalResource> =
        safeApiCall { apiService.getProfessionalById(id, token) }

    suspend fun createAppointment(appointment: AppointmentRequest, token: String): Result<AppointmentResponse> =
        safeApiCall { apiService.createAppointment(appointment, token) }

    suspend fun getPatientAppointments(patientId: String, token: String): Result<List<AppointmentResponse>> =
        safeApiCall { apiService.getPatientAppointments(patientId, token) }

    suspend fun getAppointmentTypes(token: String): Result<List<AppointmentTypeResource>> =
        safeApiCall { apiService.getAppointmentTypes(token) }

    suspend fun createTrigger(request: TriggerRequest, token: String): Result<TriggerResource> =
        safeApiCall { apiService.createTrigger(request, token) }

    suspend fun getResourceLibraries(token: String): Result<List<ResourceLibraryItem>> =
        safeApiCall { apiService.getResourceLibraries(token) }

    suspend fun createResourceLibrary(item: ResourceLibraryItem, token: String): Result<ResourceLibraryItem> =
        safeApiCall { apiService.createResourceLibrary(item, token) }

    suspend fun createSubscription(request: SubscriptionRequest, token: String): Result<SubscriptionResource> =
        safeApiCall { apiService.createSubscription(request, token) }

    suspend fun getUserSubscription(userId: String, token: String): Result<SubscriptionResource> =
        safeApiCall { apiService.getUserSubscription(userId, token) }

    suspend fun getHealthHistory(userId: String, token: String): Result<List<HealthMetricResource>> =
        safeApiCall { apiService.getUserHealthMetrics(userId, token) }
}
