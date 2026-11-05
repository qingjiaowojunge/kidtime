# 游戏管理软件 · 真实数据版（v3）

用 **HTML / JS / CSS** 写应用，打包成 **Android APK** 装到手机上运行。
应用本体就是 `index.html`（内联了全部 CSS/JS），安卓端只有一个极薄的 WebView 壳把它装进 APK 在手机里跑。

## v3.6 体验升级：更好用、更像成品

用户说「先不要升，再给我升级升级」。这一版先把改动全部压在本地、不发版，等功能与独立复核都过关后才升到 `versionCode 11` / `versionName 3.6` 出包。**仍然只改 `index.html`**，Kotlin / Manifest / 权限清单零改动，管控链路与 v3.5 (10) 完全一致，**可直接覆盖安装**。

| # | 升级 | 说明 |
|---|---|---|
| 1 | **「我的」页两级设置** | 原来 10 张卡一屏平铺，现在默认只留 5 张常用卡，排障 / 记录 / 备份收进「展开全部设置」，状态会记住；顶部快捷条直接给「暂停 30 分钟」「一键自检」「展开全部设置」 |
| 2 | **家长主动加时** | 概览「临时通融」：选应用 + 5/10/15/30 分 → 二次确认 → 今天立刻生效。补上「家长不必等孩子申请」这个真实缺口 |
| 3 | **待审申请角标** | 底部「概览」出红色数字，孩子发了申请，家长切到别的页也不会漏 |
| 4 | **孩子端日期** | 只显示「X 月 X 日 · 星期X」，不含任何额度 / 设置信息 |

**边界守得更紧**：折叠态常显的 5 张卡里特意包含「系统能力授权」和「暂停 / 重置」——后者有「彻底解除管控」，是卸载前置条件，绝不能被折叠藏起来。「临时通融」卡移到申请卡下面，收起四步引导后首屏就够得着。

**修掉一个真 bug**：新加的变量原本叫 `bonusMin`，把工程里已有的 `function bonusMin(pkg)`（`effLimit()` 依赖它算加时）覆盖成数字，会抛 `TypeError: bonusMin is not a function`、额度直接算不出来。`node --check` 查不出这类问题，是无头浏览器实跑抓到的，已改名 `quickBonusMin`。

**独立只读复核**：无 P0、无 P1，逐条确认孩子端最小信息、加时不重复 / 不清当天额度 / 不绕限额、卸载路径始终可达、`askConfirm` fail-safe、无命名冲突、配置导出不会带上新增本地状态。复核列出的 6 条 P2 修了 4 条（常用卡改 id 白名单、配置落盘统一刷新下拉、角标不重复更新、切回孩子端刷新日期），另 2 条为设计行为 / 沿用老逻辑，保留。

验证：`node --check` 通过；div 227/227、CSS 353/353、DOM 146 ids / 128 refs / 0 缺失、38 个桥接调用 / 49 个 Kotlin 方法 / 0 缺失、原生对话框残留 0；无头浏览器 7 个场景错误横幅全空，加时确认链路和清空配置后的下拉刷新都是实点验证过的。

## v3.5 加功能：申请审批 / 分组 / 周报 / 备份 / 模板

用户反馈「再给我更新更新，多添加点功能」。这一版补了 5 项**真实可用**的功能，全部只在 `index.html` 里做；Kotlin 侧 `Guard / Enforcer / MonitorService / OverlayBlocker / Watchdog / GameBridge` 一行未动。新功能只读已有真实数据、只调已有桥接方法，**管控链路零改动，可直接覆盖安装**。

| # | 功能 | 怎么用 | 数据从哪来 |
|---|---|---|---|
| 1 | **孩子申请加时 → 家长审批** | 孩子端时间快用完时点「找家长申请加时」，选 5/10/15/30 分并可写一句话；家长端「概览」出现橙色待审卡，点「同意并加时」立刻生效，点「这次不行」拒绝，孩子端会看到结果 | 申请存在本机 `localStorage`，批准时调已有的 `GameBridge.addBonus`（原生会写审计日志）；**不需要后端、不需要第二台手机** |
| 2 | **应用分组 + 一键勾选整组** | 「应用」页按游戏 / 短视频 / 社交 / 其他筛选，填好分钟数点「一键勾选」，整组未管控的应用一次性纳入 | 真实已装应用清单，按包名/名称关键字自动归类；分错了不影响管控，可手动改 |
| 3 | **本周报告** | 「统计」页顶部看近 7 天合计、日均、没超额度天数、7 天柱状图（超额那天转红）、最费时间的应用；可「复制报告文字」直接发家人 | `UsageStatsManager` 的 7 天真实时长，按受管应用汇总；只统计勾选过的 |
| 4 | **配置备份 / 恢复 / 批量重设** | 「我的」页「复制当前配置」拿到一段文字，换手机/重装后粘贴到框里点「粘贴后恢复」；也可把全部受管应用统一重设为 N 分/天 | 本机配置 JSON；恢复前会二次确认 |
| 5 | **额度模板** | 「我的 → 额度与时段」一键套用严格 20/40、常规 30/60、宽松 45/90（工作日/周末），自动打开分开设 | 改写各应用限额后下发原生；不动今天的加时 |

**顺带修掉一个老毛病**：工程里 10 多处 `window.confirm()` / `alert()` 在 WebView 默认 `WebChromeClient` 下并不可靠（表现就是「点一下没反应」或提示不出现）。现在全部换成页面内确认层 `#confirmModal`，「一键锁机 / 清空配置 / 彻底解除管控 / 今日豁免 / 密码保存」这些动作都能真的弹出来、点得动。同步的 `confirm()` 没法用全局垫片模拟，只能逐处改写成异步回调，已逐处核对「取消＝什么都不做、确定＝才执行」。

**约束守住**：孩子端主页面仍然只回答「我还能玩多久」，新增的申请条只在时间快用完 / 有申请 / 有结果时出现，不显示总额度、不给设置入口。图标全为内联 SVG，无 emoji、无紫粉渐变，颜色走 Token。

