package com.muslimdaily.app.di

import android.content.Context
import androidx.room.Room
import com.muslimdaily.app.data.local.AppDatabase
import com.muslimdaily.app.data.local.DuaDao
import com.muslimdaily.app.data.local.PrayerDao
import com.muslimdaily.app.data.local.QuranDao
import com.muslimdaily.app.data.remote.AladhanApi
import com.muslimdaily.app.data.remote.QuranApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "muslim_daily.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePrayerDao(db: AppDatabase): PrayerDao = db.prayerDao()
    @Provides fun provideQuranDao(db: AppDatabase): QuranDao = db.quranDao()
    @Provides fun provideDuaDao(db: AppDatabase): DuaDao = db.duaDao()

    @Provides
    @Singleton
    fun provideAladhanApi(): AladhanApi =
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApi::class.java)

    @Provides
    @Singleton
    fun provideQuranApi(): QuranApi =
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranApi::class.java)
}
