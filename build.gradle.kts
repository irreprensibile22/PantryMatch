// Plugin dichiarati a livello di progetto (apply false) e attivati nel modulo app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}
