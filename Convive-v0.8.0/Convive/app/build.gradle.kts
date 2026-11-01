plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/** Lee una configuración de las variables de entorno o de las propiedades de Gradle. */
fun config(name: String): String =
    (System.getenv(name) ?: (project.findProperty(name) as String?) ?: "").trim()

android {
    namespace = "pe.convive"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.condoapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "0.9.1"

        // Supabase (opcional). Sin estos valores la app funciona en modo local.
        // Solo la clave publishable: la service_role nunca va en la app.
        buildConfigField("String", "SUPABASE_URL", "\"${config("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${config("SUPABASE_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    testOptions {
        // Los métodos de android.jar devuelven valores por defecto en las pruebas JVM.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    // Animaciones de LottieFiles (archivos .json en res/raw)
    implementation("com.airbnb.android:lottie-compose:6.6.2")

    testImplementation("junit:junit:4.13.2")
}
