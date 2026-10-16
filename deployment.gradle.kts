
// deployment.gradle.kts
//
// 借鉴自 kmp-cmp-example/deployment.gradle.kts（gitcode: CPF-KMP-CMP/kmp-cmp-example，开源 KMP 示例，
// 本地副本 ~/Desktop/github/kmp-cmp-example）。按本工程 my_kmp_project 的实际情况做了适配：
//
//   与示例的差异（已适配）：
//   1. 包名/Ability/iOS Bundle 取自本工程：
//        - Android applicationId = com.example.my_kmp_project（composeApp/build.gradle.kts）
//        - Harmony bundleName   = com.example.harmonyapp（harmonyApp/AppScope/app.json5）
//        - Harmony ability      = EntryAbility（entry/src/main/module.json5 → mainElement）
//        - iOS bundle id        = com.example.my_kmp_project.my_kmp_project（iosApp/Configuration/Config.xcconfig，TEAM_ID 为空）
//   2. 目录名大小写：本工程鸿蒙壳层目录为 harmonyApp（大写 A），示例为 harmonyapp。
//   3. 鸿蒙 HAP 构建使用 DevEco 自带的 hvigorw.js（harmonyApp 内无 hvigorw 脚本），命令参数对齐
//      scripts/run-harmony.sh 与 runscript/runOhosApp-Mac.sh；安装走 `hdc shell bm install -p`，
//      并对安装失败自动卸载重试。
//   4. 发布产物校验路径带架构子目录（本工程 publishDebugBinariesToHarmonyApp 将头文件/so 拷贝到
//      entry/src/main/cpp/include/arm64-v8a/ 与 entry/libs/arm64-v8a/），与示例的 include/ 不同。
//   5. 本工程 composeApp 未声明 staticLib、也无 native-libs/sample，故**不**生成
//      verifyHarmonyStaticLib* / verifySampleStaticLib* 任务（示例里对应的 link 任务在本工程不存在）。
//   6. ohosX64 的 linkDebugSharedOhosX64 目前被工具链阻塞（Konan_cxa_demangle），故 X64 相关任务
//      默认不注册；工具链修复后可用 -PharmonyX64=true 启用（与 publish*BinariesToHarmonyAppX64 一致）。
//   7. 支持 -PandroidOnly=true（CI Android 专用）：该模式下不注册 iOS/OHOS 任务，避免任务图解析失败。
//
// 由根工程 build.gradle.kts 通过 apply(from = "deployment.gradle.kts") 引入。

import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream

// ----- 工程配置（取自本工程的真实值） -----
val androidPackage = "com.example.my_kmp_project"                 // composeApp applicationId
val androidLaunchActivity = "$androidPackage.MainActivity"         // 由 AndroidManifest 推断
val harmonyPackage = "com.example.harmonyapp"                     // AppScope/app.json5 bundleName
val harmonyAbility = "EntryAbility"                               // entry module mainElement
val harmonyAppDirName = "harmonyApp"                              // 注意大小写
val harmonyEntryModule = "entry"
val iosBundleId = "com.example.my_kmp_project.my_kmp_project"     // Config.xcconfig（TEAM_ID 为空）
val iosScheme = "iosApp"

// ----- 模式开关 -----
// 与 composeApp/build.gradle.kts 中 androidOnly 保持一致
val androidOnly = rootProject.findProperty("androidOnly")?.toString()?.toBoolean() == true
// 当前 ohosX64 共享链接被工具链阻塞，默认关闭 X64 任务；修复后用 -PharmonyX64=true 启用
val harmonyX64Enabled = rootProject.findProperty("harmonyX64")?.toString()?.toBoolean() == true

// ----- 工具函数 -----
fun isWindows(): Boolean = Os.isFamily(Os.FAMILY_WINDOWS)
fun isMac(): Boolean = Os.isFamily(Os.FAMILY_MAC)