验证：CSS 花括号 337/337、`node --check` 通过、127 个 DOM id 与 115 处引用零缺失（防白屏）、`confirm/alert/prompt` 残留 0；无头 Edge 实测三个页面运行时错误横幅全空，待审卡、周报柱状图、密码弹窗、切页交互均正常渲染。`Guard.applyConfig` 已核实不会清掉当天加时（只重写规则并清冷却票据），因此批量重设 / 模板 / 恢复配置不会抹掉今天已给的时间。

**门禁复审（v3.5 (10)）**：独立审查 4 项全部通过——新增桥接方法均真实存在、`applyConfig` 不清加时也不重置专注/暂停、`confirm → askConfirm` 逐处语义正确、孩子端没有泄露家长向信息。顺手修掉它指出的 3 个残留：① 确认弹层万一缺失时，`askConfirm` 改为**什么都不做**（原来会跳过确认直接执行「彻底解除」这类敏感动作）；② 导入配置缺 `password` 时自动补默认值，家长密码能正确恢复；③ 「彻底解除管控」不再调 `pushManaged()`——它内部会 `startMonitorInternal`，会把刚停掉的前台服务重新拉起来。第三点是老代码里就有的语义错误，正好落在「解除后才能卸载」这条链上。

## v3.4 暖纸感视觉增强（只改样式，不动功能）

用户反馈「再去多美化一下」。这一版**只在 `index.html` 的 `<style>` 末尾追加一层 CSS 覆盖**，没有增删任何 DOM 节点、没有改一行 JS 绑定；管控逻辑、自检面板、四步引导、密码弹窗全部保持 v3.3 (7) 原样，**可直接覆盖安装**。

| 位置 | 处理 |
|---|---|
| 整体纸面 | `#app` 加极淡纸张纹理 + 两处径向柔光 + 细描边与投影；顶栏改深墨绿斜向受光，压一条暖金细线 |
| 卡片 | 细描边、顶部高光、左侧章节竖线、暖纸渐变；按钮 / 输入框 / 胶囊 / 开关补材质、按压与 focus 反馈 |
| 孩子端 | 标题左对齐；剩余分钟升到 56px 等宽数字，是页面唯一主角；进度条加末端圆点；「用完了」单独走克制赭红态并缩到 34px |
| 空状态 | 实心肉色圆改空心双环钟面；没有「正在使用」时 `.play:empty{display:none}`，不留只剩圆点的空胶囊 |
| 装饰 | 去掉右上角过重的同心圆（会被看成卡片缺了一角），改成右下角 22px 淡裁切角标 |
| 动效 | 仅 `.page.active` 加 `fadeUp`，并有 `prefers-reduced-motion` 降级；**孩子端卡片一律不加动画**，因为 `renderKid()` 会被轮询重绘，动画会抖 |

自查方式：本机无头 Edge 截图三轮，修掉绿雾过重、空胶囊、同心圆缺角错觉、超限字号四处；`node --check` 通过、CSS 花括号 283/283 平衡、四步引导 1～4 齐全。

> 这是**纯视觉更新，不解决任何管控问题**。真机若仍遇到「时间到了还能玩」，请按 `自测清单.md` 第 25～31 步和「我的 → 自检 → 复制检测结果」排查。

## v3.3 稳定化：把「时间到了照样能玩」彻底堵住

真机反馈：**时间已经到了，游戏还能打开、还能继续玩**。这不是界面或数据展示问题，是管控执行链某一环静默失效——之前每一处失败都是 `return`，界面上什么也看不出来。

排查后按版本分三步收口：

| 版本 | 修的是什么 |
|---|---|
| v3.1 | 把「为什么没拦住」变成可读：`MonitorService` 每轮记 `lastResult / lastFg / lastTickAt`，新增 `Guard.blockReason()` / `explainJson()` / `GameBridge.diag()` / `restartMonitor()`；「我的」页加「自检：为什么没拦住」，可一键复制 |
| v3.2 | 修 `OverlayBlocker.show()` 的 stale root：`root != null` 不等于窗口还在屏幕上。系统摘掉窗口 token 后旧代码只更新文案、不再 `addView`，表现就是「拦过一次之后再也不拦」。现在检查 `isAttachedToWindow`，掉了就 `removeViewImmediate` 再重挂 |
| **v3.3** | 把服务存活、开机恢复、事件驱动、失败重试、权限引导一次补齐 |

### v3.3 具体改了什么

