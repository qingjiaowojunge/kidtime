package com.example.gamemanager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * 看门狗：定时确认监控服务还活着，被系统清掉就拉回来。
 *
 * 为什么需要：国产 ROM（小米 / 华为 / OPPO / vivo）会在内存紧张或"清理加速"时
 * 直接杀掉前台服务，而且不会主动告知。以前一旦被杀就再也没人管，
 * 表现就是"时间到了照样能玩"。加上电池优化白名单后概率降低，但仍不能保证。
 *
 * 只用非精确闹钟，不需要 SCHEDULE_EXACT_ALARM 这类敏感权限，也不需要联网。
 */
object Watchdog {
    private const val ACTION = "com.junge.kidtime.WATCHDOG"
    private const val REQ = 71
    private const val INTERVAL = 5 * 60_000L

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, WatchdogReceiver::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, REQ, i, flags)
    }

    /** 续上下一次检查。每次触发后都会再排一次，链条不会断。 */
    fun ensure(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            am.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL,
                pending(ctx)
            )
        } catch (e: Throwable) { }
    }

    /** Android 12+ 从后台起前台服务可能抛异常，必须兜住再降级 */
    fun startService(ctx: Context) {
        val i = Intent(ctx, MonitorService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        } catch (e: Throwable) {
            try { ctx.startService(i) } catch (e2: Throwable) { }
        }
    }
}

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        try {
            Watchdog.ensure(ctx)              // 先续上下一次，别让链条断掉
            Guard.init(ctx)
            if (!Guard.hasRules()) return     // 没配置就别起服务，省电
            if (!MonitorService.alive) Watchdog.startService(ctx)
        } catch (e: Throwable) { }
    }
}

/**
 * 开机 / 覆盖安装后自动恢复监控。
 * 不这么做，孩子重启一次手机，只要不主动打开这个 App，就永远没人管。
 * BOOT_COMPLETED 是系统允许启动前台服务的少数豁免场景之一。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val a = intent?.action ?: return
        val hit = a == Intent.ACTION_BOOT_COMPLETED ||
            a == Intent.ACTION_MY_PACKAGE_REPLACED ||
            a == "android.intent.action.QUICKBOOT_POWERON" ||
            a == "com.htc.intent.action.QUICKBOOT_POWERON"
        if (!hit) return
        try {
            Guard.init(ctx)                   // 规则存在 SharedPreferences，无需打开界面
            if (!Guard.hasRules()) return
            Watchdog.ensure(ctx)
            Watchdog.startService(ctx)
        } catch (e: Throwable) { }
    }
}
