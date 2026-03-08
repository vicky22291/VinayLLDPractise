plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("jacoco")
}

group = "com.uber.jetbrains.reviewplugin"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        local("/Applications/IntelliJ IDEA.app")
        bundledPlugin("Git4Idea")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        id = "com.uber.jetbrains.reviewplugin"
        name = "Claude Code Review"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "243"
        }
    }
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

// Register a plain unit test task that bypasses IntelliJ Platform test infrastructure.
// Use this for model/DTO tests that have no IDE dependencies.
val unitTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs pure unit tests without IntelliJ Platform instrumentation"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs = emptyList()
    systemProperties.clear()
    useJUnit()
    finalizedBy(tasks.named("jacocoUnitTestReport"))
}

// IntelliJ Platform-dependent classes that cannot be unit tested (require IDE runtime).
// Excluded from coverage verification to keep the 95% threshold meaningful.
val platformDependentExcludes = listOf(
    "com/uber/jetbrains/reviewplugin/ui/ReviewEditorListener*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewLineMarkerProvider*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewInlayRenderer*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewBlockInlayRenderer*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewToolWindowFactory*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewToolWindowSwingPanel*",
    "com/uber/jetbrains/reviewplugin/ui/ReviewStatusBarWidget*",
    "com/uber/jetbrains/reviewplugin/ui/BranchSelectionDialog*",
    "com/uber/jetbrains/reviewplugin/listeners/ReviewFileWatcher*",
    "com/uber/jetbrains/reviewplugin/listeners/ReviewFileWatcherStartup*",
    "com/uber/jetbrains/reviewplugin/services/GitDiffService*",
    "com/uber/jetbrains/reviewplugin/actions/AddReplyAction*",
    "com/uber/jetbrains/reviewplugin/actions/ReplyToReviewAction*",
)

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn(unitTest)
    executionData(unitTest.get())
    sourceDirectories.setFrom(files("src/main/kotlin"))
    classDirectories.setFrom(
        fileTree("build/classes/kotlin/main") {
            include("com/uber/jetbrains/reviewplugin/model/**")
            include("com/uber/jetbrains/reviewplugin/services/**")
            include("com/uber/jetbrains/reviewplugin/listeners/**")
            include("com/uber/jetbrains/reviewplugin/ui/**")
            exclude(platformDependentExcludes)
        }
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoUnitTestCoverageVerification") {
    dependsOn(unitTest)
    executionData(unitTest.get())
    classDirectories.setFrom(
        fileTree("build/classes/kotlin/main") {
            include("com/uber/jetbrains/reviewplugin/model/**")
            include("com/uber/jetbrains/reviewplugin/services/**")
            include("com/uber/jetbrains/reviewplugin/listeners/**")
            include("com/uber/jetbrains/reviewplugin/ui/**")
            exclude(platformDependentExcludes)
        }
    )
    violationRules {
        rule {
            limit {
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}
