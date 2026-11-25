plugins {
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.android.multiplatform.library)
}

kotlin {
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class) abiValidation()
  compilerOptions { optIn.add("kotlin.RequiresOptIn") }

  jvm()
  android {
    namespace = "org.fdroid.core"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21
    withHostTestBuilder {}.configure {}
    withDeviceTestBuilder { sourceSetTreeName = "test" }
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
  }

  sourceSets {
    commonMain { dependencies {} }
    commonTest { dependencies { implementation(kotlin("test")) } }
  }
}
