package com.example.expensetracker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.platform.app.InstrumentationRegistry
//import androidx.test.uiautomator.UiDevice
import com.example.expensetracker.ui.main.MainActivity
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI自动化测试
 * 验证用户界面交互和完整的用户流程
 */
@RunWith(AndroidJUnit4::class)
class UIAutomationTest {

    // Remove the permission rule as it's causing issues
    // @get:Rule
    // val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
    //     android.Manifest.permission.POST_NOTIFICATIONS
    // )

    @Test
    fun testCompleteUserJourney() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Add a small delay to ensure the activity is fully loaded
            Thread.sleep(2000)

            // 1. 验证主界面加载
            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("Total:"))))

            // 2. 测试添加费用功能
            testAddExpenseFlow()

            // 3. 测试编辑费用功能
            testEditExpenseFlow()

            // 4. 测试查看图表功能
            testViewChartsFlow()

            // 5. 测试删除费用功能
            testDeleteExpenseFlow()
        }
    }

    private fun testAddExpenseFlow() {
        // Add delay to ensure UI is ready
        Thread.sleep(1000)

        // 点击添加按钮
        onView(withId(R.id.fabAdd))
            .check(matches(isDisplayed()))
            .perform(click())

        // Add delay for navigation
        Thread.sleep(1000)

        // 验证添加页面打开
        onView(withId(R.id.etTitle))
            .check(matches(isDisplayed()))

        // 填写费用信息
        onView(withId(R.id.etTitle))
            .perform(typeText("UI Test Coffee"))

        onView(withId(R.id.etAmount))
            .perform(typeText("15.50"))

        onView(withId(R.id.etDescription))
            .perform(typeText("Automated test expense"))

        // 关闭键盘
        closeSoftKeyboard()

        // 选择分类
        onView(withId(R.id.spinnerCategory))
            .perform(click())
        onData(allOf(instanceOf(String::class.java), `is`("Food")))
            .perform(click())

        // 保存费用
        onView(withId(R.id.btnSave))
            .perform(click())

        // Add delay for save operation
        Thread.sleep(1000)

        // 验证返回主界面且费用已添加
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))

        onView(withText("UI Test Coffee"))
            .check(matches(isDisplayed()))
    }

    private fun testEditExpenseFlow() {
        // 点击刚添加的费用
        onView(withText("UI Test Coffee"))
            .perform(click())

        // 验证编辑页面打开
        onView(withId(R.id.etTitle))
            .check(matches(withText("UI Test Coffee")))

        // 修改金额
        onView(withId(R.id.etAmount))
            .perform(clearText(), typeText("20.00"))

        closeSoftKeyboard()

        // 保存修改
        onView(withId(R.id.btnSave))
            .perform(click())

        // 验证修改成功
        onView(withText("$20.00"))
            .check(matches(isDisplayed()))
    }

    private fun testViewChartsFlow() {
        // 点击查看图表按钮
        onView(withId(R.id.btnChart))
            .perform(click())

        // 验证图表页面打开
        onView(withId(R.id.categoryContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.dailyContainer))
            .check(matches(isDisplayed()))

        // 返回主页面
        pressBack()

        // 验证回到主界面
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    private fun testDeleteExpenseFlow() {
        // 长按费用项目
        onView(withText("UI Test Coffee"))
            .perform(longClick())

        // 在删除确认对话框中点击删除
        onView(withText("Delete"))
            .perform(click())

        // 验证费用已被删除
        Thread.sleep(1000)

        // 验证Snack bar出现
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(containsString("deleted"))))
    }

    @Test
    fun testInputValidation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Add delay to ensure activity is loaded
            Thread.sleep(2000)

            // 打开添加页面
            onView(withId(R.id.fabAdd)).perform(click())

            Thread.sleep(1000)

            // 测试空标题验证
            onView(withId(R.id.btnSave)).perform(click())
            onView(withId(R.id.etTitle))
                .check(matches(hasErrorText("Title is required")))

            // 填写标题后测试空金额
            onView(withId(R.id.etTitle))
                .perform(typeText("Test"))
            closeSoftKeyboard()

            onView(withId(R.id.btnSave)).perform(click())
            onView(withId(R.id.etAmount))
                .check(matches(hasErrorText("Amount is required")))

            // 测试无效金额
            onView(withId(R.id.etAmount))
                .perform(typeText("0"))
            closeSoftKeyboard()

            onView(withId(R.id.btnSave)).perform(click())
            onView(withId(R.id.etAmount))
                .check(matches(hasErrorText("Amount must be greater than 0")))
        }
    }

    @Test
    fun testNavigationAndLifecycle() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Add delay to ensure activity is loaded
            Thread.sleep(2000)

            // 测试应用前后台切换
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

            // 验证界面仍然正常
            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))

            // 测试配置变化（旋转屏幕模拟）
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            Thread.sleep(2000) // 等待配置变化完成

            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))
        }
    }
}