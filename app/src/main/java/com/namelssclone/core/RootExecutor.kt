package com.namelssclone.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootExecutor {

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    suspend fun runCommand(command: String): List<String> = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        result.out
    }

    suspend fun prepareClone(packageName: String, cloneId: Int, ephemeral: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val ephemeralFlag = if (ephemeral) "1" else "0"
        val cmd = "/data/adb/namelssclone/scripts/mount_clone.sh $packageName $cloneId $ephemeralFlag"
        val result = Shell.cmd(cmd).exec()
        result.isSuccess
    }

    suspend fun stopClone(packageName: String, cloneId: Int): Boolean = withContext(Dispatchers.IO) {
        val cmd = "/data/adb/namelssclone/scripts/cleanup_clone.sh $packageName $cloneId"
        val result = Shell.cmd(cmd).exec()
        result.isSuccess
    }
}
