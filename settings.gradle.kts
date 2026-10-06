pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // Version catalog `libs` is auto-loaded from gradle/libs.versions.toml —
    // no explicit versionCatalogs { create("libs") { from(...) } } block.
    // (An explicit `from(files(...))` plus auto-import counts as two `from`
    // calls and fails on Gradle 8.7+ / 9.x with "Multiple 'from' invocations".)
}
rootProject.name = "FerrisFeed"
include(":app")
include(":core-ui")
include(":feature-feed")
include(":feature-path")
include(":data")
