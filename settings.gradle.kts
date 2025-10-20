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
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // GitHub Packages 仓库
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Redamancywu/FusionKit-AutoRegister")
//            credentials {
//                username = providers.gradleProperty("gpr.user").orElse(System.getenv("GITHUB_ACTOR") ?: "").get()
//                password = providers.gradleProperty("gpr.key").orElse(System.getenv("GITHUB_TOKEN") ?: "").get()
//            }
        }
        // JitPack 仓库 - 支持私有仓库
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FusionKit"
include(":app")
include(":FusionKit-AutoRegister-Processor")

