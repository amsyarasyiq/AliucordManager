package com.aliucord.manager.patcher.steps.download

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.aliucord.manager.manager.OverlayManager
import com.aliucord.manager.manager.PathManager
import com.aliucord.manager.network.utils.SemVer
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.base.*
import com.aliucord.manager.patcher.steps.patch.ReorganizeDexStep
import com.aliucord.manager.patcher.steps.prepare.FetchInfoStep
import com.aliucord.manager.ui.components.dialogs.CustomComponentVersionPicker
import dev.wintry.manager.BuildConfig
import dev.wintry.manager.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Download a compiled dex file to be injected into the APK as the first `classes.dex` to override an entry point class.
 * Provides [ReorganizeDexStep] with the dex through the [IDexProvider] implementation.
 */
@Stable
class DownloadInjectorStep : DownloadStep(), IDexProvider, KoinComponent {
    private val paths: PathManager by inject()
    private val overlays: OverlayManager by inject()

    /**
     * This is populated right before the download starts (ref: [execute])
     */
    private var isCustomVersion: Boolean = false
    lateinit var targetVersion: SemVer
        private set

    override val localizedName = R.string.patch_step_dl_injector
    override val targetUrl = URL
    override val targetFile
        get() = paths.cachedInjectorDex(targetVersion, custom = isCustomVersion)

    override suspend fun execute(container: StepRunner) {
        val customVersions = mutableStateListOf<SemVer>()
        customVersions += paths.customInjectorDexs() ?: emptyList()

        // Prompt to select or manage custom versions instead of downloading
        if (customVersions.isNotEmpty()) {
            container.log("Custom versions present, waiting for selection: ${customVersions.joinToString()}")
            val selectedVersion = overlays.startComposableForResult { onResult ->
                CustomComponentVersionPicker(
                    componentTitle = "Injector",
                    versions = customVersions,
                    onConfirm = { version -> onResult(version) },
                    onDelete = { version ->
                        try {
                            paths.cachedInjectorDex(version, custom = true).delete()
                            customVersions.remove(version)

                            // Dismiss if no custom versions left
                            if (customVersions.isEmpty())
                                onResult(null)
                        } catch (t: Throwable) {
                            Log.e(BuildConfig.TAG, "Failed to delete custom component", t)
                        }
                    },
                    onCancel = { onResult(null) },
                )
            }

            if (selectedVersion != null) {
                isCustomVersion = true
                targetVersion = selectedVersion
                state = StepState.Skipped
                container.log("Using local custom injector version $selectedVersion")
                return
            } else {
                container.log("Selection dialog cancelled, continuing as normal")
            }
        }

        targetVersion = container.getStep<FetchInfoStep>()
            .data.injectorVersion
        container.log("Downloading injector version $targetVersion")

        super.execute(container)
    }

    private companion object {
        const val ORG = "Aliucord"
        const val MAIN_REPO = "Aliucord"

        const val URL = "https://raw.githubusercontent.com/$ORG/$MAIN_REPO/builds/Injector.dex"
    }

    override val dexCount = 1
    override val dexPriority = 3
    override fun getDexFiles() = listOf(targetFile.readBytes())
}
