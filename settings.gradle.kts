import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import kotlin.io.path.createTempFile

val androidSdkDir = file(".android-sdk")
ensureAndroidSdk(androidSdkDir)
ensureLocalProperties(file("local.properties"), androidSdkDir)

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Noteds"
include(":app")

fun ensureLocalProperties(localProperties: File, sdkDir: File) {
    val escapedPath = sdkDir.absolutePath.replace("\\", "\\\\")
    val desiredEntry = "sdk.dir=$escapedPath"
    val otherEntries = if (localProperties.exists()) {
        localProperties.readLines().filterNot { it.startsWith("sdk.dir=") }.filter { it.isNotBlank() }
    } else {
        emptyList()
    }
    val finalLines = (otherEntries + desiredEntry)
    localProperties.writeText(finalLines.joinToString(separator = System.lineSeparator()) + System.lineSeparator())
}

fun ensureAndroidSdk(targetDir: File) {
    val platformJar = targetDir.resolve("platforms/android-35/android.jar")
    val buildToolsDir = targetDir.resolve("build-tools/35.0.0")
    if (platformJar.exists() && buildToolsDir.exists()) {
        return
    }

    val cmdlineToolsDir = targetDir.resolve("cmdline-tools/latest")
    if (!cmdlineToolsDir.exists()) {
        downloadCommandLineTools(targetDir, cmdlineToolsDir)
    }

    installWithSdkManager(
        sdkRoot = targetDir,
        packages = listOf("platform-tools", "platforms;android-35", "build-tools;35.0.0")
    )
}

fun downloadCommandLineTools(sdkRoot: File, destinationDir: File) {
    val zipUrl = "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    val tempZip = createTempFile("cmdline-tools", ".zip").toFile()
    println("Downloading Android command line tools from $zipUrl")
    URI(zipUrl).toURL().openStream().use { input ->
        tempZip.outputStream().use { output -> input.copyTo(output) }
    }

    val tempDir = sdkRoot.resolve("tmp-cmdline-tools").apply { deleteRecursively(); mkdirs() }
    unzip(tempZip, tempDir)
    val extractedDir = tempDir.resolve("cmdline-tools")
    destinationDir.parentFile?.mkdirs()
    destinationDir.deleteRecursively()
    extractedDir.copyRecursively(destinationDir, overwrite = true)
    tempZip.delete()
    tempDir.deleteRecursively()
}

fun unzip(zipFile: File, targetDir: File) {
    ZipInputStream(zipFile.inputStream().buffered()).use { zipInputStream ->
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            val outFile = targetDir.resolve(entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output -> zipInputStream.copyTo(output) }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }
}

fun installWithSdkManager(sdkRoot: File, packages: List<String>) {
    val sdkManager = sdkRoot.resolve("cmdline-tools/latest/bin/sdkmanager")
    require(sdkManager.exists()) { "Unable to find sdkmanager at ${sdkManager.absolutePath}" }
    sdkManager.setExecutable(true)
    val command = mutableListOf(sdkManager.absolutePath, "--sdk_root=${sdkRoot.absolutePath}")
    command.addAll(packages)
    println("Installing Android SDK components: ${packages.joinToString()}")
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    thread(isDaemon = true) {
        process.outputStream.bufferedWriter().use { writer ->
            repeat(64) {
                writer.appendLine("y")
                writer.flush()
                Thread.sleep(50)
            }
        }
    }
    process.inputStream.bufferedReader().forEachLine { println(it) }
    val exitCode = process.waitFor()
    check(exitCode == 0) { "sdkmanager exited with $exitCode" }
}
