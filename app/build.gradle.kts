import org.gradle.api.JavaVersion.VERSION_17
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val localPropertiesFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropertiesFile.exists()) load(localPropertiesFile.inputStream())
}
val mapboxPublicToken: String = localProps.getProperty("MAPBOX_PUBLIC_TOKEN", "")

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties: Properties? = if (keystorePropertiesFile.exists()) {
    Properties().apply { load(keystorePropertiesFile.inputStream()) }
} else null

val versionPropertiesFile = file("version.properties")
val versionProperties = Properties().apply { load(versionPropertiesFile.inputStream()) }

// Maven-style -SNAPSHOT versioning. version.properties holds e.g. "9-SNAPSHOT", meaning
// "the next release will be 9, currently in development". The -SNAPSHOT lives only here as a
// marker — Android's versionCode must be an integer, so it is stripped for the actual build.
val rawVersion = versionProperties["versionCode"].toString().trim()
val isSnapshot = rawVersion.endsWith("-SNAPSHOT")
val releaseVersionCode = rawVersion.removeSuffix("-SNAPSHOT").toInt()

val buildingRelease = gradle.startParameter.taskNames.any {
    val t = it.substringAfterLast(':')
    t.equals("releaseBundle", ignoreCase = true) || t.equals("bundleRelease", ignoreCase = true)
}

// A release build "consumes" the -SNAPSHOT (ships the clean number) and advances
// version.properties to the next -SNAPSHOT for ongoing development.
if (buildingRelease && isSnapshot) {
    versionProperties["versionCode"] = "${releaseVersionCode + 1}-SNAPSHOT"
    versionProperties.store(versionPropertiesFile.outputStream(), null)
    println("Release versionCode = $releaseVersionCode  →  next dev version ${releaseVersionCode + 1}-SNAPSHOT")
}

tasks.register("releaseBundle") {
    finalizedBy("bundleRelease")
}

configure<ApplicationExtension> {
    namespace = "sk.spacirkovnik"
    compileSdk = 37

    defaultConfig {
        applicationId = "sk.spacirkovnik"
        minSdk = 24
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = if (buildingRelease) releaseVersionCode.toString() else rawVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPBOX_PUBLIC_TOKEN"] = mapboxPublicToken
        resValue("string", "mapbox_access_token", mapboxPublicToken)
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Bundle native debug symbols (e.g. Mapbox NDK libs) into the AAB so Play Console
            // can symbolicate native crashes/ANRs — removes the "missing debug symbols" warning.
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.play.services.location)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.auth)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}