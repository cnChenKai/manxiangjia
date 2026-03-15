package com.mangahaven.data.files.di

import android.content.Context
import com.mangahaven.data.files.ProgressRepository
import com.mangahaven.data.files.`import`.FileScanner
import com.mangahaven.data.files.container.ContainerReaderFactory
import com.mangahaven.data.files.provider.PageProviderFactory
import com.mangahaven.data.local.repository.LocalProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 模块：提供文件相关依赖。
 */
@Module
@InstallIn(SingletonComponent::class)
object FilesModule {

    @Provides
    @Singleton
    fun provideFileScanner(
        @ApplicationContext context: Context,
    ): FileScanner = FileScanner(context)

    @Provides
    @Singleton
    fun provideContainerReaderFactory(
        @ApplicationContext context: Context,
    ): ContainerReaderFactory = ContainerReaderFactory(context)

    @Provides
    @Singleton
    fun providePageProviderFactory(
        @ApplicationContext context: Context,
    ): PageProviderFactory = PageProviderFactory(context)
}

/**
 * 接口绑定模块。
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
