package com.aliucord.manager.patcher.steps.patch

import com.aliucord.manager.R
import com.aliucord.manager.patcher.StepRunner
import com.aliucord.manager.patcher.steps.StepGroup
import com.aliucord.manager.patcher.steps.base.Step
import com.aliucord.manager.patcher.steps.download.CopyDependenciesStep
import com.aliucord.manager.patcher.util.ManifestPatcher
import com.aliucord.manager.ui.screens.patchopts.PatchOptions
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter

/**
 * Patch the APK's AndroidManifest.xml
 */
class PatchManifestStep(private val options: PatchOptions) : Step() {
    override val group = StepGroup.Patch
    override val localizedName = R.string.patch_step_patch_manifests

    override suspend fun execute(container: StepRunner) {
        val deps = container.getStep<CopyDependenciesStep>()
        for (apk in deps.patchedApks) {
            container.log("Reading manifest from apk")
            val manifest = ZipReader(apk)
                .use { zip -> zip.openEntry("AndroidManifest.xml")?.read() }
                ?: throw IllegalArgumentException("No manifest found in APK")

            container.log("Patching manifest")
            val patchedManifest = if (apk == deps.patchedApk)
                ManifestPatcher.patchManifest(
                    manifestBytes = manifest,
                    packageName = options.packageName,
                    appName = options.appName,
                    debuggable = options.debuggable,
                )
            else {
                ManifestPatcher.patchManifestPackageName(
                    manifestBytes = manifest,
                    packageName = options.packageName,
                )
            }

            container.log("Writing patched manifest to apk unaligned compressed")
            ZipWriter(apk, /* append = */ true).use {
                it.deleteEntry("AndroidManifest.xml", apk == deps.patchedLibApk || apk == deps.patchedApk)
                it.writeEntry("AndroidManifest.xml", patchedManifest)
            }
        }
    }
}
