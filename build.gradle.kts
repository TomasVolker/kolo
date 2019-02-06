import org.gradle.internal.os.OperatingSystem

plugins {
    application
    kotlin("jvm") version "1.3.20"
}

group = "tomasvolker"
version = "0.1"

repositories {
    mavenCentral()
    maven { url = uri("http://dl.bintray.com/tomasvolker/maven") }
    maven { url = uri("https://dl.bintray.com/openrndr/openrndr/") }
}



val openrndrVersion = "0.3.30"

val openrndrOS = when (OperatingSystem.current()) {
    OperatingSystem.WINDOWS -> "windows"
    OperatingSystem.LINUX -> "linux-x64"
    OperatingSystem.MAC_OS -> "macos"
    else -> error("unsupported OS")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    compile(files("lib/komputo-0.0.1.jar"))
    compile(group = "org.tensorflow", name = "tensorflow", version = "1.11.0")
    //api(group = "org.tensorflow", name = "libtensorflow_jni_gpu", version = "1.11.0")

    compile(group = "tomasvolker", name = "numeriko-core", version = "0.0.3")
    compile(group = "tomasvolker", name = "kyplot", version = "0.0.1")

    compile("com.xenomachina:kotlin-argparser:2.0.7")

    compile("org.openrndr:openrndr-core:$openrndrVersion")
    compile("org.openrndr:openrndr-extensions:$openrndrVersion")
    compile("org.openrndr:openrndr-ffmpeg:$openrndrVersion")
    
    runtime("org.openrndr:openrndr-gl3:$openrndrVersion")
    runtime("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")

}

application {

    mainClassName = "tomasvolker.kolo.ui.KoloProgramKt"

    applicationDefaultJvmArgs += "-Djava.library.path=./lib"

    if (openrndrOS == "macos")
        applicationDefaultJvmArgs += "-XstartOnFirstThread"

}

