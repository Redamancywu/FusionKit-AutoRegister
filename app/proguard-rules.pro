# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- FusionKit AutoRegister / Hot Reload ---
# 保留编译期生成的 Providers 类，命名结尾为 *Providers
-keep class **Providers { *; }

# 可选：保留热更新核心引擎与控制器（若在运行时使用这些 API）
-keep class com.horizon.fusionkit.autoregister.processor.HotReloadEngine { *; }
-keep class com.horizon.fusionkit.autoregister.processor.HotReloadController { *; }
-keep class com.horizon.fusionkit.autoregister.processor.ServiceLifecycleManager { *; }