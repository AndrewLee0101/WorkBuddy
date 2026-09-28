# 岚播放器 · ArashiPlayer

> 风起时，替你收藏每一帧声音与画面。
>
> **包名** `com.arashiplayer` · **版本** 1.0.0 · **minSdk** 24 · **targetSdk** 35

一个本地视频 / 音乐播放器，Kotlin + Jetpack Compose 全量实现。
视频端对齐 QQ影音 / Reex 这类桌面级播放器的功能密度，音乐端走苹果 Apple Music 的克制观感。

---

## 一、功能清单

### 音乐
- 本地音乐扫描（MediaStore 优先，文件系统兜底），按 歌曲 / 歌手 / 专辑 / 文件夹 四种维度浏览
- 苹果风正在播放页：封面高斯模糊背景、呼吸缩放缓动、渐变进度条、拖动时间气泡
- **在线封面与歌词匹配**（思路参考「音乐标签」）：以「歌手 + 标题」检索 → 标题相似度 + 时长差 + 歌手命中三项打分选最优 → 拉取专辑封面与 LRC（含翻译），结果落库缓存，下次直接读
- 歌词面板：按时间轴自动高亮并居中滚动，支持译文行、点击行跳转
- 倍速 0.25x ~ 4x、收藏、随机 / 循环三态、后台播放（MediaSession 前台服务）

### 视频
- 视频库：网格 / 列表切换、文件夹筛选、多维排序（最近添加 / 名称 / 大小 / 时长）、全屏搜索
- 播放器（自绘控制层，非 PlayerView 默认控件）：
  - **全格式**：MP4 / MKV / WebM / AVI / MOV / FLV / TS / M2TS / WMV / RMVB / VOB / 3GP / M3U8(HLS) / DASH / RTSP …
    音频侧 MP3 / FLAC / WAV / AAC / M4A / OGG / OPUS / WMA / AMR / AC3 / EAC3 等
  - 硬解 / 软解一键切换，切换后保留播放进度
  - 手势：单击显隐控制层、双击左右屏 ±10s、左侧纵向调亮度、右侧纵向调音量、横向拖调进度、双指捏合缩放
  - 画面比例：适应屏幕 / 拉伸铺满 / 裁剪铺满 / 16:9 / 4:3 / 原始比例
  - 倍速、锁定屏幕、旋转、截图（PixelCopy 抓帧存 `Pictures/岚播放器/`）、定时关闭、音频轨道切换
  - 断点续播（每 5 秒落一次书签）+ 同目录自动连播
  - 常驻状态提示：分辨率、硬/软解、倍速、字幕状态

### 外挂字幕（四要素可调）
- 加载方式：手动选择（`ACTION_OPEN_DOCUMENT` + 持久化授权）／**同目录同名自动挂载**
- 支持格式：SRT、ASS/SSA、VTT、SMI、TTML、LRC
- 可调项：
  | 项目 | 范围 | 实现 |
  |---|---|---|
  | 颜色 | 9 色预设 + 自定义 | `CaptionStyleCompat.foregroundColor` |
  | 透明度 | 0.2 ~ 1.0 | 前景色 Alpha 通道重算 |
  | 位置 | 底部内边距 0 ~ 45% | `SubtitleView.setBottomPaddingFraction` |
  | 大小 | 12 ~ 40sp | `setFractionalTextSize` 按 20sp 基准换算 |
  | 描边 | 外描边 / 投影 / 背景框 / 无 | `edgeType` + `edgeColor` |
  | 加粗 | 开 / 关 | `setTypeface` + `setApplyEmbeddedStyles` |
  | 时间轴偏移 | ±5s | `Player.Listener.onCues` 接管后平移投喂 |
- 面板内含 **16:9 实时预览框**，改哪一项立刻能看到效果；样式改动 350ms 防抖落盘，拖动不卡

