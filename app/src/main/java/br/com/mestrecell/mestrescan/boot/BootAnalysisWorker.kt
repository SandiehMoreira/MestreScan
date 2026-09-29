package br.com.mestrecell.mestrescan.boot

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.mestrecell.mestrescan.data.AppPrefs
import br.com.mestrecell.mestrescan.data.RuleSet
import br.com.mestrecell.mestrescan.system.Alerts
import br.com.mestrecell.mestrescan.system.Permissions
import java.util.concurrent.TimeUnit

/** Roda alguns minutos depois de ligar, salva o reinício e avisa se achou culpado. */
class BootAnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!Permissions.hasUsageAccess(context)) return Result.success()

        val rules = RuleSet.load(context)
        val observer = BootObserver(context, rules)
        val current = BootHistory(context).refresh(observer).first()

        val trusted = AppPrefs(context).trusted
        val suspects = current.launches.filter {
            it.selfStarted && it.packageName !in trusted && !rules.isWhitelisted(it.packageName)
        }
        if (suspects.isNotEmpty()) Alerts.bootSuspects(context, suspects)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val delay = RuleSet.load(context).bootWindowSeconds.toLong()
            val request = OneTimeWorkRequestBuilder<BootAnalysisWorker>()
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("boot-analysis", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
