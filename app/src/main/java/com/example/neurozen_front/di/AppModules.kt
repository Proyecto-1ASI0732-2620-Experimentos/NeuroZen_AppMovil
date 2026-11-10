package com.example.neurozen_front.di

import android.content.Context
import androidx.room.Room
import com.example.neurozen_front.BuildConfig
import com.example.neurozen_front.data.local.AppointmentCacheDao
import com.example.neurozen_front.data.local.BreathingDao
import com.example.neurozen_front.data.local.ChallengeDao
import com.example.neurozen_front.data.local.NeuroZenDatabase
import com.example.neurozen_front.data.local.WorkloadDao
import com.example.neurozen_front.data.remote.NeuroZenApi
import com.example.neurozen_front.data.repository.AppointmentRepositoryImpl
import com.example.neurozen_front.data.repository.AuthRepositoryImpl
import com.example.neurozen_front.data.repository.LibraryRepositoryImpl
import com.example.neurozen_front.data.repository.ProfessionalRepositoryImpl
import com.example.neurozen_front.data.repository.ReportRepositoryImpl
import com.example.neurozen_front.data.repository.StressRepositoryImpl
import com.example.neurozen_front.data.repository.SubscriptionRepositoryImpl
import com.example.neurozen_front.data.repository.UserRepositoryImpl
import com.example.neurozen_front.data.repository.WellnessRepositoryImpl
import com.example.neurozen_front.data.session.AuthInterceptor
import com.example.neurozen_front.data.session.SessionRepositoryImpl
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.AuthRepository
import com.example.neurozen_front.domain.repository.LibraryRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.ReportRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.SubscriptionRepository
import com.example.neurozen_front.domain.repository.UserRepository
import com.example.neurozen_front.domain.repository.WellnessRepository
import com.example.neurozen_front.domain.usecase.ReminderCanceller
import com.example.neurozen_front.notifications.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Render free tier sleeps: the first request can take ~50 s.
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
        if (BuildConfig.DEBUG) {
            // BASIC: method, URL and status only. Never headers (token) or bodies (passwords, cards, health data).
            builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient): NeuroZenApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NeuroZenApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeuroZenDatabase =
        Room.databaseBuilder(context, NeuroZenDatabase::class.java, "neurozen.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun breathingDao(db: NeuroZenDatabase): BreathingDao = db.breathingDao()
    @Provides fun workloadDao(db: NeuroZenDatabase): WorkloadDao = db.workloadDao()
    @Provides fun challengeDao(db: NeuroZenDatabase): ChallengeDao = db.challengeDao()
    @Provides fun appointmentCacheDao(db: NeuroZenDatabase): AppointmentCacheDao = db.appointmentCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun session(impl: SessionRepositoryImpl): SessionRepository
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun user(impl: UserRepositoryImpl): UserRepository
    @Binds abstract fun stress(impl: StressRepositoryImpl): StressRepository
    @Binds abstract fun report(impl: ReportRepositoryImpl): ReportRepository
    @Binds abstract fun professional(impl: ProfessionalRepositoryImpl): ProfessionalRepository
    @Binds abstract fun appointment(impl: AppointmentRepositoryImpl): AppointmentRepository
    @Binds abstract fun library(impl: LibraryRepositoryImpl): LibraryRepository
    @Binds abstract fun subscription(impl: SubscriptionRepositoryImpl): SubscriptionRepository
    @Binds abstract fun wellness(impl: WellnessRepositoryImpl): WellnessRepository
    @Binds abstract fun reminders(impl: ReminderScheduler): ReminderCanceller
}
