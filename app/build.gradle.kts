plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.reporteya"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.reporteya"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val supabaseUrl = "https://uppdkjfjxtjnukftgwhz.supabase.co"
    val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVwcGRramZqeHRqbnVrZnRnd2h6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc3MDE3NjMsImV4cCI6MjA2MzI3Nzc2M30._bPFFm4NqghiVPmytaGkiD40QFc1Ct-oIQx1gOK0g74"
    val supabaseBucket = (project.findProperty("SUPABASE_BUCKET") as String?) ?: "public"
    val n8nWebhookUrl = "https://guillesilva04business.app.n8n.cloud/webhook/1"
    val inviteApiBase = (project.findProperty("INVITE_API_BASE") as String?) ?: ""
    val dniResolverUrl = (project.findProperty("DNI_RESOLVER_URL") as String?) ?: ""

    buildConfigField("String", "SUPABASE_URL", '"' + supabaseUrl + '"')
    buildConfigField("String", "SUPABASE_ANON_KEY", '"' + supabaseAnonKey + '"')
    buildConfigField("String", "SUPABASE_BUCKET", '"' + supabaseBucket + '"')
    buildConfigField("String", "N8N_WEBHOOK_URL", '"' + n8nWebhookUrl + '"')
    buildConfigField("String", "INVITE_API_BASE", '"' + inviteApiBase + '"')
    buildConfigField("String", "DNI_RESOLVER_URL", '"' + dniResolverUrl + '"')
  }

  buildTypes {
    debug {
      buildConfigField(
        "String",
        "INVITE_API_BASE",
        "\"https://assured-expenditure-stories-musical.trycloudflare.com\""
      )
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      buildConfigField(
        "String",
        "INVITE_API_BASE",
        "\"https://assured-expenditure-stories-musical.trycloudflare.com\""
      )
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
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  debugImplementation(libs.androidx.ui.tooling)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.test.manifest)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}
