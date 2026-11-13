# GitHub Actions 自动编译

推代码上 GitHub 就自动编译出 APK，唔使自己装环境。

## 使用方法

### 1. 创建 GitHub 仓库

1. 去 [github.com/new](https://github.com/new) 创建仓库
   - 名字：`miplay-manager`（或任意）
   - 公开（Public）= 完全免费无限分钟
   - 私人（Private）= 每月 2000 分钟免费

### 2. 上传代码

```bash
cd miplay-android-app
git init
git add .
git commit -m "MiPlay Manager v2.0"
git branch -M main
git remote add origin https://github.com/你的用户名/miplay-manager.git
git push -u origin main
```

### 3. 下载 APK

1. 去仓库页面点 **Actions** 标签
2. 点最新一次运行（Build MiPlay Manager APK）
3. 拉到最底，**Artifacts** 栏下载 `miplay-manager-v2`
4. 解压出 `app-debug.apk`，传到手机安装

### 4. 以后更新

改完代码再 `git push`，GitHub 自动重新编译，新 APK 喺 Actions 度下载。

## 免费额度

- 公开仓库：无限分钟，完全免费
- 私人仓库：每月 2000 分钟（Linux 机）
- 每次编译约 3-5 分钟，够编译 400-600 次

## 手动触发

唔想推代码都可以手动编译：
仓库页面 → Actions → Build MiPlay Manager APK → Run workflow
