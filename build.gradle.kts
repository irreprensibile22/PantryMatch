// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Abilita dopo aver aggiunto google-services.json (vedi README, sezione Firebase)
    alias(libs.plugins.google.services) apply false
}