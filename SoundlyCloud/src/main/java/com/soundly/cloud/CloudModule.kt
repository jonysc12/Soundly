package com.soundly.cloud

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudModule {
    
    @Provides
    @Singleton
    fun provideCloudRepository(): CloudRepository {
        return CloudRepository()
    }
}