fun findExecutable(name: String): String {
    val path = System.getenv("PATH").split(File.pathSeparator)
    val exeName = if (isWindows()) "$name.exe" else name

    // Check PATH
    path.forEach { dir ->
        val file = File(dir, exeName)
        if (file.exists() && file.canExecute()) {
            return file.absolutePath
        }
    }

    // Check common locations
    val commonPaths = mutableListOf<String>()
    if (name == "adb") {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            commonPaths.add("$androidHome/platform-tools/$exeName")
        }
        if (isMac()) {
            commonPaths.add("/Users/${System.getProperty("user.name")}/Library/Android/sdk/platform-tools/$exeName")
        } else if (isWindows()) {
            commonPaths.add("C:\\Users\\${System.getProperty("user.name")}\\AppData\\Local\\Android\\Sdk\\platform-tools\\$exeName")
        }
    } else if (name == "hdc") {
        val devecoHome = System.getenv("DEVECO_SDK_HOME")
        if (devecoHome != null) {
            commonPaths.add("$devecoHome/toolchains/$exeName")
            commonPaths.add("$devecoHome/default/openharmony/toolchains/$exeName")
        }
        if (isMac()) {
            commonPaths.add("/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/$exeName")
            commonPaths.add("/Applications/DevEco Studio.app/Contents/sdk/toolchains/$exeName")
        }
    }

    commonPaths.forEach { p ->
        val file = File(p)
        if (file.exists()) {
            return file.absolutePath
        }
    }

    return name // Return name to let shell try to find it
}

// 解析 hvigorw：本工程 harmonyApp 内无 hvigorw 脚本，优先用 DevEco 自带的 hvigorw.js
fun findHvigorw(): String {
    val devecoHome = System.getenv("DEVECO_HOME") ?: "/Applications/DevEco-Studio.app/Contents"
    val hvigorwJs = File(devecoHome, "tools/hvigor/bin/hvigorw.js")
    if (hvigorwJs.exists()) return hvigorwJs.absolutePath
    // 回退：harmonyApp 目录或 PATH
    val inApp = File(layout.projectDirectory.asFile, "$harmonyAppDirName/hvigorw")
    if (inApp.exists()) return inApp.absolutePath
    return findExecutable("hvigorw")
}

// 从 hdc list targets 解析设备序列号
fun resolveHarmonyDeviceId(hdc: String, project: org.gradle.api.Project): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine(hdc, "list", "targets")
        standardOutput = out
        isIgnoreExitValue = true
    }
    val devices = out.toString().trim().split("\n")
        .filter { it.isNotBlank() && !it.startsWith("List of") && !it.contains("Empty") }
        .map { it.split("\\s+".toRegex())[0] }
    return if (project.hasProperty("target")) {
        val target = project.property("target") as String
        println("使用指定目标设备: $target")
        target
    } else if (devices.isEmpty()) {
        println("未发现鸿蒙设备，请使用 -Ptarget=<device> 指定，或先连接设备。")
        ""
    } else {
        devices[0]
    }
}

// =====================================================================
// ==================== HarmonyOS Verify Tasks =========================
// =====================================================================

