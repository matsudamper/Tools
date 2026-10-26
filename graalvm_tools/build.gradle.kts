import java.nio.file.LinkOption
import kotlin.io.path.createLinkPointingTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

plugins {
    id("java")
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("org.graalvm.buildtools.native") version "0.9.18"
}

base.archivesName.set("util")

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("net.matsudamper:command:1.0.2")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

val graalVmLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.matching("GraalVM Community"))
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(graalVmLauncher)
            imageName.set(base.archivesName.get())
            mainClass.set("net.matsudamper.tool.MainKt")
        }
    }
}

// Workaround: https://github.com/gradle/gradle/issues/28583
// nativeCompile / nativeTestCompile の前に壊れたシンボリックリンクを修復する
tasks.matching { it.name == "nativeCompile" || it.name == "nativeTestCompile" }.configureEach {
    doFirst {
        val binPath = graalVmLauncher.get().executablePath.asFile.toPath().parent
        val svmBinPath = binPath.resolve("../lib/svm/bin")
        fixSymlink(binPath.resolve("native-image"), svmBinPath.resolve("native-image"))
    }
}

// Workaround: https://github.com/gradle/gradle/issues/28583
// Gradle のコピー処理で GraalVM JDK 内のシンボリックリンクが空ファイルに化ける問題を修正する。
// 空ファイルになった native-image を削除し、実体へのハードリンクとして再作成する。
fun fixSymlink(target: java.nio.file.Path, expectedSrc: java.nio.file.Path) {
    if (!expectedSrc.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
        logger.info("fixSymlink: expected is not regular, skip (expected: {})", expectedSrc)
        return
    }
    if (!target.isRegularFile(LinkOption.NOFOLLOW_LINKS) || target.fileSize() > 0) {
        logger.info("fixSymlink: target is not regular or the file size > 0, skip (target: {})", target)
        return
    }
    logger.warn("fixSymlink: {} -> {}", target, expectedSrc)
    target.deleteExisting()
    target.createLinkPointingTo(expectedSrc)
}
