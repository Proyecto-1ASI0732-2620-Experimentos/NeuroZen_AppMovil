package com.example.neurozen_front.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contract of https://neurozen-backend-mobile.onrender.com/swagger/v1/swagger.json (base /api/v1/).
 * The Authorization header is added by AuthInterceptor; admin endpoints are web-only and not declared here.
 */
interface NeuroZenApi {
    // Authentication
    @POST("authentication/sign-in") suspend fun signIn(@Body body: SignInDto): Response<AuthenticatedUserDto>
    @POST("authentication/sign-up") suspend fun signUp(@Body body: SignUpDto): Response<AuthenticatedUserDto>
    @POST("authentication/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordDto): Response<MessageDto>
    @POST("authentication/reset-password") suspend fun resetPassword(@Body body: ResetPasswordDto): Response<MessageDto>

    // Users (owner only)
    @GET("users/{id}") suspend fun getUser(@Path("id") id: Long): Response<UserDto>
    @PUT("users/{id}") suspend fun updateUser(@Path("id") id: Long, @Body body: UpdateUserDto): Response<UserDto>
    @PUT("users/{id}/password") suspend fun changePassword(@Path("id") id: Long, @Body body: ChangePasswordDto): Response<Unit>
    @DELETE("users/{id}") suspend fun deleteUser(@Path("id") id: Long): Response<Unit>

    // Stress
    @GET("triggers") suspend fun getTriggers(@Query("userId") userId: Long): Response<List<TriggerDto>>
    @POST("triggers") suspend fun createTrigger(@Body body: CreateTriggerDto): Response<TriggerDto>
    @DELETE("triggers/{id}") suspend fun deleteTrigger(@Path("id") id: Long): Response<Unit>
    @GET("assessments") suspend fun getAssessments(@Query("userId") userId: Long): Response<List<AssessmentDto>>
    @POST("assessments") suspend fun createAssessment(@Body body: CreateAssessmentDto): Response<AssessmentDto>
    @GET("users/{userId}/health_metrics") suspend fun getHealthMetrics(@Path("userId") userId: Long): Response<List<HealthMetricDto>>
    @POST("users/{userId}/health_metrics") suspend fun createHealthMetric(@Path("userId") userId: Long, @Body body: CreateHealthMetricDto): Response<Unit>

    // Reports
    @GET("reports") suspend fun getReports(@Query("patientId") patientId: Long? = null, @Query("professionalId") professionalId: Long? = null): Response<List<ReportDto>>
    @POST("reports") suspend fun createReport(@Body body: CreateReportDto): Response<ReportDto>

    // Professionals
    @GET("professionals") suspend fun getProfessionals(): Response<List<ProfessionalDto>>
    @GET("professionals/{id}") suspend fun getProfessional(@Path("id") id: Long): Response<ProfessionalDto>
    @GET("professionals/{id}/reviews") suspend fun getReviews(@Path("id") id: Long): Response<List<ReviewDto>>
    @POST("professionals/{id}/reviews") suspend fun createReview(@Path("id") id: Long, @Body body: CreateReviewDto): Response<ReviewDto>
    @GET("professionals/{id}/available-slots") suspend fun getAvailableSlots(@Path("id") id: Long, @Query("date") date: String): Response<List<String>>
    @GET("professionals/me") suspend fun getMyProfessional(): Response<MyProfessionalDto>
    @PUT("professionals/me") suspend fun updateMyProfessional(@Body body: UpdateMyProfessionalDto): Response<MyProfessionalDto>

    // Appointments
    @GET("appointments/types") suspend fun getAppointmentTypes(): Response<List<AppointmentTypeDto>>
    @GET("appointments") suspend fun getAppointments(@Query("patientId") patientId: Long? = null, @Query("professionalId") professionalId: Long? = null): Response<List<AppointmentDto>>
    @POST("appointments") suspend fun createAppointment(@Body body: CreateAppointmentDto): Response<AppointmentDto>
    @PATCH("appointments/{id}/status") suspend fun updateAppointmentStatus(@Path("id") id: Long, @Body body: UpdateStatusDto): Response<AppointmentDto>

    // Library
    @GET("resource-libraries") suspend fun getResources(): Response<List<ResourceDto>>
    @GET("resource-libraries/{id}") suspend fun getResource(@Path("id") id: Long): Response<ResourceDto>

    // Subscriptions
    @POST("subscriptions") suspend fun createSubscription(@Body body: CreateSubscriptionDto): Response<SubscriptionDto>
    @GET("subscriptions/user/{userId}") suspend fun getSubscription(@Path("userId") userId: Long): Response<SubscriptionDto>
}
