cmake_minimum_required(VERSION 3.10)
project(MyTutorial)

include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/refs/tags/v1.14.0.zip
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

enable_testing()

add_executable(my_test test_math.cpp math_lib.cpp)

target_link_libraries(my_test GTest::gtest_main)

include(GoogleTest)
gtest_discover_tests(my_test)

add_executable(my_app main.cpp)

