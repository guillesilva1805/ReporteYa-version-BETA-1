plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.reporteya"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.reporteya"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val supabaseUrl = "https://uppdkjfjxtjnukftgwhz.supabase.co"
    val supabaseAnonKey = (project.findProperty("SUPABASE_ANON_KEY") as String?)
      ?: System.getenv("SUPABASE_ANON_KEY")
      ?: ""
    val supabaseBucket = (project.findProperty("SUPABASE_BUCKET") as String?) ?: "public"
    val n8nWebhookUrl = "https://guillesilva04business.app.n8n.cloud/webhook/1"

    buildConfigField("String", "SUPABASE_URL", '"' + supabaseUrl + '"')
    buildConfigField("String", "SUPABASE_ANON_KEY", '"' + supabaseAnonKey + '"')
    buildConfigField("String", "SUPABASE_BUCKET", '"' + supabaseBucket + '"')
    buildConfigField("String", "N8N_WEBHOOK_URL", '"' + n8nWebhookUrl + '"')
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true }
  buildFeatures {
    buildConfig = true
    compose = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.foundation)
  implementation(libs.androidx.ui.text)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.ui.tooling.preview)

  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.coil.compose)

  debugImplementation(libs.androidx.ui.tooling)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.test.manifest)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}