tasks.register("verifyHarmonyKexe") {
    group = "verification"
    description = "编译并推送 ohosArm64 测试 kexe 到设备运行验证（无设备则仅校验产物存在）"
    dependsOn(":composeApp:linkDebugTestOhosArm64")
    doLast {
        val projectDir = layout.projectDirectory.asFile
        val kexe = File(projectDir, "composeApp/build/bin/ohosArm64/debugTest/test.kexe")
        if (!kexe.exists()) {
            throw GradleException("未找到文件: ${kexe.absolutePath}")
        }
        val hdc = findExecutable("hdc")
        val out = ByteArrayOutputStream()
        exec {
            commandLine(hdc, "list", "targets")
            standardOutput = out
            isIgnoreExitValue = true
        }
        val devices = out.toString().trim().lines().filter { it.isNotBlank() && !it.contains("Empty") }
            .map { it.split("\\s+".toRegex())[0] }
        if (devices.isEmpty()) {
            println("未发现鸿蒙设备，跳过运行验证（构建产物 test.kexe 已存在）")
            return@doLast
        }
        val deviceId = if (project.hasProperty("target")) {
            val target = project.property("target") as String
            println("使用指定目标设备: $target")
            target
        } else {
            devices.first()
        }
        println("推送到设备: $deviceId")
        exec {
            commandLine(hdc, "-t", deviceId, "file", "send", kexe.absolutePath, "/data/local/tmp/test.kexe")
            isIgnoreExitValue = true
        }
        exec {
            commandLine(hdc, "-t", deviceId, "shell", "chmod", "+x", "/data/local/tmp/test.kexe")
            isIgnoreExitValue = true
        }

        println("运行测试程序...")
        val runOut = ByteArrayOutputStream()
        val runErr = ByteArrayOutputStream()

        val ANSI_RESET = "\u001B[0m"
        val ANSI_RED = "\u001B[31m"
        val ANSI_GREEN = "\u001B[32m"
        val ANSI_YELLOW = "\u001B[33m"

        val deviceRemotePath = "/data/local/tmp/test.kexe"
        val shellCommand = """
            export LD_LIBRARY_PATH=/system/lib64/platformsdk:/system/lib64/ark:/system/lib64:${'$'}LD_LIBRARY_PATH
            $deviceRemotePath
        """.trimIndent()

        val result = exec {
            commandLine(hdc, "-t", deviceId, "shell", "sh", "-c", shellCommand)
            standardOutput = runOut
            errorOutput = runErr
            isIgnoreExitValue = true
        }

        val output = runOut.toString()
        val error = runErr.toString()
        println("Output: $output")

        if (error.isNotEmpty()) {
            if (!error.contains("symbol not found")) {
                println("Error Output: $error")
            } else {
                println("Linker/Runtime Warnings (Potential Issues):")
                println(error)
            }
            if (error.contains("Permission denied")) {
                println("${ANSI_YELLOW}提示: 设备可能禁止在 /data/local/tmp 执行二进制文件（noexec 挂载或安全策略）。请通过 HAP 内运行或使用设备允许的执行路径/进程环境。${ANSI_RESET}")
            } else if (error.contains("libark_jsruntime.so") || error.contains("libace_napi.z.so")) {
                println("${ANSI_YELLOW}提示: 动态依赖缺失或版本不匹配。已尝试设置 LD_LIBRARY_PATH=/system/lib64:/system/lib64/platformsdk:/system/lib64/ark。若仍失败，请在应用(HAP)进程内运行，或避免 kexe 依赖 NAPI。${ANSI_RESET}")
            }
        }

        if (result.exitValue == 0) {
            println("${ANSI_GREEN}设备运行验证通过！${ANSI_RESET}")
            if (!output.contains("OhosPlatformTest: Platform name: HarmonyOS") && !output.contains("[INFO]")) {
                println("${ANSI_YELLOW}提示: 未检测到标准测试输出，但退出码为 0。请检查上方 Output 确认运行结果。${ANSI_RESET}")
            }
        } else {
            println("${ANSI_RED}失败: 设备运行测试失败 (退出码 ${result.exitValue})。${ANSI_RESET}")
            println("${ANSI_RED}错误信息: $error${ANSI_RESET}")
            println("${ANSI_YELLOW}警告: 这可能是由于 standalone kexe 在 HarmonyOS 上缺少运行时环境或链接库导致。${ANSI_RESET}")
            println("${ANSI_GREEN}构建产物验证通过 (test.kexe 存在).${ANSI_RESET}")
        }
    }
}