### 加密空间
- **只改名换后缀，不复制文件、不额外占用系统空间**（全程 `File.renameTo`，同一文件系统内零字节开销）
  ```
  /Movies/旅行.mp4   →   /Movies/.arashi_vault/8f3a1c9e7b2d40aa.arv
  ```
- 同时在所在目录写入 `.nomedia`，相册与第三方扫描器直接跳过；`.arv` 后缀也不被 MediaStore 识别，
  所以系统图库、其他 App 的媒体扫描都看不到
- 两种解锁方式，用户自选：**数字密码**（4~6 位，PIN 键盘）／**手势密码**（九宫格，至少连 4 点）
- 凭据用 PBKDF2-HMAC-SHA256（12000 轮 + 16 字节随机盐）派生摘要后存放，**不保存明文**
- 连续 5 次输错锁定 30 秒倒计时；错误时震感 + 抖动
- 图片 / 视频 / 音频 / 文档都能进来；支持应用内预览（图片可缩放）、单条移出、彻底删除、一键全部移出
- 离开页面自动回锁，回锁时间可在设置里选（立即 / 15 秒 / 1 分钟 / 5 分钟）

### 设置
- 外观：主题模式（跟随系统 / 浅色 / 深色）、6 套主题色
- 播放：自动匹配在线封面歌词、硬/软解、手势控制、记忆播放位置
- 安全：加密空间管理、修改密码、解锁方式、隐藏加密缩略图、自动回锁时间
- 关于：
  - **联系作者** —— 行尾带小红书标识，点击进入作者主页
    <https://www.xiaohongshu.com/user/profile/65d47c96000000000401e154>
  - **赞赏作者** —— 支付宝收款码 + 微信赞赏码，页首附言「您的支持是我坚持的动力」

---

## 二、技术栈

| 层 | 选型 |
|---|---|
| 语言 / UI | Kotlin 2.0.21 · Jetpack Compose（BOM 2024.12.01）· Material 3 |
| 播放内核 | AndroidX Media3 1.5.1（ExoPlayer / HLS / DASH / RTSP / UI / Session / Extractor） |
| 本地存储 | Room 2.6.1（加密索引、在线元数据缓存、书签、最近播放、歌单） |
| 偏好设置 | DataStore Preferences |
| 图片加载 | Coil 2.7.0（含 `coil-video` 视频抽帧） |
| 网络 | OkHttp 4.12 + kotlinx.serialization |
| 导航 | Navigation Compose 2.8.5 |
| 权限 | Accompanist Permissions |

架构是单 Activity + Compose Navigation，数据层用极简 Service Locator（`ArashiApp`）注入，
没有 Hilt/Koin —— 目的是让这份代码 clone 下来 `./gradlew assembleDebug` 就能出包，不依赖注解处理顺序。

---

## 三、目录结构

```
ArashiPlayer/
├─ app/src/main/java/com/arashiplayer/
│  ├─ ArashiApp.kt                 应用入口 / 服务定位器
│  ├─ MainActivity.kt              权限申请 + 主题装载
│  ├─ data/
│  │  ├─ model/Models.kt           全部数据模型（含 SubtitleStyle）
│  │  ├─ local/Db.kt               Room 实体 / DAO / Database
│  │  ├─ local/SettingsStore.kt    DataStore 偏好
│  │  ├─ repo/MediaRepository.kt   MediaStore 扫描
│  │  ├─ repo/VaultRepository.kt   加密空间（原地改名）
│  │  └─ net/OnlineMetaApi.kt      在线封面歌词匹配 + LRC 解析
│  ├─ player/
│  │  ├─ PlayerHolder.kt           全局 ExoPlayer（硬软解切换）
│  │  └─ ArashiPlaybackService.kt  MediaSession 前台服务
│  ├─ security/LockKit.kt          PBKDF2 / 路径解析 / 混淆命名
│  ├─ ui/
│  │  ├─ theme/Theme.kt            设计令牌（ArashiPalette / Shape / Typography）
│  │  ├─ components/Common.kt      卡片、分组行、空态、小红书标识
│  │  ├─ navigation/ArashiNav.kt   全部路由
│  │  └─ screens/
│  │     ├─ home/                  首页
│  │     ├─ music/                 音乐库 / 正在播放 / ViewModel
│  │     ├─ video/                 视频库
│  │     ├─ player/                视频播放器 / 字幕样式面板
│  │     ├─ vault/                 加密空间 / 手势与 PIN 控件
│  │     └─ settings/              设置 / 联系作者 / 赞赏作者
│  └─ util/Utils.kt                时间、体积、扩展名、路径常量
└─ app/src/main/res/
   ├─ drawable/ic_launcher_foreground.xml   图标前景（左视频·右音乐·中间风）
   ├─ drawable/ic_launcher_background.xml   岚色渐变底
   ├─ drawable/ic_reward_alipay.jpg         支付宝收款码
   ├─ drawable/ic_reward_wechat.png         微信赞赏码
   └─ ...
```

