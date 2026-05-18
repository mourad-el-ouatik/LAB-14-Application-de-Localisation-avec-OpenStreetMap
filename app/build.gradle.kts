plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mapapplication"
    compileSdk = 36  // Utilisation du SDK 34 pour la compatibilité avec les API récentes

    defaultConfig {
        applicationId = "com.example.mapapplication"
        minSdk = 24  // Android 7.0 minimum pour assurer une large compatibilité
        targetSdk = 36  // Ciblage de la version récente pour les meilleures pratiques
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // Désactive la minification pour ce projet d'apprentissage
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8  // Utilisation de Java 8 pour les fonctionnalités modernes
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Bibliothèques Android standard
    implementation(libs.appcompat)  // Support des fonctionnalités récentes sur anciennes versions
    implementation(libs.material)   // Composants Material Design
    implementation(libs.activity)   // API Activity moderne
    implementation(libs.constraintlayout)  // Layout flexible et performant

    // Bibliothèques de test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Bibliothèques spécifiques au projet
    implementation(libs.volley)     // Pour les requêtes HTTP
    implementation(libs.maps.core)  // Support de base pour les cartes
    implementation(libs.osmdroid)   // Bibliothèque OpenStreetMap pour Android
}