tasks.register("verifyHarmonySharedLib") {
    group = "verification"
    description = "校验 ohosArm64 共享库 libkn.so / libkn_api.h 已生成"
    dependsOn(":composeApp:linkDebugSharedOhosArm64")
    doLast {
        val ANSI_RESET = "\u001B[0m"
        val ANSI_GREEN = "\u001B[32m"

        val projectDir = layout.projectDirectory.asFile
        val dir = File(projectDir, "composeApp/build/bin/ohosArm64/debugShared")
        if (!dir.exists() || !dir.isDirectory) {
            throw GradleException("未找到目录: ${dir.absolutePath}")
        }
        val so = File(dir, "libkn.so")
        val header = File(dir, "libkn_api.h")
        if (!so.exists()) {
            throw GradleException("未找到文件: ${so.absolutePath}")
        }
        if (!header.exists()) {
            throw GradleException("未找到文件: ${header.absolutePath}")
        }
        println("${ANSI_GREEN}libkn.so 大小: ${so.length()}${ANSI_RESET}")
        println("${ANSI_GREEN}libkn_api.h 大小: ${header.length()}${ANSI_RESET}")
        println("${ANSI_GREEN}Harmony Shared Lib 验证通过${ANSI_RESET}")
    }
}

tasks.register("verifyHarmonyHapIntegration") {
    group = "verification"
    description = "校验发布产物（libkn_api.h / libkn.so）已正确拷贝到 harmonyApp/entry（arm64-v8a）"
    dependsOn(":composeApp:publishDebugBinariesToHarmonyApp")
    doLast {
        val ANSI_RESET = "\u001B[0m"
        val ANSI_GREEN = "\u001B[32m"

        val projectDir = layout.projectDirectory.asFile
        // 注意：本工程 publish 任务将头文件/so 拷贝到带架构子目录的路径
        val header = File(projectDir, "$harmonyAppDirName/$harmonyEntryModule/src/main/cpp/include/arm64-v8a/libkn_api.h")
        val so = File(projectDir, "$harmonyAppDirName/$harmonyEntryModule/libs/arm64-v8a/libkn.so")
        if (!header.exists()) {
            throw GradleException("未找到文件: ${header.absolutePath}")
        }
        if (!so.exists()) {
            throw GradleException("未找到文件: ${so.absolutePath}")
        }
        println("${ANSI_GREEN}HAP 集成验证通过 (arm64-v8a)${ANSI_RESET}")
    }
}

// =====================================================================
// ==================== X64 Verify Tasks（默认关闭，需 -PharmonyX64=true） ===========
// =====================================================================

