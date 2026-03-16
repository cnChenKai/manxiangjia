package com.mangahaven.data.files.di

import com.mangahaven.model.repository.ProgressRepository
import com.mangahaven.data.local.repository.LocalProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 接口绑定模块。
 * 之前的 @Provides 已移除，因为依赖的对象（FileScanner、PageProviderFactory等）都具有 @Inject 构造函数。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FilesBindsModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: LocalProgressRepository,
    ): ProgressRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FilesProvidesModule {

    @dagger.Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
}