---

## 四、编译

环境要求：JDK 17、Android SDK 35、Android Studio Ladybug 及以上。

```bash
# 打开工程
# Android Studio → Open → 选择 ArashiPlayer/ 目录

# 或命令行
cd ArashiPlayer
./gradlew assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # 正式包（需自备签名，见 app/build.gradle.kts）
```

> 首次同步会拉取 Gradle 8.11.1 与依赖。国内网络可在 `settings.gradle.kts` 中把
> `mavenCentral()` 换成阿里云镜像（已预置一行 `maven.aliyun.com` 镜像）。

---

## 五、权限说明

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `READ_MEDIA_IMAGES` | 扫描本地媒体（Android 13+） |
| `READ_EXTERNAL_STORAGE`（≤ Android 12） | 同上 |
| `MANAGE_EXTERNAL_STORAGE` | 加密空间需要在任意目录内**原地改名**；不授予则只能对 App 可访问目录生效 |
| `POST_NOTIFICATIONS` | 音乐后台播放的通知栏控制 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台不断音 |
| `INTERNET` | 在线匹配封面与歌词 |

App **不收集任何用户数据**，不联网上报。唯一的网络请求是发往公开音乐接口的检索与歌词拉取，
可以在设置里整个关掉（关掉后完全离线可用）。

---

## 六、关于加密空间，需要说清楚的两件事

1. **它是「藏」，不是「锁」。** 按需求实现为改名 + 改后缀，因此零额外空间开销，速度是瞬时的。
   但文件本体并未被密码学打乱 —— 如果有人拿到手机并用文件管理器翻到 `.arashi_vault` 目录，
   改回后缀就能打开。
2. **如果你要的是「拷走也打不开」** 这个强度，需要 AES-256 加密 + 分块复制存储，代价是
   额外占用一份空间、加密大文件有耗时。代码里已经把 `VaultRepository.hide()` 收敛成一个入口点，
   加一个 `CipherOutputStream` 分支即可平滑升级，不会影响 UI 层。

选哪种由你定，代码结构和注释里都留了口子。

---

## 七、已知限制

- APE / DTS 等冷门无损编码依赖设备解码器，部分机型需要走软解或提前转码
- ASS 的复杂特效（\k 卡拉OK、\move 位移、矢量绘图）不会渲染，只按普通字幕显示
- 字幕时间轴偏移的**负值**（提前）受 Media3 投递管线限制，只能原速显示；正值精确
- 未做平板 / 折叠屏的横屏分栏布局，大屏上音乐库是单列拉伸

---

## 八、版权与免责

- 本工程为个人学习与自用工具，不含任何内容源，播放的是用户自己的本地文件
- 在线匹配使用的是公开接口，仅用于为个人本地曲库补全元数据，请勿用于分发或商业用途
- 请勿使用本工具播放、存储或传播任何侵犯他人著作权的内容
- 「小红书」为小红书科技有限公司的商标，「支付宝」「微信」分别为蚂蚁集团、腾讯公司的商标，
  本项目仅作跳转与收款展示，与上述公司无任何关联
