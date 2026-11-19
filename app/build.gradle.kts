import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: read from keystore.properties (path in KEYSTORE_PROPERTIES, default ~/openliquidwidget-signing/keystore.properties).
// Nothing secret is stored in the repository. Without credentials the release falls back to the debug key.
val signingProps = Properties().apply {
    val f = file(System.getenv("KEYSTORE_PROPERTIES") ?: "${System.getProperty("user.home")}/openliquidwidget-signing/keystore.properties")
    if (f.exists()) f.inputStream().use(::load)
}
val hasReleaseSigning = signingProps.getProperty("storeFile")?.let { file(it).exists() } == true

android {
    namespace = "io.github.linuxkernel44.openliquidwidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.linuxkernel44.openliquidwidget"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        androidResources.localeFilters += arrayOf("en")
    }

    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += arrayOf("DebugProbesKt.bin", "kotlin-tooling-metadata.json", "META-INF/*.version")
        }
    }
    lint {
        checkReleaseBuilds = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(
                "OpenLiquidWidget-${variant.outputs.first().versionName.get()}-${variant.buildType}.apk",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.kyant.backdrop)
}
