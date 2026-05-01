import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

val metronUsername: String = localProps.getProperty("METRON_USERNAME") ?: ""
val metronPassword: String = localProps.getProperty("METRON_PASSWORD") ?: ""
val comicVineApiKey: String = localProps.getProperty("COMICVINE_API_KEY")
    ?: localProps.getProperty("API_KEY")
    ?: ""

// Claude wrote this, I couldn't figure out how to do secrets correctly so ggs
val generateApiConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/kotlin/commonMain")
    inputs.property("metronUsername", metronUsername)
    inputs.property("metronPassword", metronPassword)
    inputs.property("comicVineApiKey", comicVineApiKey)
    outputs.dir(outputDir)
    doLast {
        val mUser = inputs.properties["metronUsername"] as String
        val mPass = inputs.properties["metronPassword"] as String
        val cvKey = inputs.properties["comicVineApiKey"] as String
        val metronFile = outputDir.get().file("com/moravian/comictracker/network/MetronConfig.kt").asFile
        metronFile.parentFile.mkdirs()
        metronFile.writeText(
            "package com.moravian.comictracker.network\n\n" +
            "internal object MetronConfig {\n" +
            "    const val USERNAME = \"$mUser\"\n" +
            "    const val PASSWORD = \"$mPass\"\n" +
            "    const val COMICVINE_API_KEY = \"$cvKey\"\n" +
            "}\n"
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
        }
        commonMain {
            kotlin.srcDir(generateApiConfig)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    // Room KSP processors — one entry per KMP target
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

android {
    namespace = "com.moravian.comictracker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildFeatures { buildConfig = true }

    defaultConfig {
        applicationId = "com.moravian.comictracker"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

room {
    schemaDirectory("$projectDir/schemas")
}
