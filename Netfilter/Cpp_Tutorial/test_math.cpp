#include <gtest/gtest.h>

int add(int a, int b);

TEST(MathTests, TestAddition) {
    EXPECT_EQ(add(1,1), 2);
    EXPECT_EQ(add(-1, -2), -3);
}