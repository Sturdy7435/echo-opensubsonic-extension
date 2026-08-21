plugins {
    alias(libs.plugins.android.application)
}

dependencies {
    implementation(project(":ext"))
    compileOnly(libs.echo.common)
    compileOnly(libs.kotlin.stdlib)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

val extType = project.property("extType")
val extId = project.property("extId")
val extClass = project.property("extClass")

val extIconUrl = project.property("extIconUrl")
val extName = project.property("extName")
val extDescription = project.property("extDescription")

val extAuthor = project.property("extAuthor")
val extAuthorUrl = project.property("extAuthorUrl")

val extRepoUrl = project.property("extRepoUrl")
val extUpdateUrl = project.property("extUpdateUrl")

val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()
val verCode = gitCount
val verName = "v$gitHash"


val outputDir = file("${layout.buildDirectory.asFile.get()}/generated/proguard")
val generatedProguard = file("${outputDir}/generated-rules.pro")

tasks.register("generateProguardRules") {
    description = "Generate the Proguard rules"
    doLast {
        outputDir.mkdirs()
        generatedProguard.writeText(
            """
                -dontobfuscate
                -keep,allowoptimization class dev.brahmkshatriya.echo.extension.$extClass
                """.trimMargin()
        )
    }
}

tasks.named("preBuild") {
    dependsOn("generateProguardRules")
}

android {
    namespace = "dev.brahmkshatriya.echo.extension"
    compileSdk = 37

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo.extension.$extId"
        minSdk = 24
        targetSdk = 37

        manifestPlaceholders.apply {
            put("type", "dev.brahmkshatriya.echo.${extType}")
            put("id", extId!!)
            put("class_path", "dev.brahmkshatriya.echo.extension.${extClass}")
            put("version", verName)
            put("version_code", verCode.toString())
            put("icon_url", extIconUrl ?: "")
            put("app_name", "Echo : $extName Extension")
            put("name", extName!!)
            put("description", extDescription ?: "")
            put("author", extAuthor!!)
            put("author_url", extAuthorUrl ?: "")
            put("repo_url", extRepoUrl ?: "")
            put("update_url", extUpdateUrl ?: "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                generatedProguard.absolutePath
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()