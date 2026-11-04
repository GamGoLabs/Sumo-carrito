package com.sumocarrito.di

import android.content.Context
import androidx.room.Room
import com.sumocarrito.data.local.AppDatabase
import com.sumocarrito.data.prefs.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context, store: SettingsStore): AppDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "sumocarrito.db")
            .openHelperFactory(SupportOpenHelperFactory(store.dbPassphrase()))
            .build()
    }
    @Provides fun cartDao(db: AppDatabase) = db.cartItemDao()
    @Provides fun purchaseDao(db: AppDatabase) = db.purchaseDao()
}
