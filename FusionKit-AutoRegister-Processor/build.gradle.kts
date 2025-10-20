plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("maven-publish")
    id("signing")
}

group = "com.redamancy.fusionkit"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    implementation("com.squareup:kotlinpoet:2.1.0")
}

// GitHub Packages 发布配置
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Redamancywu/FusionKit-AutoRegister")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            artifactId = "autoregister-processor"
            
            pom {
                name.set("FusionKit AutoRegister Processor")
                description.set("A powerful Kotlin Symbol Processing (KSP) based auto-registration framework for multi-module Android projects")
                url.set("https://github.com/Redamancywu/FusionKit-AutoRegister")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("Redamancywu")
                        name.set("Redamancy")
                        email.set("22340676@qq.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/Redamancywu/FusionKit-AutoRegister.git")
                    developerConnection.set("scm:git:ssh://github.com:Redamancywu/FusionKit-AutoRegister.git")
                    url.set("https://github.com/Redamancywu/FusionKit-AutoRegister")
                }
            }
        }
    }
}

