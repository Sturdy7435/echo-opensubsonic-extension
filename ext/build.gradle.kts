import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.gradle.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    compileOnly(libs.echo.common)
    compileOnly(libs.kotlin.stdlib)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    implementation(libs.kotlinx.coroutines.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

// Extension properties goto `gradle.properties` to set values

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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "dev.brahmkshatriya.echo.extension"
            artifactId = extId.toString()
            version = verName

            from(components["java"])
        }
    }
}

tasks {
    shadowJar {
        archiveBaseName.set(extId.toString())
        archiveVersion.set(verName)
        manifest {
            attributes(
                mapOf(
                    "Extension-Id" to extId,
                    "Extension-Type" to extType,
                    "Extension-Class" to extClass,

                    "Extension-Version-Code" to verCode,
                    "Extension-Version-Name" to verName,

                    "Extension-Icon-Url" to extIconUrl,
                    "Extension-Name" to extName,
                    "Extension-Description" to extDescription,

                    "Extension-Author" to extAuthor,
                    "Extension-Author-Url" to extAuthorUrl,

                    "Extension-Repo-Url" to extRepoUrl,
                    "Extension-Update-Url" to extUpdateUrl,
                ),
            )
        }
    }
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()

val compileKotlin = tasks.named<KotlinCompile>("compileKotlin").get()
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}