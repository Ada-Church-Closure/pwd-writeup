add_test([=[MathTests.TestAddition]=]  /home/ada/pwd-writeup/Netfilter/Cpp_Tutorial/build/my_test [==[--gtest_filter=MathTests.TestAddition]==] --gtest_also_run_disabled_tests)
set_tests_properties([=[MathTests.TestAddition]=]  PROPERTIES DEF_SOURCE_LINE /home/ada/pwd-writeup/Netfilter/Cpp_Tutorial/test_math.cpp:5 WORKING_DIRECTORY /home/ada/pwd-writeup/Netfilter/Cpp_Tutorial/build SKIP_REGULAR_EXPRESSION [==[\[  SKIPPED \]]==])
set(  my_test_TESTS MathTests.TestAddition)
