package com.example.gamemanager

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务 —— 强烈建议开启，它是**唯一事件驱动**的可靠通道。
 *
 * 两个作用：
 * ① 窗口一切换就立刻判定（不用等 5 秒轮询），时间到当场拦；
 * ② 无障碍服务由系统绑定，比前台服务难被 ROM 杀掉，顺手做服务自愈。
 * 另外只有它能调用 performGlobalAction(GLOBAL_ACTION_HOME) 把游戏压回桌面。
 *
 * 行为约束（上一版反复弹锁的根源就在这儿，已改）：
 * - 只处理"窗口切到受管控应用"这一种事件；
 * - 判定统一走 Guard（含前台感知、超限判定、20 秒冷却、暂停）；
 * - 本应用、桌面、系统界面的事件一律忽略；
 * - 绝不在这里锁屏。
 */
class AppMonitorAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile private var instance: AppMonitorAccessibilityService? = null

        /** 供后台服务请求"退回桌面"（只能由无障碍服务执行） */
        fun requestHome() {
            val s = instance ?: return
            s.handler.post {
                try {
                    s.performGlobalAction(GLOBAL_ACTION_HOME)
                } catch (e: Exception) { }
            }
        }

        fun isRunning(): Boolean = instance != null
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
        Guard.init(this)
        // 必须以 XML 里已加载的配置为底：新建空 AccessibilityServiceInfo 会整体覆盖掉
        // canRetrieveWindowContent、settingsActivity 和 flags，部分国产 ROM 上事件会变稀疏。
        val info = try {
            serviceInfo ?: AccessibilityServiceInfo()
        } catch (e: Throwable) {
            AccessibilityServiceInfo()
        }
        // 只认窗口切换。内容变化事件的包名可能来自后台应用 / 悬浮窗，
        // 拿它同步前台会把 UsageUtil.evFg 污染成非前台包；而且它高频触发会不断刷新
        // evFgAt，让「3 秒内用实时值」的快速返回恒命中、queryEvents 永远跑不到，
        // 结果反而是静默不拦。全屏 / 沉浸式下靠 5 秒轮询 + queryUsageStats 交叉校正兜底。
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // 服务被 ROM 清掉时，借这次窗口事件顺手把它拉回来
        if (!MonitorService.alive) {
            try { Watchdog.ensure(this); Watchdog.startService(this) } catch (e: Throwable) { }
        }
        val pkg = event.packageName?.toString() ?: return
        // 系统界面（下拉通知栏等）和本应用本身不算"切到别的应用"，别用它们覆盖真实前台
        if (pkg == "android" || pkg == "com.android.systemui" || pkg == packageName) return
        // 同步给轮询链路共用：避免 queryEvents 在省电模式下返回空时，轮询还拿着旧包名
        UsageUtil.noteForeground(pkg)

        // 判定集中在 Guard：非受管控应用 / 没超限 / 在冷却 / 家长已暂停 → 全部跳过
        if (!Guard.shouldHandle(this, pkg)) return

        // 走到这里才说明真该处置，交给统一执行器（拉拦截页，必要时附带退回桌面）
        Enforcer.enforce(this, pkg)
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