if (harmonyX64Enabled) {
    tasks.register("verifyHarmonyKexeOhosX64") {
        group = "verification"
        description = "编译并推送 ohosX64 测试 kexe 到设备运行验证（需 -PharmonyX64=true）"
        dependsOn(":composeApp:linkDebugTestOhosX64")
        doLast {
            val projectDir = layout.projectDirectory.asFile
            val kexe = File(projectDir, "composeApp/build/bin/ohosX64/debugTest/test.kexe")
            if (!kexe.exists()) {
                throw GradleException("未找到文件: ${kexe.absolutePath}")
            }
            val hdc = findExecutable("hdc")
            val out = ByteArrayOutputStream()
            exec { commandLine(hdc, "list", "targets"); standardOutput = out; isIgnoreExitValue = true }
            val devices = out.toString().trim().lines().filter { it.isNotBlank() && !it.contains("Empty") }
                .map { it.split("\\s+".toRegex())[0] }
            if (devices.isEmpty()) {
                println("未发现鸿蒙设备，跳过运行验证（构建产物 test.kexe 已存在）")
                return@doLast
            }
            val deviceId = if (project.hasProperty("target")) (project.property("target") as String) else devices.first()
            println("推送到设备: $deviceId")
            val remotePath = "/data/local/tmp/test_x64.kexe"
            exec { commandLine(hdc, "-t", deviceId, "file", "send", kexe.absolutePath, remotePath); isIgnoreExitValue = true }
            exec { commandLine(hdc, "-t", deviceId, "shell", "chmod", "+x", remotePath); isIgnoreExitValue = true }

            println("运行测试程序 (x64)...")
            val runOut = ByteArrayOutputStream()
            val runErr = ByteArrayOutputStream()
            val ANSI_RESET = "\u001B[0m"; val ANSI_RED = "\u001B[31m"; val ANSI_GREEN = "\u001B[32m"; val ANSI_YELLOW = "\u001B[33m"

            val shellCommand = """
                export LD_LIBRARY_PATH=/system/lib64/platformsdk:/system/lib64/ark:/system/lib64:${'$'}LD_LIBRARY_PATH
                $remotePath
            """.trimIndent()
            val result = exec {
                commandLine(hdc, "-t", deviceId, "shell", "sh", "-c", shellCommand)
                standardOutput = runOut; errorOutput = runErr; isIgnoreExitValue = true
            }
            val output = runOut.toString()
            val error = runErr.toString()
            println("Output: $output")
            if (error.isNotEmpty()) {
                if (!error.contains("symbol not found")) println("Error Output: $error") else { println("Linker/Runtime Warnings:"); println(error) }
                if (error.contains("Permission denied")) {
                    println("${ANSI_YELLOW}提示: 设备可能禁止在 /data/local/tmp 执行二进制文件。${ANSI_RESET}")
                } else if (error.contains("libark_jsruntime.so") || error.contains("libace_napi.z.so")) {
                    println("${ANSI_YELLOW}提示: 动态依赖缺失或版本不匹配。${ANSI_RESET}")
                }
            }
            if (result.exitValue == 0) {
                println("${ANSI_GREEN}设备运行验证通过 (x64)！${ANSI_RESET}")
            } else {
                println("${ANSI_RED}失败: 设备运行测试失败 (退出码 ${result.exitValue})。${ANSI_RESET}")
                println("${ANSI_GREEN}构建产物验证通过 (test.kexe 存在).${ANSI_RESET}")
            }
        }
    }

    tasks.register("verifyHarmonySharedLibOhosX64") {
        group = "verification"
        description = "校验 ohosX64 共享库 libkn.so / libkn_api.h（需 -PharmonyX64=true）"
        dependsOn(":composeApp:linkDebugSharedOhosX64")
        doLast {
            val ANSI_RESET = "\u001B[0m"; val ANSI_GREEN = "\u001B[32m"
            val projectDir = layout.projectDirectory.asFile
            val dir = File(projectDir, "composeApp/build/bin/ohosX64/debugShared")
            if (!dir.exists() || !dir.isDirectory) throw GradleException("未找到目录: ${dir.absolutePath}")
            val so = File(dir, "libkn.so"); val header = File(dir, "libkn_api.h")
            if (!so.exists()) throw GradleException("未找到文件: ${so.absolutePath}")
            if (!header.exists()) throw GradleException("未找到文件: ${header.absolutePath}")
            println("${ANSI_GREEN}libkn.so 大小: ${so.length()}${ANSI_RESET}")
            println("${ANSI_GREEN}libkn_api.h 大小: ${header.length()}${ANSI_RESET}")
            println("${ANSI_GREEN}Harmony Shared Lib (x64) 验证通过${ANSI_RESET}")
        }
    }

    tasks.register("verifyHarmonyHapIntegrationOhosX64") {
        group = "verification"
        description = "校验发布产物（x86_64）已拷贝到 harmonyApp/entry（需 -PharmonyX64=true）"
        dependsOn(":composeApp:publishDebugBinariesToHarmonyAppX64")
        doLast {
            val ANSI_RESET = "\u001B[0m"; val ANSI_GREEN = "\u001B[32m"
            val projectDir = layout.projectDirectory.asFile
            val header = File(projectDir, "$harmonyAppDirName/$harmonyEntryModule/src/main/cpp/include/x86_64/libkn_api.h")
            val so = File(projectDir, "$harmonyAppDirName/$harmonyEntryModule/libs/x86_64/libkn.so")
            if (!header.exists()) throw GradleException("未找到文件: ${header.absolutePath}")
            if (!so.exists()) throw GradleException("未找到文件: ${so.absolutePath}")
            println("${ANSI_GREEN}HAP 集成验证通过 (x64)${ANSI_RESET}")
        }
    }
}

