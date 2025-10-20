# FusionKit AutoRegister

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.28-green.svg)](https://github.com/google/ksp)
[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[🇺🇸 English](README_EN.md) | [🇨🇳 中文](README_CN.md)

A powerful Kotlin Symbol Processing (KSP) based auto-registration framework for multi-module Android projects. Automatically discover and register service implementations at compile time.

一个基于 Kotlin 符号处理 (KSP) 的强大自动注册框架，专为多模块 Android 项目设计。在编译时自动发现和注册服务实现。

## 📁 Project Structure

```
FusionKit/
├── FusionKit-AutoRegister-Processor/     # KSP Annotation Processor
│   ├── src/main/java/.../processor/      # Core processor implementation
│   ├── src/main/resources/META-INF/       # KSP service registration
│   └── README.md                          # Processor documentation
├── app/                                   # Example Android Application
│   ├── src/main/java/.../test/           # Test service implementations
│   ├── src/test/java/.../                # Unit tests
│   └── README.md                          # Usage examples
├── README.md                              # This file
├── README_EN.md                           # English documentation
├── README_CN.md                           # Chinese documentation
└── LICENSE                                # MIT License
```

## 🎯 What This Framework Does

- **Compile-time Service Discovery** - Automatically finds annotated classes
- **Zero Runtime Overhead** - No reflection, pure compile-time code generation
- **Multi-interface Support** - Register one implementation for multiple interfaces
- **Priority-based Selection** - Automatic best service selection
- **Environment Control** - Different implementations for DEBUG/RELEASE
- **Type-based Grouping** - Organize services by business type

## ✨ Key Features

- 🚀 **Compile-time Code Generation** - Zero runtime overhead
- 🎯 **Multi-interface Support** - Register one implementation for multiple interfaces
- 📊 **Priority-based Selection** - Automatic best service selection
- 🏷️ **Type-based Grouping** - Organize services by business type
- 🌍 **Environment Control** - Different implementations for DEBUG/RELEASE
- 🔄 **Object & Class Support** - Support both Kotlin objects and classes
- 📱 **Android Optimized** - Designed specifically for Android projects

## 🚀 Quick Example

```kotlin
// 1. Define interface
interface UserService {
    fun getUserInfo(): String
}

// 2. Register implementation
@AutoRegister(
    value = [UserService::class],
    name = "primary",
    type = "core",
    priority = 100
)
class UserServiceImpl : UserService {
    override fun getUserInfo(): String = "John Doe"
}

// 3. Use generated provider
val service = UserServiceProviders.get("primary")
```

## 📋 Requirements

- **Kotlin** 2.0.21+
- **KSP** 2.0.21-1.0.28+
- **Android Gradle Plugin** 8.12.3+
- **Gradle** 8.13+

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Redamancy** - *Initial work* - [GitHub](https://github.com/redamancy)

---

Made with ❤️ for the Android community
