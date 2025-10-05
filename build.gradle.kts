import org.jetbrains.changelog.Changelog
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
    id("org.jetbrains.changelog") version "2.2.0"
}

group = "com.zapshark.applydiff"
version = "0.2.8"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // IntelliJ IDEA Community 2025.1.4.1
        create("IC", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        // Add more bundledPlugin(...) if you need them
    }
}

/**
 * CHANGELOG plugin settings:
 * - reads CHANGELOG.md
 * - uses the section for this build's `version` or falls back to "Unreleased"
 */
changelog {
    version.set(project.version.toString())
    groups.set(listOf("Added", "Changed", "Fixed"))
}


intellijPlatform {
    pluginConfiguration {
        // Your plugin identity
        id = "com.zapshark.applydiff"
        name = "Apply Diff to Current File"
        description = "Apply diffs from clipboard to the current editor with preview. Supports fenced diffs, unified hunks, GitHub suggestions, and selection patches."

        vendor {
            name = "ZapShark Technologies"
            email = "opensource@zapshark.com"
            url = "https://plugins.jetbrains.com/plugin/28582-apply-diff-to-current-file/"
        }

        ideaVersion {
            sinceBuild = "251"
            // untilBuild = null
        }

        // Auto-fill change notes from CHANGELOG.md (as HTML)
        // NOTE: This overrides any <change-notes> that might be in plugin.xml at build time.
        changeNotes = providers.provider {
            val v = project.version.toString()
            val item = if (changelog.has(v)) changelog.get(v) else changelog.getUnreleased()
            changelog.renderItem(
                item.withHeader(false).withEmptySections(false),
                Changelog.OutputType.HTML
            )
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
