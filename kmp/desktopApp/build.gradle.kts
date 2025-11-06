plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.6.0"
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.words.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Words"
            packageVersion = "1.0.0"
            description = "A word guessing game"
            vendor = "Words Team"

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
        }
    }
}
