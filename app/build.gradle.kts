import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase (ofertas por notificação): só liga quando o arquivo do Firebase existir.
// Ver docs/ENVIAR-OFERTAS.md.
if (file("google-services.json").exists() || file("src/mestrecell/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Chave de assinatura da versão final. Fica fora do git (ver docs/PUBLICAR-DOWNLOAD.md).
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "br.com.mestrecell.mestrescan"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.mestrecell.mestrescan"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.0"
    }

    // Cada assistência (white label) é um "sabor": nome, logo, cores e WhatsApp
    // ficam em src/<sabor>/res e nos campos abaixo. O código é o mesmo para todos.
    flavorDimensions += "brand"
    productFlavors {
        create("mestrecell") {
            dimension = "brand"
            applicationId = "br.com.mestrecell.mestrescan"
            buildConfigField("String", "WHATSAPP_NUMBER", "\"5541991663168\"")
        }
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
}
