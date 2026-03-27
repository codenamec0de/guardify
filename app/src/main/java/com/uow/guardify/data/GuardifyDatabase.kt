package com.uow.guardify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.uow.guardify.data.dao.AlertDao
import com.uow.guardify.data.dao.AppSettingsDao
import com.uow.guardify.data.dao.BreachResultDao
import com.uow.guardify.data.dao.DeviceCheckDao
import com.uow.guardify.data.dao.MonitoredAppDao
import com.uow.guardify.data.dao.ScanResultDao
import com.uow.guardify.data.entity.AlertEntity
import com.uow.guardify.data.entity.AppSettingsEntity
import com.uow.guardify.data.entity.BreachResultEntity
import com.uow.guardify.data.entity.DeviceCheckEntity
import com.uow.guardify.data.entity.MonitoredAppEntity
import com.uow.guardify.data.entity.ScanResultEntity

@Database(
    entities = [
        AlertEntity::class,
        MonitoredAppEntity::class,
        ScanResultEntity::class,
        DeviceCheckEntity::class,
        BreachResultEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GuardifyDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun deviceCheckDao(): DeviceCheckDao
    abstract fun breachResultDao(): BreachResultDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: GuardifyDatabase? = null

        fun getInstance(context: Context): GuardifyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuardifyDatabase::class.java,
                    "guardify_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
