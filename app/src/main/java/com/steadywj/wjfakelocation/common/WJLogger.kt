// WJLogger.kt
package com.steadywj.wjfakelocation.common

import com.steadywj.wjfakelocation.BuildConfig
import timber.log.Timber

/**
 * 统一日志管理工具
 *
 * 功能:
 * - 统一使用 Timber 进行日志记录
 * - Debug 版本自动输出日志，Release 版本禁用
 * - 支持自定义标签和日志级别
 *
 * 使用示例:
 * ```
 * WJLogger.d("地图加载完成")
 * WJLogger.e(exception, "位置获取失败")
 * WJLogger.i("用户切换到高德地图")
 * ```
 *
 * @author WJFakeLocation Team
 * @since 2.0.0
 */
object WJLogger {
    /**
     * 初始化日志系统
     * 需要在 Application 的 onCreate 中调用
     */
    fun init() {
        if (BuildConfig.DEBUG) {
            // Debug 模式：输出详细日志到 Logcat
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun createStackElementTag(element: StackTraceElement): String {
                        // 显示类名和方法名
                        return "${element.fileName}:${element.lineNumber}#${element.methodName}"
                    }

                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        // 添加时间戳
                        val timestamp =
                            java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                                .format(java.util.Date())
                        super.log(priority, tag, "[$timestamp] $message", t)
                    }
                },
            )

            Timber.d("WJLogger initialized in DEBUG mode")
        } else {
            // Release 模式：不输出日志（或可以集成崩溃收集）
            // Timber.plant(CrashlyticsTree()) // 可选：集成 Firebase Crashlytics
        }
    }

    /**
     * 输出 VERBOSE 级别日志
     */
    fun v(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.v(throwable, message)
        } else {
            Timber.v(message)
        }
    }

    /**
     * 输出 VERBOSE 级别日志（带自定义 TAG）
     */
    fun v(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.tag(tag).v(throwable, message)
        } else {
            Timber.tag(tag).v(message)
        }
    }

    /**
     * 输出 DEBUG 级别日志（最常用）
     */
    fun d(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.d(throwable, message)
        } else {
            Timber.d(message)
        }
    }

    /**
     * 输出 DEBUG 级别日志（带自定义 TAG）
     */
    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.tag(tag).d(throwable, message)
        } else {
            Timber.tag(tag).d(message)
        }
    }

    /**
     * 输出 INFO 级别日志
     */
    fun i(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.i(throwable, message)
        } else {
            Timber.i(message)
        }
    }

    /**
     * 输出 INFO 级别日志（带自定义 TAG）
     */
    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.tag(tag).i(throwable, message)
        } else {
            Timber.tag(tag).i(message)
        }
    }

    /**
     * 输出 WARN 级别日志
     */
    fun w(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.w(throwable, message)
        } else {
            Timber.w(message)
        }
    }

    /**
     * 输出 WARN 级别日志（带自定义 TAG）
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.tag(tag).w(throwable, message)
        } else {
            Timber.tag(tag).w(message)
        }
    }

    /**
     * 输出 ERROR 级别日志
     */
    fun e(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.e(throwable, message)
        } else {
            Timber.e(message)
        }
    }

    /**
     * 输出 ERROR 级别日志（带自定义 TAG）
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }

    /**
     * 输出 ASSERT 级别日志
     */
    fun wtf(
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Timber.wtf(throwable, message)
        } else {
            Timber.wtf(message)
        }
    }

    /**
     * 向上转型为 Timber.Tree
     * 用于直接调用 Timber API
     */
    val tree: Timber.Tree
        get() = Timber.forest().firstOrNull() ?: Timber.DebugTree()
}
