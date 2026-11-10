// Plugins declared at project level (apply false) and applied in the app module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}
