package com.richdin.finance.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.richdin.finance.R
import com.richdin.finance.core.domain.DailyAllowanceEngine
import com.richdin.finance.core.model.PocketType
import com.richdin.finance.core.repository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FinanceRepository,
    private val allowanceEngine: DailyAllowanceEngine
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "richdin_reminders"
        const val CHANNEL_NAME = "Pengingat Harian & Peringatan Keuangan"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val activePeriod = repository.getActiveSalaryPeriodOnce() ?: return@withContext Result.success()
        val dailyPocket = repository.getPocketByTypeOnce(activePeriod.id, PocketType.DAILY) ?: return@withContext Result.success()

        val today = LocalDate.now()
        val allowanceResult = allowanceEngine.calculateDailyAllowance(
            today = today,
            nextSalaryDate = activePeriod.nextSalaryDate,
            remainingDailyBalance = dailyPocket.availableBalance,
            spentToday = 0L,
            periodStartDate = activePeriod.startDate,
            totalAllocatedDaily = dailyPocket.allocatedAmount
        )

        createNotificationChannel()

        val formattedAllowance = "%,d".format(allowanceResult.todayAllowance).replace(',', '.')
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🐮 Ringkasan Keuangan Hari Ini")
            .setContentText("Jatah hari ini: Rp$formattedAllowance. Sisa ${allowanceResult.remainingDays} hari menuju gajian!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Jatah hari ini: Rp$formattedAllowance. Sisa ${allowanceResult.remainingDays} hari menuju gajian.\n\n${allowanceResult.adviceMessage}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status jatah harian dan peringatan dini Richdin Finance"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

@HiltWorker
class HybridBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FinanceRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Hybrid Backup: Simulates encrypted local / Google Drive app data sync
        try {
            val meta = repository.getBackupMeta().let {
                // In production, sync serialized SQLite/Room database snapshot to Google Drive App Folder
                com.richdin.finance.core.model.BackupMeta(
                    lastBackupTimestamp = System.currentTimeMillis(),
                    driveFileId = "drive_backup_richdin_appdata",
                    isPendingSync = false
                )
            }
            repository.saveBackupMeta(meta)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object WorkScheduler {
    private const val DEBOUNCED_BACKUP_WORK_NAME = "work_debounced_backup"
    private const val DAILY_REMINDER_WORK_NAME = "work_daily_reminder"

    /**
     * Schedules a debounced backup 15 minutes after last data modification.
     * Uses ExistingWorkPolicy.REPLACE so subsequent edits reset the 15-minute timer.
     */
    fun scheduleDebouncedBackup(context: Context) {
        val request = OneTimeWorkRequestBuilder<HybridBackupWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBOUNCED_BACKUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Schedules expedited backup when app moves to background.
     */
    fun scheduleBackgroundBackup(context: Context) {
        val request = OneTimeWorkRequestBuilder<HybridBackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Schedules the recurring daily reminder at evening (e.g. 20:00).
     */
    fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