| 失效点 | 修法 |
|---|---|
| 国产 ROM 杀掉前台服务后**再也没人拉回来** | 新增 `Watchdog.kt`：`setAndAllowWhileIdle()` 每 5 分钟确认 `MonitorService.alive`，死了就 `startForegroundService()` 拉回；每次触发先续下一次，链条不断 |
| **重启手机后不打开 App 就彻底失管** | 新增 `RECEIVE_BOOT_COMPLETED` + `BootReceiver`，监听开机 / 包替换 / 快速开机，直接读 SharedPreferences 里的规则把服务拉起来，不需要打开界面 |
| 只能靠 10 秒轮询，慢且容易被挂起 | 轮询压到 **5 秒**；无障碍服务改成**事件驱动**，窗口一切换当场判定；无障碍事件里若发现服务已死，顺手自愈 |
| 一次拦截失败造成 **90 秒不设防** | 冷却票据从 90 秒压到 **20 秒**；拦截页 / Activity 起不来时 `Guard.clearStrike(pkg)` 立刻释放，下一轮马上重试 |
| 拦截到底成没成功，界面上看不到 | `Enforcer.lastEnforceText / lastEnforceOk` 如实记录，`diag()` 输出 `lastEnforce / enforceOk`；自检结论会直接报「条件都齐，但最近一次拦截没成功」 |
| 前台识别拿到的是**几十分钟前的旧包名** | `queryEvents` 在国产 ROM 省电 / Doze 下可能一条事件都不给，轮询会一直用旧值，判定「不在受管应用」→ 到点不拦，自检还会显示一个看似正常的旧包名。现在：无障碍事件实时同步进 `UsageUtil.noteForeground()`，3 秒内以它为准；事件为空且已知值超过 1 分钟没刷新时，用 `queryUsageStats` 的 `lastTimeUsed` 交叉校正一次 |
| 拦截页还没挂上就先报「成功」 | `OverlayBlocker.show()` 是 `main.post` 异步的，原先 `Enforcer` 在调用后就写 `lastEnforceOk = true`，`addView` 失败时自检反而显示成功。现在改成由真正挂上窗口的地方 `Enforcer.recordEnforce()` 回报；挂失败还会 `clearStrike()` 释放冷却票据，下一轮立即重试 |
| 家长不知道该开哪些权限 | 引导卡、手册、自检三处统一成「四项清单」，明确标注哪些必须、哪些强烈建议 |
| **拦过一次之后 20 秒随便玩**（最可能是真机根因） | 原实现 `shouldHandle` 一通过就占 20 秒冷却票据，但孩子点「回到桌面」收掉拦截层时票据没释放，重开游戏被直接放行。现在冷却**只在拦截层真的还在显示时才生效**（`Enforcer.blockShowing`），并在 `OverlayBlocker.hide()` / `BlockActivity.onDestroy()` 主动 `clearStrike()` 双保险，层一消失立刻恢复布防 |
| 自检报「该拦」却没动手（假绿灯） | `blockReason()` 原先只对齐 `isBlockedNow`，漏了暂停/忽略包/**冷却中**三道前置，冷却放行时照样返回「应拦」。现在前置顺序与 `shouldHandle` 完全一致，冷却中如实显示「拦截页还在显示，N 秒后复查」 |
| 悬浮窗「挂着却看不见」 | 原 stale-root 修复只在 `isAttachedToWindow == false` 时重建；窗口被 ROM 压到全屏游戏下面但仍是 attached 时只更新文案、不重新置顶。现在每次都先摘再挂，强制重新置顶 |
| 无障碍事件变稀疏 | `onServiceConnected` 原来 new 一个空 `AccessibilityServiceInfo` 整体覆盖 XML，丢掉 `canRetrieveWindowContent`/`settingsActivity`/flags。现在基于已加载配置增量修改。**刻意不注册 `TYPE_WINDOW_CONTENT_CHANGED`**：它的包名可能来自后台应用/悬浮窗，会污染前台识别，且高频刷新会让「3 秒内用实时值」的快速返回恒命中、`queryEvents` 永远跑不到，反而静默不拦。沉浸式下靠 5 秒轮询 + `queryUsageStats` 交叉校正兜底 |
| 并发下票据清不掉 / 串包 | `strikes` 改为 `ConcurrentHashMap`，`currentPkg` 加 `@Volatile`；降级通道不再用残留的 `blockShowing` 做守卫；`startForeground` 失败和看门狗拉起被拒都会写进 `lastResult`，不再静默 |

### 这次新增的权限（回答「有没有更好的权限可以加」）

- **已加** `RECEIVE_BOOT_COMPLETED`：普通权限，装完自动有，不需要家长点。用于开机/升级后自动恢复监控。
- **已加** `SCHEDULE_EXACT_ALARM`（复核后补）：普通权限，装完自动授予、家长不用点。它是 Android 12+ 少数允许**从后台启动前台服务**的豁免场景——审查发现纯 `setAndAllowWhileIdle` 在 Doze 下可能十几分钟才触发，且从后台起 FGS 会被系统直接拒绝，服务被杀就拉不回来。现在优先用精确闹钟，拿不到该权限（用户在「闹钟和提醒」里关了）自动退回非精确，链条不断。
- **已有、但必须家长手动开**：`PACKAGE_USAGE_STATS`（使用情况访问）、`SYSTEM_ALERT_WINDOW`（显示在其他应用上层）。这两项不给，判定和拦截都无从谈起。
- **已有、且强烈建议开**：无障碍服务（`BIND_ACCESSIBILITY_SERVICE`）、电池优化白名单（`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`）。前者改成事件驱动当场拦，后者降低被 ROM 杀后台的概率。
- **不启用设备管理员**：它只能 `lockNow()` 锁屏，不能真正禁用别的 App，而且会造成「删不掉」，之前已经踩过这个死锁。
- **拿不到、也不假装能拿到**：真正让游戏图标点不开需要「设备所有者 / Device Owner」，只有公司统一配发或刷机的设备能拿到，普通手机上的第三方 App 不可能有。本应用做的是**到点盖一张退不出去的全屏拦截页**，孩子只能找家长要时间。

### 自查补记（v3.3 (7)）：交付后又查出的 6 处

| 问题 | 影响 | 处理 |
|---|---|---|
| `MonitorService` 未重写 `onStartCommand` | targetSdk 34 下基类默认 `START_NOT_STICKY`，系统杀掉服务后**不会自动重建**，只能靠看门狗 / 无障碍拉回 | 显式返回 `START_STICKY` |
| 拦截层窗口被 ROM 摘掉后，静态 `blockShowing` 仍为 true | 冷却条件误判「拦截页仍在显示」，仍留 20 秒空窗 | `isShowing()` 改为 `root?.isAttachedToWindow == true`；`tick()` 每轮校验，掉了就复位标志并清空票据 |
| `Enforcer` 降级 Activity 成功时 `lastEnforceOk` 仍为 false | 自检字段语义错误（当前被「缺悬浮窗权限」分支遮住，暂未表现为误报） | 统一走 `recordEnforce(..., true/false)` |
| `UsageUtil` 的 `queryUsageStats` 兜底无节流 | 冷启动且 `queryEvents` 为空时，每 5 秒扫一次全天 usage 数据 | 加 60 秒探测节流 |
| README 下载地址章节仍写「当前版本 v3.0 (1)」 | 家长按错的版本号核对 | 改为 v3.3 (7)，补当前版本核验表 |
| 界面写「设备管理员（可选）」，手册写「不要开」 | 可能诱导家长启用，重蹈「删不掉」的死锁 | 界面改为「不建议启用」，按钮同步标注 |

### 装完 v3.3 家长要做的四件事

1. 使用情况访问 —— **必须**
2. 显示在其他应用上层 —— **必须**
3. 无障碍服务 —— **强烈建议**
4. 电池优化白名单 —— **强烈建议**（手机「设置 → 应用 → 时间约定 → 电池」设成「允许后台运行 / 不受限制」）

设完到「我的 → 自检：为什么没拦住」点「重新检测」，看到**绿色结论**才算齐。若仍拦不住，点「复制检测结果」把整段文字发回来，按里面的 `lastResult` / `lastEnforce` 直接定位，不用再猜权限。

## v3.0 大改：界面重做 + 默认孩子端

前两版解决的是"能不能用"，这一版解决"敢不敢给自己孩子用、家长看不看得懂"。

| 改动 | 之前 | 现在 |
|---|---|---|
| **整体配色** | 冷灰蓝底 + 系统标准蓝（`#2563EB`），像后台管理系统的网页 | 暖米白底（`#f6f3ec`）+ 墨绿主色（`#2f6b52`）+ 赭橙警示（`#b87333`）。原生拦截层、启动主题色、图标底色同步换掉，不再出现"界面米白、一弹窗变深蓝"的割裂 |
| **默认入口** | 打开进家长端，孩子能直接看到总额度、排行、操作记录 | **每次打开都进孩子端**，家长要进家长端必须输密码（不再记忆上次模式，孩子重启也绕不过去） |
| **孩子端显示什么** | 每个应用：`今日已用 42 / 限额 40 分` + 进度条 | 每个应用只留三样：名字、剩余分钟（大字）、一条表示"还剩多少"的进度条。超限显示「用完了」+「今天到此为止」。**不再出现已用/限额/总额度**，也不出现任何能改设置的按钮 |
| **家长密码弹窗** | `window.prompt` | 页面内弹窗。**WebView 默认不接管 `onJsPrompt`，`prompt()` 会静默返回 null** —— 一旦默认进孩子端，家长就会被永远关在门外。这是这次顺手堵掉的一个死锁 |
| **新手引导** | 无 | 概览页顶部「四步就能用」，白话四步，可收起，「我的」里能再调出来 |

孩子端**不做**的事（刻意的）：
- 不显示总额度、剩余总时长、使用排行、操作记录；
- 不显示"权限未授予""网页预览"这类家长向横幅；
- 不显示顶部管控状态条（那上面有「暂停 30 分钟」按钮，摆给孩子看等于把开关交给他）。

孩子端**保留**的出口只有一个：家长还没配置时，页面中间有一个「我是家长，去设置」按钮，点了要输密码。防止刚装好就卡死在一个空页面上。

## v2 大改：修掉"一碰就锁、界面自动关闭"

真机实测暴露出三个连锁问题，全部指向同一类错误——**判定与执行没有边界**：

| 现象 | 真实根因 | v2 修法 |
|------|----------|---------|
| 界面一弹出来就自动关闭 | 孩子端的全屏遮罩只要**任一**应用超限就盖满屏幕，而且**没有任何按钮**，盖住就退不出去 | 遮罩不再自动弹出，并补了「返回」出口；超限改由原生拦截层处理 |
| 一碰就锁定、接着退出界面 | `MonitorService` 每 15 秒读一次用量后**无条件** `lockNow()`，完全不看当前前台是哪个应用——所以你在这个 App 里点任何东西，15 秒内屏幕就被锁 | 改为**前台感知**：只在受管控应用真的在前台时才动作；本应用/桌面/系统界面永不触发 |
| 超限后一整天都恢复不了 | 真实用量取自系统、**归零不了**，超一次当天永久超限，没有任何豁免通道 | 新增**今日加时 / 今日豁免**（用加时抵消已用时长，不改每日基准限额） |

### 新的执行模型（默认最温和）

```
前台应用 → Guard 单一裁定（暂停？自己/桌面/系统？超限？冷却中？）
        → Enforcer 统一执行
             ① 全屏拦截层（悬浮窗，默认）   ← 需要「显示在其他应用上层」
             ② 退回桌面（可选）            ← 需要无障碍
             ③ 锁屏（可选，默认关）        ← 需要设备管理员
```

关键约束：

- **默认只做①**，不锁屏、不退回桌面。也就是说默认行为绝不会"锁你的手机"。
- 同一应用 **90 秒冷却**，不会连环弹。
- 家长打开本应用时，拦截层**自动撤掉**，绝不挡住家长自己。
- 家长可以随时**暂停管控 30 分钟 / 立即恢复**，暂停期间完全不拦截。

## v2.1 新增功能（本轮）

在 v2 修净"乱锁乱弹"的基础上，把管控能力做细：

| 功能 | 说明 | 原生位置 |
|------|------|----------|
| **工作日 / 周末分开限额** | 同一个应用可以设两套额度（如上学日 30 分、周末 90 分），按当天是周六日自动切换 | `Guard.Rule(weekday, weekend)` |
| **每日总时长上限** | 所有受管应用**合计**不得超 N 分钟，防止"每个都不超、加起来玩半天" | `Guard.totalLimitMin` |
| **免打扰时段（宵禁）** | 如 22:00→07:00，时段内所有受管应用一律拦；**支持跨零点** | `Guard.inCurfew()` |
| **快用完提醒** | 剩 N 分钟时推一条高优先级通知，每个应用每天只提醒一次（跨天自动重置） | `Guard.shouldNotifyRemaining` + `game_warn` 渠道 |
| **今日使用排行 Top5** | 概览页直接看"今天都在用什么"，不受管的应用也统计 | `UsageUtil.rankingJson` |
| **操作记录（家长审计）** | 加时 / 豁免 / 拦截 / 暂停 / 恢复都留痕，保留最近 40 条，可一键清空 | `Guard.audit` |
| **崩溃日志页** | 「我的 → 崩溃日志」可直接看到上次崩溃的时间、线程、版本、完整栈 | `App.kt` + `GameBridge.getLastCrash` |
| **批量设限额** | 应用页勾选多个应用，一次写入同一个额度 | `index.html #batchApply` |
| **管控状态条** | 顶部常驻：绿=管控中 / 橙=已暂停或宵禁中 / 灰=未配置，一眼看清现在到底管不管 | `Guard.stateJson` |

同时把**所有** `@JavascriptInterface` 方法都套上异常护栏（`safe` / `safeUnit`）——桥接方法抛出的异常会打到 WebView 的 JavaBridge 线程并**直接杀掉应用**，这是"点一下就闪退"的典型死法。

## v2.2：堵掉最后一种"一打开就闪退"

`MainActivity.onRenderProcessGone()`。

WebView 的渲染进程是独立进程。内存紧张、或厂商 ROM 清理后台时，系统会**直接杀掉渲染进程，并默认连宿主应用一起杀掉**。这种"闪退"不是 Java 异常，`CrashHandler` 抓不到，日志里什么都没有——是最难查的一种。

现在改成：拆掉坏掉的 WebView，在主线程消息队列里**重建一个新 WebView 并重新加载页面**，返回 `true` 告诉系统"我自己处理"，应用继续活着。首次启动和重建走的是同一条 `attachWeb()` 路径，不会出现两套逻辑。

## v2.4 新增：专注模式 + 通知栏实时剩余

| 功能 | 说明 |
|---|---|
| **专注模式** | 概览页选 25/45/60/90 分钟，一键开始。期间**受管应用一律打不开**，且**不消耗、不占用今天的额度**——专注结束该用多少还是多少。只作用于你勾选过的应用，电话、短信、相机不会被误锁。拦截页在专注期文案会变成「XX 专注模式中」，第一个按钮变成「家长：结束专注（需密码）」，不会伪装成"时间用完了"。 |
| **通知栏实时剩余** | 常驻通知从「正在监控 N 个应用」改成显示**最紧张的那个**：`王者荣耀 还剩 12 分钟` / `今日总时长还剩 40 分钟` / `专注模式中，还剩 23 分钟`。同时修掉一个隐患：这条通知原本只有服务启动时生成一次，之后永不更新，显示的数字会一直是旧的。 |
| **密码防猜** | 连续错 5 次锁 1 分钟；保存密码时至少 4 位，输入 `1234` 会额外提醒一次。 |

## 下载地址（当前版本 v3.6 (11)）

- 手机下载页：<https://qingjiaowojunge.github.io/kidtime/>
- 仓库：<https://github.com/qingjiaowojunge/kidtime>

> 当前版本 **v3.6 (11)**（`versionName 3.6` / `versionCode 11`），产物 `KidTime-v3.6-11.apk`。这版做了「更好用」的 4 项升级并过了独立只读复核，管控链路与 v3.5 (10) 一致。
> **核对方法**：下载页徽标必须显示 `v3.6 (11)`；显示 `(10)` / `(9)` 或更旧都说明浏览器缓存了旧页，刷新或换浏览器再下。
> 文件名带版本号和构建号，**不会被浏览器缓存成旧包**；每次换版文件名必然变化。

### 为什么换仓库、换包名

旧仓库 `game-manager` 里那个包（`com.example.gamemanager`）踩了两个坑，靠改代码救不回来：

1. **第一版是 CI 临时生成 debug.keystore 签的**，和后来固定的 `gm-release.jks` 对不上 → 新版装不上去（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），只能先卸载；而那一版又开了设备管理员 → 系统拒绝卸载。**签名不一致 + 设备管理员 = 装不上也删不掉**。
2. 同名 `GameManager.apk` 一直被浏览器 / CDN 按 URL 缓存，同一个链接发出去永远是旧包。

所以这一版彻底换新：

| | 旧 | 新 |
|---|---|---|
| 仓库 | `qingjiaowojunge/game-manager` | `qingjiaowojunge/kidtime` |
| 包名 | `com.example.gamemanager` | `com.junge.kidtime` |
| 应用名 | 游戏管理软件 | **时间约定** |
| 版本 | 2.5 (9) | 3.0 (1) |

包名换了，就意味着它是一个**全新安装的应用**，不会跟手机里任何残留的系统安装记录打架（设备管理员残留、旧签名记录都不会再拦它）。代码包名 `com.example.gamemanager` 没动，`applicationId` 单独改，避免改动几百处引用。

### v3.6 (11) 产物核验（当前版本）

| 检查项 | 结果 |
|---|---|
| CI | 待构建（出包后补 run id / commit / 字节数 / sha256） |
| 产物 | `KidTime-v3.6-11.apk` · 下载页徽标必须为 `v3.6 (11)` |
| 包内 | `assets/index.html` 必须含 v3.6 标记：`ME_KEEP` 白名单、`quickBonusMin`、`reqTabDot`、`kidDate`、`bonusApply`、`pauseCard` |
| 签名 | 固定 `gm-release.jks`，与 v3.5 (10) 同证书，**可直接覆盖安装，不必卸载** |
| 边界 | 只改 `index.html` + `build.gradle` 版本号 + 文档；Kotlin / Manifest / 权限清单不变 |

### v3.5 (10) 产物核验（历史版本）

| 检查项 | 结果 |
|---|---|
| CI | run `42024056242` · build ✅ · pages ✅ · 远端 commit `addcb3d5628defbf316f1ca5e0ac0e6959de35ff` |
| 产物 | `KidTime-v3.5-10.apk` · **2,130,067 字节** · sha256 `baa1e2959b9d4d7fb07d8b1ff7e17ac10898655f729090c9060fd139238a1550` · 下载页徽标 `v3.5 (10)`，页面 SHA256 与实际包一致 |
| 门禁修复 | 包内 `assets/index.html` 已逐项核实：`if(!m) return;`、`if(!c.password) c.password='1234';`、`setManagedApps → startMonitorInternal` 注释，**3/3 在位** |
| 包内 | `assets/index.html` 129,518 字节；22 个 v3.5 新功能标记 + 12 个原有功能标记全在位；`confirm/alert/prompt` 残留 0；DOM id 127 / refs 115 / missing 0；前端 38 个 `GameBridge.*` 调用在 Kotlin 49 个 `fun` 中零缺失 |
| 签名 | 固定 `gm-release.jks`（899 字节证书 DER 在 APK 偏移 `2116496` 逐字节命中），与 v3.5 (9) 同证书，**可直接覆盖安装，不必卸载** |
| 下载页 | 主按钮指向 `KidTime-v3.5-10.apk`，无 `v3.5-9` / `v3.4-8` / `v3.3-7` 旧包链接；HTML meta 带 `no-store` |
| 边界 | 只改 `index.html` + `build.gradle` 版本号；Kotlin / Manifest / 权限清单不变 |

### v3.4 (8) 产物核验（历史版本）

| 检查项 | 结果 |
|---|---|
| CI | run `41845480059` · build ✅ · pages ✅ · 远端 commit `bc9fd9e2` |
| 产物 | `KidTime-v3.4-8.apk` · **2,120,215 字节** · sha256 `94613bf1cd819a1d186deaa30614bdf68fc29238ee51ad448a86f99a067412b5` · 下载页徽标 `v3.4 (8)` 且页面 SHA256 与实际包一致 |
| 签名 | 固定 `gm-release.jks`（899 字节证书 DER 在 APK 偏移 `2106644` 逐字节命中），与 v3.3 (7) 同证书，**可直接覆盖安装，不必卸载** |
| 包内 | `assets/index.html` 98,684 字节，含「视觉增强层 · 暖纸感」、`.kid-hd .play:empty`、`kid-card::after`；四步引导 / `pwModal` / 不建议启用 / 自检面板 / `renderKid` / `showChild` 等功能字符串与 v3.3 (7) 一致 |
| 边界 | 只改 CSS；dex / AXML / 权限清单相对 v3.3 (7) 不变（`index.html` diff 为 +280 / -0） |
| 下载页 | 主按钮指向 `KidTime-v3.4-8.apk`，无 `KidTime-v3.3-7.apk` 残留；页面带 `no-store`；APK `content-type=application/vnd.android.package-archive` |

### v3.3 (7) 产物核验（历史版本）

| 检查项 | 结果 |
|---|---|
| CI | run `41840503751` · build ✅ · pages ✅ · commit `59c4578` |
| 产物 | `KidTime-v3.3-7.apk` · **2,115,483 字节** · sha256 `272f0e2a6b2f8fb82b979376f2e3e6200f4f4592ec5db24f43cea641f51d4c0e` · 下载页徽标 `v3.3 (7)` |
| 签名 | 固定 `gm-release.jks`（899 字节证书 DER 在 APK 偏移 `2101912` 逐字节命中），**可直接覆盖安装，不必卸载** |
| 包内 | `assets/index.html` 含自检面板与四项权限文案；dex 含 `Watchdog` / `BootReceiver` / `recordEnforce` / `noteForeground`；AXML 含 `SCHEDULE_EXACT_ALARM`、`RECEIVE_BOOT_COMPLETED`、`com.junge.kidtime` |
| 下载页 | 返回 200、带 `no-store`、APK 的 `content-type` 为 `application/vnd.android.package-archive` |

### v3.0 产物核验（历史，不是"构建成功"就算数，已逐个查过）

| 检查项 | 结果 |
|---|---|
| CI | run `41826577607` · build ✅ · pages ✅ |
| Release | `v1.0.1` · `KidTime-v3.0-1.apk` · **2,106,103 字节** |
| SHA256 | `c5f902619ff36207bd9de140c156b4c5b4a31a70891ceb0ed5ef5ecbd38f621b` |
| 真实包名（解析 `AndroidManifest.xml` 二进制，不是 grep 字符串） | `package = com.junge.kidtime` / `versionName = 3.0` |
| 签名证书 SHA256 | `80:48:4E:2E:7D:C5:74:36:AF:36:EC:96:60:5B:F2:38:27:5B:4B:3F:05:FF:6B:C5:9A:24:D3:0D:D1:7E:F5:53`——用 `keytool -exportcert` 导出 `gm-release.jks` 的证书 DER，在 APK 里**逐字节命中**（偏移 `0x1FEDF4`），不是看指纹字符串猜的 |
| `assets/index.html` | 74,696 字节，含 `--bg:#f6f3ec` / `--primary:#2f6b52` / `--warning:#b87333` / `pwModal` / `四步就能用` / `kidAllDone`；孩子端卡片里**已无**"今日已用/限额" |
| dex | 含 `Guard / Enforcer / OverlayBlocker / BlockActivity / MonitorService / UsageUtil / GameBridge` |
| Pages | <https://qingjiaowojunge.github.io/kidtime/> 返回 200，版本徽标 `v3.0 (1)`，主按钮指向 `KidTime-v3.0-1.apk`；该 URL 返回 `application/vnd.android.package-archive`、长度与 Release **完全一致**（同一份） |

### v2.5 产物核验（历史）

| 检查项 | 结果 |
|---|---|
| CI | run `41632958894`(v1.0.8) / `41633737189`(v1.0.9) / `41634691926`(v1.0.10) 均 build ✅ pages ✅ |
| Release | **`v1.0.15`**（v2.5 build 9）· `GameManager-v2.5-9.apk` · 2,102,967 字节 |
| SHA256 | `200f217848caae36b2458e515ea69bada68a82adcbb6070931751157a57c565e` |
| 签名证书 SHA256 | `80:48:4E:…:7E:F5:53`（与仓库 `android/app/gm-release.jks` 完全一致，已用 APK 签名块逐字节比对） |
| Pages 上的包与 Release 是否同一份 | **是**（同一 sha256） |
| `assets/index.html` | 含 `getTodayRanking / getLastCrash / getAuditLog / addBonus / exemptToday / batchApply / guardBar`，含「工作日 / 周末 / 免打扰 / 每日总时长 / 操作记录 / 崩溃日志」 |
| dex | 含 `Guard / Enforcer / OverlayBlocker / BlockActivity / MonitorService / UsageUtil / GameBridge / MainActivity`，含 `isBlockedNow / shouldNotifyRemaining / inCurfew / totalUsedMinutes / rankingJson / readCrash / auditJson / requestHome` |
| AndroidManifest | 含 `SYSTEM_ALERT_WINDOW`、`PACKAGE_USAGE_STATS`、`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`、`QUERY_ALL_PACKAGES`、`BlockActivity`、`App`；`versionName=2.1` |


**这版彻底去除了假数据**——所有数据都来自手机真实系统：
- 应用清单来自 `PackageManager`（手机真实已装应用，家长从中勾选要管控的）
- 使用时长来自 `UsageStatsManager`（每个应用真实前台耗时，不是应用自己数的）
- 超额判定与锁机由原生前台服务读取真实时长后执行（`DevicePolicyManager.lockNow()` 真锁屏）

## 工作流程（真实可用）
1. **家长端 → 应用页**：拉取手机真实已装应用列表，勾选要管控的应用，设置**工作日 / 周末**两套每日分钟数（也可勾多个批量设）。
2. **家长端 → 概览 / 统计**：今日已用、剩余、限额环形图，Top5 使用排行，以及每个应用近 7 天真实时长柱状图，全部来自系统真实用量。
3. **孩子端**：展示每个应用"真实剩余时间"并实时刷新；剩余不足时推通知，用完由原生拦截层盖住。
4. **后台监控**：原生 `MonitorService` 每 10 秒做一次**前台感知**检查，只对"真的在前台且确实超限"的受管应用动手；同一应用 90 秒冷却，绝不连环弹。
5. **家长随时可干预**：加时 +15 / +30 分、今日豁免、暂停 30 分钟、切换执行策略。
6. **每一步都留痕**：加时、豁免、拦截、暂停、恢复全部写进「操作记录」。

默认家长密码 `1234`，可在「我的」页修改（进入家长端需输密码，防止孩子篡改）。

## 目录结构
```
游戏管理软件v1/
├── index.html                 # 应用本体（HTML/JS/CSS），真实数据驱动，你主要改这里
├── android/                   # 极薄 WebView 壳 + 原生桥接层
│   ├── app/src/main/java/.../
│   │   ├── MainActivity.kt            # WebView 加载 assets/index.html（唯一入口）
│   │   ├── GameBridge.kt              # @JavascriptInterface 暴露真实数据/控制
│   │   ├── UsageUtil.kt               # 已装应用 + 真实用量 + 真实前台应用
│   │   ├── Guard.kt                   # 判定中枢：暂停/前台感知/超限/冷却，唯一裁定入口
│   │   ├── Enforcer.kt                # 执行器：拦截层 →（可选）退桌面 →（可选）锁屏
│   │   ├── OverlayBlocker.kt          # 悬浮窗全屏拦截层（默认通道）
│   │   ├── BlockActivity.kt           # 全屏拦截页（悬浮窗不可用时的备用通道）
│   │   ├── MonitorService.kt          # 前台服务：每 10 秒前台感知检查
│   │   ├── AppMonitorAccessibilityService.kt  # 可选：退回桌面
│   │   └── AdminReceiver.kt           # 可选：设备管理员
│   ├── app/src/main/res/...   # 图标、主题、无障碍描述
│   ├── app/build.gradle / build.gradle / settings.gradle / gradle.properties
├── .github/workflows/build-apk.yml   # 推到 GitHub 自动出 APK
├── push.bat                   # 一键推送并触发出包（Windows）
└── README.md
```

## 本地预览（不打包也能看）
直接双击 `index.html`，或起个静态服务器：
```bash
python -m http.server 8000
# 浏览器打开 http://localhost:8000
```
> 浏览器里是"网页预览模式"：没有真实设备数据，界面只展示结构。装到手机（APK）后才读得到真实已装应用与真实用量。

## 一键出 APK（推到 GitHub，GitHub 自动打包）
本仓库内置 `.github/workflows/build-apk.yml`：推送即自动把 `index.html` 编译进 APK 并发 Release。
最简单的做法：在本目录运行 `push.bat`（需先 `gh auth login`）。或手动：
```bash
git init
git add .
git commit -m "first version"
git branch -M main
git remote add origin https://github.com/<你的用户名>/<仓库名>.git
git push -u origin main
```
等 Actions 跑完（约 3–5 分钟）。出包后有两种装法：

- **手机直接装（推荐，无需 GitHub 账号）**：打开 `https://<你的用户名>.github.io/<仓库名>/`（构建时自动部署到 GitHub Pages），或扫 Release 里的 `qr.png`，页面直接下载 `GameManager.apk`。
  - 需在仓库 **Settings → Pages → Source** 选择 **GitHub Actions**（`push.bat` 会用 `gh` 自动帮你开；没开成则手动设一次）。
- **Release 下载（需登录 GitHub）**：进仓库 **Releases** 点 `GameManager.apk`。

之后每次 `git push` 都会重新出包并更新 Pages，手机下载页地址不变。

## 卸载本应用（重要，否则会"删不掉"）

安卓规定：**只要应用是「设备管理员」，系统就禁止卸载**，任何查杀软件都不会报毒——它本来就不是毒。这是 v2.5 之前"装了删不掉"的唯一原因。

**正确顺序**：

1. 打开应用 → 底栏「我的」→ 最下面「暂停 / 重置」→ 点 **「彻底解除管控（解除后才能卸载本应用）」** → 确认
2. 回桌面长按图标 → 卸载（或 设置 → 应用 → 应用管理 → 游戏管理软件 → 卸载）

**如果应用已经打不开了**，直接去系统设置手动取消：

| 品牌 | 路径 |
|---|---|
| 荣耀 / 华为 | 设置 → 安全 → 更多安全设置 → **设备管理器** → 取消勾选「游戏管理软件」 |
| 小米 / Redmi | 设置 → 密码与安全 → 系统安全 → **设备管理应用** |
| OPPO / vivo / 一加 | 设置 → 安全 → **设备管理器**（或 系统安全 → 设备管理） |
| 都不行 | 设置顶部搜索框直接搜「设备管理」 |

取消激活后即可正常卸载。仍不行则用电脑：`adb shell dpm remove-active-admin com.example.gamemanager/.AdminReceiver` 然后 `adb uninstall com.example.gamemanager`。

## 版本之间可以直接覆盖升级（v2.5 起）

v2.5 之前每次构建都用 CI 临时生成的调试签名，**每个版本签名都不同** → 安卓拒绝覆盖安装 → 只能先卸载 → 而卸载又被设备管理员挡住，形成死锁。

现在仓库里固定了 `android/app/gm-release.jks`，`debug` 与 `release` 都签同一套证书。**从 v2.5 开始，后续版本可以直接覆盖安装，不再需要卸载。**

> 代价：keystore 在公开仓库里（含固定口令）。本项目是自用工具、不上架任何商店，风险仅为"别人可以用同签名打包一个同名应用"，实际影响可忽略。若要公开发布，应改为 GitHub Secrets 存放 keystore。

## 必须授权（否则管控不生效）
| 权限 | 作用 | 是否必需 |
|------|------|----------|
| **使用情况访问**（Usage Access） | 读取每个应用真实前台时长 | **必需**——不授权则所有时长显示为 0，监控完全失效 |
| **显示在其他应用上层**（悬浮窗） | 超额时盖出全屏拦截层 | **必需**（Android 10+ 后台服务不能随意起 Activity，没有它弹不出拦截） |
| **设备管理员** | 真锁屏 `lockNow()` | 仅当你在「超额后怎么处理」里选了“拦截页 + 直接锁屏”才需要 |
| **无障碍服务** | 把超限应用退回桌面 | 仅当你在「超额后怎么处理」里选了“拦截页 + 退回桌面”才需要 |
| （安装时自动声明）`QUERY_ALL_PACKAGES` | 列出手机全部已装应用 | 系统级，Android 11+ 必需 |

授权入口在家长端「我的 → 系统能力授权」卡片，点对应按钮跳转系统设置。

## 未授权 / 已授权 能力对照
| 能力 | 未授权 | 已授权 |
|------|--------|--------|
| 列出真实已装应用 | 不可用（需 QUERY_ALL_PACKAGES，安装即带） | 可用 |
| 读取真实使用时长 | **不可用（0）** | 可用 |
| 概览 / 统计（真实数据） | 不可用 | 可用 |
| 孩子端真实剩余时间 | 不可用 | 可用 |
| 超额弹全屏拦截层 | 不可用（只能发一条通知） | 可用（悬浮窗） |
| 超额退回桌面 | 不可用 | 可用（无障碍） |
| 超额锁屏 | 不可用 | 可用（设备管理员） |

## 已知边界（诚实说明）
- 应用无法篡改系统记录的真实用量；家长只能"今日加时 / 今日豁免"来放宽，不能把已用时长清零。
- 默认策略**不锁屏**，只盖拦截层。孩子能按"回到桌面"关掉拦截层，但如果再打开那个应用，冷却结束后仍会被重新拦住。
- 拦截层是悬浮窗，**会盖住整台手机**（这正是拦截的意思）；家长打开本应用时会自动撤掉，不会挡自己。
- 若既不授权悬浮窗、也不开无障碍，超额时只能发通知——这是安卓系统限制，不是应用偷懒。
- 不同厂商 ROM 对悬浮窗 / 设备管理员 / 无障碍 / 后台保活有额外限制，必要时需在系统"电池/自启动/应用管理"里加白。
- 本应用面向家长自主管理自用或子女设备，请遵守当地法律法规与未成年人保护相关规定。

## 出包踩坑记录（改代码前先看，避免重犯）
| 曾经报错 | 真实原因 | 正确做法 |
|---|---|---|
| 手机下载到的总是旧包 | 浏览器与 Pages CDN 按 **URL** 缓存，文件名固定就会一直下发旧包 | 产物文件名带版本号 `GameManager-v<版本>-<构建号>.apk`，每次发版文件名必变；下载页也标出版本号便于核对 |
| 下载页版本号显示为空 | `GITHUB_ENV` 只在**单个 job 内**有效，`pages` 是独立 job 拿不到 `build` 里设的变量 | 在 `pages` 任务里从产物文件名 `GameManager-v*-*.apk` 反解版本号 |
| `git push` 报 `schannel: failed to receive handshake` / `CONNECT tunnel failed, response 502` | 本机到 `github.com:443` 被出口代理拦截，但 `api.github.com` 正常 | 用 `python push_api.py`，走 Git Data API（blob→tree→commit→改 ref），内容按 tree 校验，等价 push 且照样触发 Actions |
| `sdkmanager tools` 退出码 1 | `android-actions/setup-android@v3` 与 runner 自带 cmdline-tools 冲突 | 用 runner 自带 `/usr/local/lib/android/sdk` + 显式 `sdkmanager` 装 platform/build-tools，并写 `android/local.properties` |
| `Unresolved reference: FEEDBACK_ALL_MASK` | 常量属于 `AccessibilityServiceInfo`，不是 `AccessibilityManager` | `AccessibilityServiceInfo.FEEDBACK_ALL_MASK` |
| `'onEnabled' overrides nothing` | Java 平台类型参数不能用 `Context?` / `Intent?` 重写 | 用非空 `Context` / `Intent`，或直接不写这两个重写 |
| `Type ...MainActivity is defined multiple times`（dex 阶段） | 同名 `.java` 与 `.kt` 同时存在 | 一个类只保留一份源码 |
