package com.example.gamemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.os.Build
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * 统一执行器：把"决定要动手"变成"实际的处置动作"。
 *
 * 所有处置都从这里走，保证后台轮询与无障碍事件两条链路行为一致。
 * 处置顺序：全屏拦截页 →（可选）退回桌面 →（可选）锁屏。
 * 默认策略只做第一步，后两步必须家长显式开启。
 */
object Enforcer {

    /** 拦截页是否正在显示，避免重复拉起 */
    @Volatile var blockShowing: Boolean = false

    /** 自检用：最后一次"动手"到底做成了什么 */
    @Volatile var lastEnforceText: String = "还没执行过拦截"
    @Volatile var lastEnforceOk: Boolean = false

    private fun nowText(): String = try {
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    } catch (e: Exception) { "" }

    /**
     * 由真正执行挂窗口的地方回报结果。
     * OverlayBlocker.show() 是 main.post 异步的，若在这里先写"成功"，
     * addView 失败时自检就会误报成功，家长查不出为什么没拦住。
     */
    fun recordEnforce(text: String, ok: Boolean) {
        lastEnforceOk = ok
        lastEnforceText = (if (ok) "" else "失败：") + text + " · " + nowText()
    }

    private fun dpm(ctx: Context) =
        ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    private fun admin(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    /**
     * 对 pkg 执行处置。
     * 调用前必须已通过 Guard.shouldHandle（它会占用冷却票据），这里不再做判定。
     */
    fun enforce(ctx: Context, pkg: String) {
        showBlock(ctx, pkg)

        if (Guard.policy == Guard.POLICY_BLOCK_HOME || Guard.policy == Guard.POLICY_LOCK) {
            AppMonitorAccessibilityService.requestHome()
        }
        if (Guard.policy == Guard.POLICY_LOCK) {
            val d = dpm(ctx)
            if (d != null && d.isAdminActive(admin(ctx))) {
                try { d.lockNow() } catch (e: Exception) { }
            }
        }
    }

    fun showBlock(ctx: Context, pkg: String) {
        // 通道一（最稳）：有"显示在其他应用上层"权限 → 直接盖悬浮窗。
        // 成败由 OverlayBlocker 真正挂上窗口之后回报，这里不预先宣称成功。
        if (OverlayBlocker.canOverlay(ctx)) {
            OverlayBlocker.show(ctx, pkg)
            return
        }
        // 没悬浮窗权限时判定了也盖不住，必须如实写进自检
        lastEnforceOk = false
        lastEnforceText = "缺悬浮窗权限，降级尝试 · " + pkg
        // 通道二：Activity（无障碍在前台时通常可拉起，纯后台服务可能被系统拦）
        // 不再用全局 blockShowing 做守卫：它可能残留 true（悬浮窗权限被撤销 / ROM 收回
        // 窗口 token），会让降级通道永久不拦。CLEAR_TOP|SINGLE_TOP 已经能防重复拉起。
        val i = Intent(ctx, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockActivity.EXTRA_PKG, pkg)
        }
        try {
            ctx.startActivity(i)
            blockShowing = true
            recordEnforce("拦截页 Activity · " + pkg, true)
        } catch (e: Exception) {
            // 通道三：降级成通知，不崩、不锁屏；并释放冷却票据，下一轮立刻重试
            notifyBlocked(ctx, pkg)
            recordEnforce("拦截页起不来，已发通知 · " + pkg, false)
            try { Guard.clearStrike(pkg) } catch (e2: Throwable) { }
        }
    }

    private fun notifyBlocked(ctx: Context, pkg: String) {
        try {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            // 服务可能还没建过这个 channel，Android O+ 会静默丢掉降级通知
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    nm.createNotificationChannel(
                        NotificationChannel("game_monitor", "游戏计时", NotificationManager.IMPORTANCE_LOW)
                    )
                } catch (e: Throwable) { }
            }
            val pi = PendingIntent.getActivity(
                ctx, 2,
                Intent(ctx, BlockActivity::class.java)
                    .putExtra(BlockActivity.EXTRA_PKG, pkg)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE
            )
            nm.notify(
                2,
                NotificationCompat.Builder(ctx, "game_monitor")
                    .setContentTitle("今日时间已用完")
                    .setContentText("点此查看并让家长加时")
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (e: Exception) { }
    }
}