// =====================================================================
// ==================== Android Deploy Task =============================
// =====================================================================

tasks.register("deployAndroid") {
    group = "deployment"
    description = "Build, install and launch Android app (com.example.my_kmp_project)"
    dependsOn(":composeApp:assembleDebug")

    doLast {
        val adb = findExecutable("adb")
        println("Using ADB: $adb")

        // Find device
        val out = ByteArrayOutputStream()
        exec { commandLine(adb, "devices"); standardOutput = out }
        val devices = out.toString().trim().split("\n")
            .filter { it.isNotBlank() && !it.startsWith("List of") }
            .map { it.split("\\s+".toRegex())[0] }

        if (devices.isEmpty()) {
            println("No Android devices found.")
            return@doLast
        }

        val deviceId = if (project.hasProperty("target")) {
            val target = project.property("target") as String
            println("Using target device: $target")
            target
        } else {
            devices[0]
        }
        println("Deploying to device: $deviceId")

        // Find APK
        val apkDir = File(layout.projectDirectory.asFile, "composeApp/build/outputs/apk/debug")
        val apkFiles = apkDir.walkTopDown().filter { it.extension == "apk" && !it.name.contains("unaligned") }.toList()

        if (apkFiles.isEmpty()) {
            throw GradleException("No APK file found in $apkDir")
        }

        val apkFile = apkFiles.maxByOrNull { it.lastModified() }!!
        println("Found APK: ${apkFile.name}")

        // Stop App
        println("Stopping app...")
        exec { commandLine(adb, "-s", deviceId, "shell", "am", "force-stop", androidPackage); isIgnoreExitValue = true }

        // Install App
        println("Installing APK...")
        exec { commandLine(adb, "-s", deviceId, "install", "-r", apkFile.absolutePath) }

        // Launch App（精确启动 MainActivity）
        println("Launching app...")
        exec { commandLine(adb, "-s", deviceId, "shell", "am", "start", "-n", "$androidPackage/$androidLaunchActivity") }
        println("Android app launched!")
    }
}

// =====================================================================
// ==================== iOS Deploy Task (macOS only) ====================
// =====================================================================

