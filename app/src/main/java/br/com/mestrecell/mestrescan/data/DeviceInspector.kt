package br.com.mestrecell.mestrescan.data

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** Lê do Android as informações de cada app instalado pelo usuário. */
class DeviceInspector(private val context: Context) {
    private val pm = context.packageManager

    fun userApps(): List<InstalledApp> {
        val launcherPackages = launcherPackages()
        val bootPackages = pm.queryBroadcastReceivers(Intent(Intent.ACTION_BOOT_COMPLETED), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        val admins = context.getSystemService(DevicePolicyManager::class.java)
            ?.activeAdmins.orEmpty()
            .groupBy { it.packageName }
        val accessibility = enabledAccessibilityPackages()

        @Suppress("DEPRECATION")
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        return packages.mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) return@mapNotNull null
            if (info.packageName == context.packageName) return@mapNotNull null

            val requested = info.requestedPermissions?.toSet().orEmpty()
            InstalledApp(
                packageName = info.packageName,
                label = appInfo.loadLabel(pm).toString(),
                installedAt = info.firstInstallTime,
                installer = installerOf(info.packageName),
                hasLauncherIcon = info.packageName in launcherPackages,
                adminComponents = admins[info.packageName].orEmpty(),
                hasAccessibility = info.packageName in accessibility,
                overlay = overlayState(info.packageName, appInfo.uid, requested),
                canInstallApps = Manifest.permission.REQUEST_INSTALL_PACKAGES in requested,
                startsOnBoot = info.packageName in bootPackages ||
                    Manifest.permission.RECEIVE_BOOT_COMPLETED in requested,
            )
        }
    }

    fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Nome do app, ou null se não existe mais ou é app de sistema. */
    fun userAppLabel(packageName: String): String? = try {
        val info = pm.getApplicationInfo(packageName, 0)
        if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) null else info.loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun launcherPackages(): Set<String> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .toSet()

    fun homePackages(): Set<String> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()

    private fun enabledAccessibilityPackages(): Set<String> {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return emptySet()
        return raw.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            .toSet()
    }

    private fun installerOf(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Nem todo Android deixa consultar se a sobreposição foi concedida a outro app;
     * quando não dá, ficamos com "pede a permissão".
     */
    private fun overlayState(packageName: String, uid: Int, requested: Set<String>): OverlayState {
        if (Manifest.permission.SYSTEM_ALERT_WINDOW !in requested) return OverlayState.NONE
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return OverlayState.REQUESTED
        val mode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, packageName)
            }
        } catch (e: Exception) {
            null
        }
        return if (mode == AppOpsManager.MODE_ALLOWED) OverlayState.GRANTED else OverlayState.REQUESTED
    }
}
