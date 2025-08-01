package com.aliucord.manager.patcher.steps.download

import androidx.compose.runtime.Stable
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.services.AliucordMavenService
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.network.utils.getOrThrow
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.DownloadStep
import com.aliucord.manager.patcher.steps.base.IDexProvider
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.github.diamondminer88.zip.ZipReader
import dev.wintry.manager.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a packaged AAR of the latest Aliuhook build from the Aliucord maven.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadAliuhookStep : DownloadStep(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val maven: AliucordMavenService by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    lateinit var targetVersion: SemVer
        private set

    override val localizedName = R.string.patch_step_dl_aliuhook
    override val targetUrl get() = AliucordMavenService.getAliuhookUrl(targetVersion.toString())
    override val targetFile get() = paths.cachedAliuhookAAR(targetVersion)

    override suspend fun execute(container: StepRunner) {
        container.log("Obtaining latest aliuhook version")
        targetVersion = maven.getAliuhookVersion(force = true).getOrThrow()
        container.log("Fetched aliuhook version: $targetVersion")

        super.execute(container)
    }

    override val dexPriority = 0
    override val dexCount = 1
    override fun getDexFiles(): List<ByteArray> {
        val dexBytes = ZipReader(targetFile).use { zip ->
            zip.openEntry("classes.dex")?.read()
                ?: throw IllegalStateException("No prebuilt classes.dex in downloaded aliuhook build")
        }

        return listOf(dexBytes)
    }
}
