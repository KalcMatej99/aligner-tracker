plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.8" apply false
    id("com.diffplug.spotless") version "7.2.1"
}

spotless { kotlin { target("wear-transport/src/**/*.kt"); ktfmt("0.58").kotlinlangStyle() } }
