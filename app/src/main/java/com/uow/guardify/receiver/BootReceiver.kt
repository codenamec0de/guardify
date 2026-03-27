package com.uow.guardify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uow.guardify.service.GuardifyMonitorService

/**
 * Restarts the persistent monitoring service after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            GuardifyMonitorService.start(context)
        }
    }
}
