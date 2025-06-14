plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.artem52"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.artem52"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")                              //file("CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    ndkVersion = "25.2.9519653"
    buildToolsVersion = "35.0.1"
}

dependencies {
    implementation ("androidx.appcompat:appcompat:1.6.1")
    //implementation ("com.google.android.material:material:1.11.0")
    //implementation ("androidx.opengl:opengl:1.1.0-alpha05")
    //implementation ("androidx.opengl:opengl-api:1.0.0")
    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("com.google.android.material:material:1.6.0")
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("androidx.databinding:viewbinding:7.4.2")
    //implementation ("androidx.appcompat:appcompat:1.4.1")
   // implementation ("com.google.android.material:material:1.5.0")
    //implementation ("androidx.opengl:opengl:1.2.0")
    //implementation ("androidx.opengl:opengl-api:1.0.0")
    /*
        val gdxVersion = "1.12.1"
        implementation ("com.badlogicgames.gdx:gdx:$gdxVersion")
        implementation ("com.badlogicgames.gdx:gdx-box2d:$gdxVersion") // если используете Box2D
        implementation ("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop") // для десктопной платформы
        implementation ("com.badlogicgames.gdx:gdx-g3d:$gdxVersion") // для работы с 3D моделями
        implementation ("com.badlogicgames.gdx:gdx-g3d-platform:$gdxVersion:natives-desktop") // для десктопной платформы



        implementation("com.badlogicgames.gdx:gdx:1.12.1")
        implementation("com.badlogicgames.gdx:gdx-backend-android:1.12.1")

        // 3D-рендеринг (для ваших импортов)
        implementation("com.badlogicgames.gdx:gdx-box2d:1.12.1")
        implementation("com.badlogicgames.gdx:gdx-ai:1.8.2")

        // Нативные библиотеки (обязательно!)
        implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
        implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
        implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86")
        implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")

        implementation(libs.gdx)
        implementation(libs.gdx.backend.android)
        implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-armeabi-v7a")
        implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-arm64-v8a")
        implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86")
        implementation("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-x86_64")
    */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}