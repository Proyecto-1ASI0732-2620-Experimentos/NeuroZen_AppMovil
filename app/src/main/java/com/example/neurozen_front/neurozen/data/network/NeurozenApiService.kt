package com.example.neurozen_front.neurozen.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NeurozenApiService {

    // --- Módulo de Autenticación (IAM) ---
    @POST("authentication/sign-in")
    suspend fun signIn(@Body request: SignInRequest): Response<SignInResponse>

    @POST("authentication/sign-up")
    suspend fun signUp(@Body request: SignUpRequest): Response<SignUpResponse>

    // --- Módulo de Profesionales (Psicólogos) ---
    @GET("professionals")
    suspend fun getProfessionals(
        @Header("Authorization") bearerToken: String
    ): Response<List<ProfessionalResource>>

    @GET("professionals/{id}")
    suspend fun getProfessionalById(
        @Path("id") id: Int,
        @Header("Authorization") bearerToken: String
    ): Response<ProfessionalResource>

    @POST("professionals")
    suspend fun createProfessional(
        @Body request: CreateProfessionalRequest,
        @Header("Authorization") bearerToken: String
    ): Response<ProfessionalResource>

    // --- Módulo de Citas (Appointments) ---
    @POST("appointments")
    suspend fun createAppointment(
        @Body request: AppointmentRequest,
        @Header("Authorization") bearerToken: String
    ): Response<AppointmentResponse>

    @GET("appointments/{id}")
    suspend fun getAppointmentById(
        @Path("id") id: Int,
        @Header("Authorization") bearerToken: String
    ): Response<AppointmentResponse>

    @GET("appointments/appointments/{patientId}")
    suspend fun getPatientAppointments(
        @Path("patientId") patientId: String,
        @Header("Authorization") bearerToken: String
    ): Response<List<AppointmentResponse>>

    @GET("appointments/types")
    suspend fun getAppointmentTypes(
        @Header("Authorization") bearerToken: String
    ): Response<List<AppointmentTypeResource>>

    // --- Módulo de Triggers ---
    @POST("triggers")
    suspend fun createTrigger(
        @Body request: TriggerRequest,
        @Header("Authorization") bearerToken: String
    ): Response<TriggerResource>

    // --- Módulo de Biblioteca de Recursos (Resource Libraries) ---
    @GET("resource-libraries")
    suspend fun getResourceLibraries(
        @Header("Authorization") bearerToken: String
    ): Response<List<ResourceLibraryItem>>

    @GET("resource-libraries/{id}")
    suspend fun getResourceLibraryById(
        @Path("id") id: Int,
        @Header("Authorization") bearerToken: String
    ): Response<ResourceLibraryItem>

    @POST("resource-libraries")
    suspend fun createResourceLibrary(
        @Body request: ResourceLibraryItem,
        @Header("Authorization") bearerToken: String
    ): Response<ResourceLibraryItem>

    // --- Módulo de Suscripciones ---
    @POST("subscriptions")
    suspend fun createSubscription(
        @Body request: SubscriptionRequest,
        @Header("Authorization") bearerToken: String
    ): Response<SubscriptionResource>

    @GET("subscriptions/plan/{planId}")
    suspend fun getSubscriptionByPlan(
        @Path("planId") planId: Int,
        @Header("Authorization") bearerToken: String
    ): Response<SubscriptionResource>

    @GET("subscriptions/active/{active}")
    suspend fun getActiveSubscriptions(
        @Path("active") active: Boolean,
        @Header("Authorization") bearerToken: String
    ): Response<List<SubscriptionResource>>

    @GET("subscriptions/user/{userId}")
    suspend fun getUserSubscription(
        @Path("userId") userId: String,
        @Header("Authorization") bearerToken: String
    ): Response<SubscriptionResource>

    @GET("users/{userId}/health_metrics")
    suspend fun getUserHealthMetrics(
        @Path("userId") userId: String,
        @Header("Authorization") bearerToken: String
    ): Response<List<HealthMetricResource>>

    // --- Dashboard & Meditaciones (Compatibilidad) ---
    @GET("users/{userId}/dashboard")
    suspend fun getDashboard(
        @Path("userId") userId: String,
        @Header("Authorization") bearerToken: String
    ): Response<DashboardResponse>

    @GET("contents/meditations")
    suspend fun getMeditations(
        @Header("Authorization") bearerToken: String
    ): Response<List<MeditationResource>>
}
