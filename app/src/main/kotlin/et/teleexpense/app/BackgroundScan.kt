package et.teleexpense.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Safety net: reads new telecom SMS every 15 minutes even if the live receiver was not delivered. */
class ScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = Repo.get(applicationContext)
        if (repo.prefs.isSetUp && repo.hasSmsPermission()) {
            repo.scanNew()
            repo.load()
        }
        return Result.success()
    }
}

object BackgroundScan {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ScanWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("tele-expense-scan", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
