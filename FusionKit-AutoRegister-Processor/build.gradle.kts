plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

// 与 1.0.3 一致的 groupId
group = "io.github.redamancywu"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    // 交由插件生成 javadocJar，避免重复
    // withJavadocJar()
}

// 移除额外资源注入，保持与 1.0.3 风格一致
// tasks.jar {
//     from("proguard-rules.pro")
//     archiveClassifier.set("")
// }

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    implementation("com.squareup:kotlinpoet:2.1.0")
}

// Maven Central 发布配置（Central Portal），与 1.0.3 元数据一致
mavenPublishing {
    coordinates("io.github.redamancywu", "FusionKit-AutoRegister-Processor", version.toString())
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

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
            // 修正为与 1.0.3 相同的 SSH 连接写法（使用斜杠而非冒号）
            developerConnection.set("scm:git:ssh://github.com/Redamancywu/FusionKit-AutoRegister.git")
            url.set("https://github.com/Redamancywu/FusionKit-AutoRegister")
        }
    }
}

// GitHub Packages 发布配置（保持不变）
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Redamancywu/FusionKit-AutoRegister")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            artifactId = "autoregister-processor"
            pom {
                name.set("FusionKit AutoRegister Processor")
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
                    developerConnection.set("scm:git:ssh://github.com/Redamancywu/FusionKit-AutoRegister.git")
                    url.set("https://github.com/Redamancywu/FusionKit-AutoRegister")
                }
            }
        }
    }
}

