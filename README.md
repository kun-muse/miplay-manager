# 妙播管理器 Android App v2.0

完整控制屋企 Debian J3160 上嘅妙播（miplay-bridge），对应网页控制台 `http://192.168.1.10:8092/` 嘅全部功能。

## 功能（v2.0，完整版）

### 状态显示
- ✅ 在线状态、当前来源（妙播 / DLNA / MPD / 空闲）
- ✅ 妙播连接状态、DLNA 状态、播放状态
- ✅ 声卡信息（QCC5125，Card 0）
- ✅ 设备名「飞牛音箱」

### 音量控制（对应网页 Mixer）
- ✅ PCM 音量滑块（0–15 原始值，实时写入）
- ✅ 静音开关（pswitch on/off）

### 播放控制（对应 /api/control）
- ✅ 播放/暂停（toggle）
- ✅ 停止（stop）
- ✅ 上一首（prev）、下一首（next）

### 连接管理（对应网页按钮）
- ✅ 清除妙播连接（/api/miplay/disconnect）
- ✅ 清除 DLNA 连接（/api/dlna/disconnect）
- ✅ 全部停止（/api/control stop_all）

### 测试
- ✅ 测试声卡（/api/test-play，440Hz 测试音 2 秒）

## 服务器

- 地址：192.168.1.10:8092
- 设备：飞牛音箱（DLNA + AirPlay）
- 声卡：QCC5125（Card 0），PCM 0–15，默认 10（67%）

## API 对应表

| App 功能 | API |
|---------|-----|
| 刷新状态 | GET /api/status + GET /api/mixer |
| 设音量 | POST /api/mixer `{"name":"PCM","stream":"playback","volume":[N]}` |
| 静音 | POST /api/mixer `{"name":"PCM","stream":"playback","switch":"off"/"on"}` |
| 播放/暂停/停止/上下首 | POST /api/control `{"action":"toggle"/"stop"/"next"/"prev"}` |
| 全部停止 | POST /api/control `{"action":"stop_all"}` |
| 清除妙播 | POST /api/miplay/disconnect |
| 清除 DLNA | POST /api/dlna/disconnect |
| 测试音 | POST /api/test-play |

## 编译方法

### Android Studio（推荐）
1. 打开 `miplay-android-app` 文件夹
2. 等 Gradle 同步完成
3. 接手机点 Run ▶️

### 命令行（Debian J3160 已验证）
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/kun/android-sdk
cd /home/kun/miplay-android-app
gradle assembleDebug --no-daemon
# APK：app/build/outputs/apk/debug/app-debug.apk
```

## 使用要求

- 手机同 Debian 连同一个 Wi-Fi（192.168.1.x）
- Debian 开机（每日 08:00–01:00）
- 妙播容器运行中
