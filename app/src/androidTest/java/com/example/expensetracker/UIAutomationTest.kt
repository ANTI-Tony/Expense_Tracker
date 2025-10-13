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
import com.example.expensetracker.ui.main.MainActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI自动化测试
 * 验证用户界面交互和完整的用户流程
 *
 */
@RunWith(AndroidJUnit4::class)
class UIAutomationTest {

    companion object {
        private const val SHORT_DELAY = 1000L
        private const val MEDIUM_DELAY = 2000L
        private const val SAVE_DELAY = 1500L
    }

    @Test
    fun testCompleteUserJourney() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 等待主界面完全加载
            Thread.sleep(MEDIUM_DELAY)

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
        // 等待UI准备就绪
        Thread.sleep(SHORT_DELAY)

        // 点击添加按钮
        onView(withId(R.id.fabAdd))
            .check(matches(isDisplayed()))
            .perform(click())

        // 等待页面导航完成
        Thread.sleep(SHORT_DELAY)

        // 验证添加页面打开
        onView(withId(R.id.etTitle))
            .check(matches(isDisplayed()))

        // 填写费用标题
        onView(withId(R.id.etTitle))
            .perform(
                typeText("UI Test Coffee"),
                closeSoftKeyboard()
            )

        // 填写金额
        onView(withId(R.id.etAmount))
            .perform(
                typeText("15.50"),
                closeSoftKeyboard()
            )

        // 填写描述
        onView(withId(R.id.etDescription))
            .perform(
                typeText("Automated test expense"),
                closeSoftKeyboard()
            )

        // 等待键盘完全关闭
        Thread.sleep(SHORT_DELAY)

        // 选择分类
        onView(withId(R.id.spinnerCategory))
            .perform(click())

        onData(allOf(instanceOf(String::class.java), `is`("Food")))
            .perform(click())

        // 等待分类选择完成
        Thread.sleep(SHORT_DELAY)

        // 保存费用
        onView(withId(R.id.btnSave))
            .perform(click())

        // 等待保存操作完成
        Thread.sleep(SAVE_DELAY)

        // 验证返回主界面且费用已添加
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))

        onView(withText("UI Test Coffee"))
            .check(matches(isDisplayed()))
    }

    private fun testEditExpenseFlow() {
        // 等待列表刷新
        Thread.sleep(SHORT_DELAY)

        // 点击刚添加的费用
        onView(withText("UI Test Coffee"))
            .perform(click())

        // 等待编辑页面打开
        Thread.sleep(SHORT_DELAY)

        // 验证编辑页面打开
        onView(withId(R.id.etTitle))
            .check(matches(withText("UI Test Coffee")))

        // 修改金额
        onView(withId(R.id.etAmount))
            .perform(
                clearText(),
                typeText("20.00"),
                closeSoftKeyboard()
            )

        // 等待输入完成
        Thread.sleep(SHORT_DELAY)

        // 保存修改
        onView(withId(R.id.btnSave))
            .perform(click())

        // 等待保存完成
        Thread.sleep(SAVE_DELAY)

        // 验证修改成功
        onView(withText("$20.00"))
            .check(matches(isDisplayed()))
    }

    private fun testViewChartsFlow() {
        // 等待数据刷新
        Thread.sleep(SHORT_DELAY)

        // 点击查看图表按钮
        onView(withId(R.id.btnChart))
            .perform(click())

        // 等待图表页面加载
        Thread.sleep(MEDIUM_DELAY)

        // 验证图表页面打开
        onView(withId(R.id.categoryContainer))
            .check(matches(isDisplayed()))

        onView(withId(R.id.dailyContainer))
            .check(matches(isDisplayed()))

        // 返回主页面
        pressBack()

        // 等待返回动画完成
        Thread.sleep(SHORT_DELAY)

        // 验证回到主界面
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
    }

    private fun testDeleteExpenseFlow() {
        // 等待列表加载
        Thread.sleep(SHORT_DELAY)

        // 长按费用项目触发删除
        onView(withText("UI Test Coffee"))
            .perform(longClick())

        // 等待对话框出现
        Thread.sleep(SHORT_DELAY)

        // 在删除确认对话框中点击删除
        onView(withText("Delete"))
            .perform(click())

        // 等待删除操作和Snackbar出现
        Thread.sleep(SHORT_DELAY)

        // 验证Snackbar出现
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(containsString("deleted"))))
    }

    @Test
    fun testInputValidation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 等待Activity加载
            Thread.sleep(MEDIUM_DELAY)

            // 打开添加页面
            onView(withId(R.id.fabAdd))
                .perform(click())

            Thread.sleep(SHORT_DELAY)

            // 测试空标题验证
            onView(withId(R.id.btnSave))
                .perform(click())

            onView(withId(R.id.etTitle))
                .check(matches(hasErrorText("标题不能为空")))

            // 填写标题后测试空金额
            onView(withId(R.id.etTitle))
                .perform(
                    typeText("Test"),
                    closeSoftKeyboard()
                )

            Thread.sleep(SHORT_DELAY)

            onView(withId(R.id.btnSave))
                .perform(click())

            onView(withId(R.id.etAmount))
                .check(matches(hasErrorText("金额不能为空")))

            // 测试无效金额（零）
            onView(withId(R.id.etAmount))
                .perform(
                    typeText("0"),
                    closeSoftKeyboard()
                )

            Thread.sleep(SHORT_DELAY)

            onView(withId(R.id.btnSave))
                .perform(click())

            onView(withId(R.id.etAmount))
                .check(matches(hasErrorText("金额必须大于0")))
        }
    }

    @Test
    fun testNavigationAndLifecycle() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // 等待Activity加载
            Thread.sleep(MEDIUM_DELAY)

            // 测试应用前后台切换
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            Thread.sleep(SHORT_DELAY)

            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            Thread.sleep(SHORT_DELAY)

            // 验证界面仍然正常
            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))

            // 测试配置变化（旋转屏幕模拟）
            scenario.onActivity { activity ->
                activity.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            // 等待配置变化完成
            Thread.sleep(MEDIUM_DELAY)

            // 验证旋转后界面仍然正常
            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))

            // 恢复竖屏方向
            scenario.onActivity { activity ->
                activity.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            Thread.sleep(MEDIUM_DELAY)
        }
    }

    @Test
    fun testInvalidInputHandling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(MEDIUM_DELAY)

            // 打开添加页面
            onView(withId(R.id.fabAdd))
                .perform(click())

            Thread.sleep(SHORT_DELAY)

            // 测试负数金额
            onView(withId(R.id.etTitle))
                .perform(
                    typeText("Negative Test"),
                    closeSoftKeyboard()
                )

            onView(withId(R.id.etAmount))
                .perform(
                    typeText("-10"),
                    closeSoftKeyboard()
                )

            Thread.sleep(SHORT_DELAY)

            onView(withId(R.id.btnSave))
                .perform(click())

            // 验证错误提示（负数会被验证为无效）
            onView(withId(R.id.etAmount))
                .check(matches(hasErrorText("金额必须大于0")))
        }
    }

    @Test
    fun testEmptyStateDisplay() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(MEDIUM_DELAY)

            // 验证主界面正常显示
            onView(withId(R.id.tvTotalAmount))
                .check(matches(isDisplayed()))
        }
    }
}