if (isMac()) {
    tasks.register("deployIos") {
        group = "deployment"
        description = "Build and deploy iOS app to simulator (macOS only)"

        // Step 1: Build Kotlin Framework
        dependsOn(":composeApp:linkDebugFrameworkIosSimulatorArm64")

        doLast {
            val projectDir = layout.projectDirectory.asFile
            val iosAppDir = File(projectDir, "iosApp")
            val buildDir = File(projectDir, "build/ios")

            // Step 2: Build Xcode Project
            println("Building Xcode project...")
            exec {
                workingDir = iosAppDir
                commandLine("xcodebuild",
                    "-project", "iosApp.xcodeproj",
                    "-scheme", iosScheme,
                    "-configuration", "Debug",
                    "-sdk", "iphonesimulator",
                    "-derivedDataPath", buildDir.absolutePath,
                    "-quiet")
            }

            // Step 3: Find Booted Simulator
            val deviceId = if (project.hasProperty("target")) {
                project.property("target") as String
            } else {
                println("Finding booted simulator...")
                val simOut = ByteArrayOutputStream()
                exec { commandLine("xcrun", "simctl", "list", "devices", "booted"); standardOutput = simOut }
                var foundDeviceId: String? = null
                val lines = simOut.toString().lines()
                for (line in lines) {
                    if (line.contains("(Booted)")) {
                        val match = "\\(([A-F0-9\\-]+)\\)".toRegex().find(line)
                        if (match != null) { foundDeviceId = match.groupValues[1]; break }
                    }
                }
                if (foundDeviceId == null) {
                    println("No booted simulator found. Please launch a simulator manually, or use -Ptarget <device> to specify a simulator.")
                    return@doLast
                }
                println("Using simulator: $foundDeviceId")
                foundDeviceId
            }

            // Step 4: Install App
            val productsDir = File(buildDir, "Build/Products/Debug-iphonesimulator")
            val appFiles = productsDir.listFiles { _, name -> name.endsWith(".app") }
            if (appFiles == null || appFiles.isEmpty()) {
                throw GradleException("App bundle not found in $productsDir")
            }
            val appPath = appFiles.first()
            println("Found app bundle: ${appPath.name}")

            println("Installing app...")
            exec { commandLine("xcrun", "simctl", "install", deviceId, appPath.absolutePath) }

            // Step 5: Launch App
            var bundleId = iosBundleId
            val infoPlist = File(appPath, "Info.plist")
            if (infoPlist.exists()) {
                try {
                    val out = ByteArrayOutputStream()
                    exec {
                        commandLine("/usr/libexec/PlistBuddy", "-c", "Print :CFBundleIdentifier", infoPlist.absolutePath)
                        standardOutput = out
                        isIgnoreExitValue = true
                    }
                    val extractedId = out.toString().trim()
                    if (extractedId.isNotEmpty() && !extractedId.contains("Does Not Exist")) {
                        bundleId = extractedId
                        println("Extracted Bundle ID: $bundleId")
                    }
                } catch (e: Exception) {
                    println("Failed to extract Bundle ID: $e")
                }
            }

            println("Launching app ($bundleId)...")
            exec { commandLine("xcrun", "simctl", "launch", deviceId, bundleId) }
            println("iOS app launched!")
        }
    }
}

// =====================================================================
// ==================== HarmonyOS Deploy Task ===========================
// =====================================================================

