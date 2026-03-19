plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

tasks.register("buildEapk") {
    group = "build"
    description = "Build .eapk (assemble + package .eapk)"
    dependsOn(":app:assembleDebug", ":ext:shadowJar")
    doLast {
        val gitHash = providers.exec { commandLine("git", "rev-parse", "HEAD") }
            .standardOutput.asText.get().trim().take(7)
        val version = "v$gitHash"
        val tag: String = project.findProperty("extId") as? String ?: "unknown"
        val src = project.file("app/build/outputs/apk/debug/app-debug.apk")
        if (!src.exists()) throw GradleException("APK not found: ${src.absolutePath}")
        val outDir = project.file("app/build")
        val outFile = project.file("${outDir.path}/${tag}-$gitHash.eapk")
        copy { from(src); into(outDir); rename { outFile.name } }
        println("EAPK: ${outFile.absolutePath}")
    }
}
