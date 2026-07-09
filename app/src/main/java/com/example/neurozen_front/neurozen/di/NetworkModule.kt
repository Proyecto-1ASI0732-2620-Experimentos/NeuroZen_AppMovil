package com.example.neurozen_front.neurozen.di

import android.content.Context
import androidx.room.Room
import com.example.neurozen_front.neurozen.data.local.AppointmentDao
import com.example.neurozen_front.neurozen.data.local.NeurozenDatabase
import com.example.neurozen_front.neurozen.data.network.ApiConfig
import com.example.neurozen_front.neurozen.data.network.NeurozenApiService
import com.example.neurozen_front.neurozen.data.network.NeurozenRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeurozenDatabase {
        return Room.databaseBuilder(
            context,
            NeurozenDatabase::class.java,
            "neurozen_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAppointmentDao(db: NeurozenDatabase): AppointmentDao {
        return db.appointmentDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // Aumentado para conexiones lentas
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNeurozenApiService(retrofit: Retrofit): NeurozenApiService {
        return retrofit.create(NeurozenApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNeurozenRepository(apiService: NeurozenApiService): NeurozenRepository {
        return NeurozenRepository(apiService)
    }
}
