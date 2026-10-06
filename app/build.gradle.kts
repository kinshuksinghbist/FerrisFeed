plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.baselineprofile)
}

/**
 * Mirrors repo-root `content/*.json` (minus the schema) into generated
 * assets. Incremental: re-runs only when content changes.
 */
val syncCurriculumAssets = tasks.register<Sync>("syncCurriculumAssets") {
    // NB: exclude() must sit at task level, NOT nested inside from() { }.
    // A from(...) { exclude(...) } closure mis-scopes its delegate and
    // breaks :app configuration with phantom "compileSdk not specified" +
    // "hilt-android not found" errors (bisected 2026-10-06).
    from(rootProject.file("content"))
    exclude("schema.json")
    into(layout.buildDirectory.dir("generated/curriculum"))
}

android {
    namespace = "com.ferrisfeed.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ferrisfeed.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-mvp"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // Compose compiler is driven by the kotlin-compose plugin; no explicit version here.
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    baselineProfile {
        // Generates startup + scroll baselines into src/main/baselineProfiles/
        automaticGenerationDuringBuild = true
    }
    sourceSets {
        // Single source of truth stays in repo-root content/ (validated JSON).
        // This syncs it into generated assets every build so :data's
        // CurriculumSeeder can populate Room on first launch. Do NOT check
        // generated files in and do NOT hand-copy JSON into src/main/assets.
        getByName("main").assets.srcDir(syncCurriculumAssets)
    }
}

room {
    // Generates Room schema export for the prepackaged curriculum DB.
    schemaDirectory("$projectDir/schemas")
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":feature-feed"))
    implementation(project(":feature-path"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val bom = libs.compose.bom
    implementation(platform(bom))
    androidTestImplementation(platform(bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation3 — type-safe backstack NavDisplay
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.workmanager.ktx)
    implementation(libs.hilt.work)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.manifest)
}
