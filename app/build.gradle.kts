import org.gradle.api.file.FileCollection
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    jacoco
}

android {
    namespace = "com.example.campushub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.edu.uri.santiago.campushub"
        minSdk = 37
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.google.firebase:firebase-auth")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("org.hamcrest:hamcrest:2.2")
}

// ---------- Cobertura dos testes unitários (JaCoCo) ----------
jacoco { toolVersion = "0.8.12" }

// Classes que dependem de Android ou Firebase: cobertas por integração e E2E
val coverageExcludes = listOf(
    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*Activity*.*", "**/*Adapter*.*", "**/*ViewHolder*.*", "**/*Listener*.*",
    "**/*Repository*.*", "**/SeedEvents*.*", "**/MediaDbHelper*.*", "**/ImageUtils*.*",
    "**/AddressGeocoder*.*", "**/ImageCodec*.*", "**/QrCodeImage*.*", "**/RoleRouter*.*"
)

fun coverageClasses(): FileCollection {
    val buildDir = layout.buildDirectory.get().asFile
    val fromKotlinTask = files(tasks.matching { it.name == "compileDebugKotlin" })
        .asFileTree.matching { include("**/*.class") }
    val fromKnownDirs = listOf(
        "tmp/kotlin-classes/debug",
        "intermediates/built_in_kotlinc/debug",
        "intermediates/javac/debug"
    ).map { fileTree(File(buildDir, it)) { include("**/*.class") } }

    return files(fromKotlinTask, fromKnownDirs).asFileTree.matching { exclude(coverageExcludes) }
}

fun coverageExecutionData(): FileCollection =
    fileTree(layout.buildDirectory.get().asFile) {
        include("jacoco/*.exec", "outputs/unit_test_code_coverage/**/*.exec")
    }

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    group = "verification"
    description = "Executa os testes unitários e gera o relatório de cobertura (HTML e XML)."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(coverageExecutionData())
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoUnitTestCoverageVerification") {
    group = "verification"
    description = "Falha o build se a cobertura de linhas dos testes unitários for menor que 70%."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses())
    executionData.setFrom(coverageExecutionData())
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}