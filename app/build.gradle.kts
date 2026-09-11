plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.diffplug.spotless")
}
android {
    namespace = "org.alignertracker.app"
    compileSdk = 36
    sourceSets.getByName("main").java.srcDir("../wear-transport/src/main/kotlin")
    defaultConfig {
        applicationId = "org.alignertracker.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    sourceSets.getByName("androidTest").assets.srcDir("schemas")
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("maven.repo.local", rootProject.layout.projectDirectory.dir(".gradle/robolectric-maven").asFile.absolutePath)
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
        }
    }
    lint { abortOnError = true; warningsAsErrors = false }
    buildTypes { debug { isPseudoLocalesEnabled = true }; release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
spotless { kotlin { target("src/**/*.kt"); ktfmt("0.58").kotlinlangStyle() } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("org.microg.gms:play-services-wearable:0.3.14.250932")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.11.01"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