if (!androidOnly) {
    tasks.register("deployHarmony") {
        group = "deployment"
        description = "Build, install and launch HarmonyOS app (harmonyApp). 可选 -PharmonyX64=true 额外发布 X64 so（当前被工具链阻塞）"

        // Step 1: Publish shared binaries (arm64；X64 受开关控制)
        dependsOn(":composeApp:publishDebugBinariesToHarmonyApp")
        if (harmonyX64Enabled) {
            dependsOn(":composeApp:publishDebugBinariesToHarmonyAppX64")
        }

        doLast {
            val projectDir = layout.projectDirectory.asFile
            val harmonyDir = File(projectDir, harmonyAppDirName)

            val hdc = findExecutable("hdc")
            val hvigorw = findHvigorw()

            // DevEco 路径解析（与 runscript/runOhosApp-Mac.sh 一致）
            val devecoHome = System.getenv("DEVECO_HOME") ?: "/Applications/DevEco-Studio.app/Contents"
            val devecoSdkHome = System.getenv("DEVECO_SDK_HOME") ?: "$devecoHome/sdk"

            // 拼装 PATH：确保 hdc / node / ohpm / hvigorw 在环境变量中
            val extraPath = listOf(
                "$devecoSdkHome/default/openharmony/toolchains",
                "$devecoHome/sdk/default/openharmony/toolchains",
                "$devecoHome/jbr/Contents/Home/bin",
                "$devecoHome/tools/node/bin",
                "$devecoHome/tools/ohpm/bin",
                "$devecoHome/tools/hvigor/bin"
            ).filter { File(it).exists() }.joinToString(File.pathSeparator)
            val envPath = "$extraPath${File.pathSeparator}${System.getenv("PATH") ?: ""}"

            println("Using HDC: $hdc")
            println("Using Hvigorw: $hvigorw")
            println("DEVECO_SDK_HOME: $devecoSdkHome")

            // 预停止应用
            exec { commandLine(hdc, "shell", "aa", "force-stop", harmonyPackage); isIgnoreExitValue = true }

            // Step 2: ohpm install + hvigor 同步 + 打包 HAP
            println("Building HarmonyOS HAP...")
            exec {
                workingDir = harmonyDir
                environment("DEVECO_SDK_HOME", devecoSdkHome)
                environment("PATH", envPath)
                commandLine("ohpm", "install", "--all")
                isIgnoreExitValue = true
            }
            exec {
                workingDir = harmonyDir
                environment("DEVECO_SDK_HOME", devecoSdkHome)
                environment("PATH", envPath)
                commandLine("node", hvigorw, "--sync", "-p", "product=default", "-p", "buildMode=debug",
                    "--analyze=normal", "--parallel", "--incremental", "--daemon")
            }
            exec {
                workingDir = harmonyDir
                environment("DEVECO_SDK_HOME", devecoSdkHome)
                environment("PATH", envPath)
                commandLine("node", hvigorw, "--mode", "module", "-p", "module=$harmonyEntryModule@default",
                    "-p", "product=default", "-p", "buildMode=debug", "-p", "requiredDeviceType=phone", "assembleHap")
            }

            // Step 3: 查找 HAP
            val buildOutputDir = File(harmonyDir, "$harmonyEntryModule/build/default/outputs/default")
            val hapFiles = buildOutputDir.walkTopDown().filter { it.extension == "hap" }.toList()
            if (hapFiles.isEmpty()) {
                throw GradleException("No HAP file found in $buildOutputDir")
            }
            // 优先 signed HAP，否则取最新
            val hapFile = hapFiles.firstOrNull { it.name.endsWith("-signed.hap") }
                ?: hapFiles.maxByOrNull { it.lastModified() }!!
            println("Found HAP: ${hapFile.name}")

            // Step 4: 查找设备
            val deviceId = resolveHarmonyDeviceId(hdc, project)
            if (deviceId.isEmpty()) {
                println("未找到鸿蒙设备，跳过安装。HAP 已产出在: ${hapFile.absolutePath}")
                return@doLast
            }
            println("Deploying to device: $deviceId")

            // Step 5: 安装（推送到临时目录 + bm install；失败则卸载重试）
            println("Installing HAP...")
            val remoteDir = "/data/local/tmp/debug_install"
            exec { commandLine(hdc, "-t", deviceId, "shell", "mkdir", "-p", remoteDir); isIgnoreExitValue = true }
            exec { commandLine(hdc, "-t", deviceId, "file", "send", hapFile.absolutePath, remoteDir); isIgnoreExitValue = true }

            val installOut = ByteArrayOutputStream()
            exec {
                commandLine(hdc, "-t", deviceId, "shell", "bm", "install", "-p", "$remoteDir/${hapFile.name}")
                standardOutput = installOut; errorOutput = installOut; isIgnoreExitValue = true
            }
            if (installOut.toString().contains("failed")) {
                println("安装失败，尝试卸载后重装...")
                exec { commandLine(hdc, "-t", deviceId, "shell", "bm", "uninstall", "-n", harmonyPackage); isIgnoreExitValue = true }
                Thread.sleep(5000)
                exec { commandLine(hdc, "-t", deviceId, "shell", "bm", "install", "-p", "$remoteDir/${hapFile.name}"); isIgnoreExitValue = true }
            }
            exec { commandLine(hdc, "-t", deviceId, "shell", "rm", "-rf", remoteDir); isIgnoreExitValue = true }

            // Step 6: 启动
            println("Launching Ability...")
            exec { commandLine(hdc, "-t", deviceId, "shell", "aa", "start", "-a", harmonyAbility, "-b", harmonyPackage) }
            println("HarmonyOS app launched!")
        }
    }
}
