// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.multiplatform.library) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.ksp) apply false
  alias(libs.plugins.android.hilt) apply false
  alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
  alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
  alias(libs.plugins.jetbrains.compose.compiler) apply false
}
