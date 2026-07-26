plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    id("jacoco") // 代码覆盖率
    kotlin("kapt") // Kotlin 注解处理
    
    // 应用代码质量插件
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

// Room 数据库配置
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.steadywj.wjfakelocation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.steadywj.wjfakelocation"
        minSdk = 33
        targetSdk = 35
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // CI/CD 签名配置
            signingConfig = if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfigs.create("release") {
                    storeFile = file(System.getenv("KEYSTORE_PATH"))
                    storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") 
                        ?: project.findProperty("RELEASE_KEYSTORE_PASSWORD") as String? ?: ""
                    keyAlias = System.getenv("RELEASE_KEY_ALIAS") 
                        ?: project.findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
                    keyPassword = System.getenv("RELEASE_KEY_PASSWORD") 
                        ?: project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
                }
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Kapt 兼容性：使用 Kotlin 1.9 语言版本
        languageVersion = "1.9"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)
    
    // AMap (高德地图) - 本地 JAR (v2026.03.06)
    implementation(files("libs/AMap3DMap_11.1.000_AMapSearch_9.7.4_AMapLocation_11.1.000_20260306.jar"))
    
    // Baidu Map (百度地图) - 本地 JAR (v7.5.4)
    implementation(files("libs/BaiduLBS_Android.jar"))
    
    // Supabase (云同步后端)
    implementation(platform("io.github.jan-tennert.supabase:bom:2.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    
    // Lua 脚本引擎
    implementation("org.luaj:luaj-jse:3.0.1")
    
    // Xposed
    compileOnly(libs.xposed.api)
    
    // Utilities
    implementation(libs.hiddenapibypass)
    implementation(libs.security.crypto)
    
    // Timber - 统一日志管理
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Test
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ==================== Jacoco 代码覆盖率配置 ====================
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// 创建 Jacoco 报告任务
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "Reporting"
    description = "Generate Jacoco coverage reports"
    
    dependsOn(tasks.named("testDebugUnitTest"))
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*Hilt*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*_Impl*.*",
        "**/*Module.*"
    )
    
    val kotlinClassesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get()
    val javaClassesDir = layout.buildDirectory.dir("intermediates/javac/debug").get()
    
    classDirectories.setFrom(
        fileTree(mapOf("dir" to kotlinClassesDir, "excludes" to fileFilter)),
        fileTree(mapOf("dir" to javaClassesDir, "excludes" to fileFilter))
    )
    
    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))
    
    executionData.setFrom(
        fileTree(mapOf(
            "dir" to layout.buildDirectory,
            "includes" to listOf("outputs/unit_test_code_coverage/**/*.exec", "jacoco/test.exec")
        ))
    )
}

// ==================== Ktlint 代码风格检查配置 ====================
ktlint {
    version.set("1.0.1")
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        exclude { element -> element.file.path.contains("build/") }
    }
}

// ==================== Detekt 静态代码分析配置 ====================
detekt {
    toolVersion = "1.23.4"
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = false
    source.setFrom(
        files("$projectDir/src/main/java",
              "$projectDir/src/main/kotlin",
              "$projectDir/src/test/java",
              "$projectDir/src/test/kotlin")
    )
    baseline = file("$projectDir/detekt-baseline.xml")
}

// 创建综合质量检查任务
tasks.register("codeQualityCheck") {
    group = "verification"
    description = "Run all code quality checks (ktlint + detekt)"
    dependsOn("ktlintCheck", "detekt")
}