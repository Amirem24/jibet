plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// نسخه از CI قابل پاس‌دادن است: ./gradlew -PjibetVersionCode=12 -PjibetVersionName=2.2.0
val jibetVersionCode: Int = (project.findProperty("jibetVersionCode") as String?)?.toIntOrNull() ?: 9
val jibetVersionName: String = (project.findProperty("jibetVersionName") as String?) ?: "2.3.4"

// امضای ریلیز — از Secrets خوانده می‌شود؛ اگر نبود، ریلیز بدون امضای اختصاصی بیلد می‌شود (فقط محلی)
val keystoreFile: String? = System.getenv("JB_KEYSTORE_PATH")
val keystorePass: String? = System.getenv("JB_STORE_PASSWORD")
val keyAliasName: String? = System.getenv("JB_KEY_ALIAS")
val keyPass: String? = System.getenv("JB_KEY_PASSWORD")
val hasReleaseSigning = !keystoreFile.isNullOrBlank() && !keystorePass.isNullOrBlank() &&
    !keyAliasName.isNullOrBlank() && !keyPass.isNullOrBlank()

android {
    namespace = "com.jibet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jibet.app"
        minSdk = 26
        targetSdk = 35
        versionCode = jibetVersionCode
        versionName = jibetVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePass
                keyAlias = keyAliasName
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint { abortOnError = false }
}

// Room — خروجی اسکیمای هر نسخه در ریپو (پیش‌نیاز تست مهاجرت با MigrationTestHelper)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    // بلور واقعی پس‌زمینه (backdrop-filter وب) — برای قرص شیشه‌ای نوبار؛ روی API 31+ فعال است و پایین‌تر فقط tint می‌ماند
    implementation("dev.chrisbanes.haze:haze:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
