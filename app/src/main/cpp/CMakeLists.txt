cmake_minimum_required(VERSION 3.22.1)
project("cube-solver")

# Базовые настройки
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Настройки для Android
if(ANDROID)
    # Проверка и установка пути к NDK
    if(NOT DEFINED ANDROID_NDK)
        set(ANDROID_NDK $ENV{ANDROID_NDK_HOME})
        if(NOT ANDROID_NDK)
            message(FATAL_ERROR "ANDROID_NDK_HOME not set!")
        endif()
    endif()

    # Системные инклюды
    include_directories(SYSTEM
            ${ANDROID_NDK}/sysroot/usr/include
            ${ANDROID_NDK}/sysroot/usr/include/aarch64-linux-android
    )

    # Флаги для Android
    add_compile_options(
            -fPIC
            -Wall
            -Wno-unused-command-line-argument
            -D_LIBCPP_DISABLE_AVAILABILITY
            -U__STDCPP_THREADS__
    )

    # Явно указываем использовать libc++
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
endif()

# Список исходников
set(SOURCE_FILES
        cube-solver.cpp
        search.cpp
        facecube.cpp
        prunetable_helpers.cpp
        cubiecube.cpp
        coordcube.cpp
)

# Создание библиотеки
add_library(
        cube-solver
        SHARED
        ${SOURCE_FILES}
)
find_library(log-lib log)
# Подключение зависимостей
target_link_libraries(
        cube-solver
        log
        c
        m
        dl
        c++_shared
)

# Общие определения компиляции
target_compile_definitions(cube-solver PRIVATE
        -DLOG_TAG=\"cube-solver\"
        # Убрали проблемные макросы
)

# Настройки компилятора
target_compile_options(cube-solver PRIVATE
        -fms-extensions
        -Wno-error=implicit-function-declaration
)