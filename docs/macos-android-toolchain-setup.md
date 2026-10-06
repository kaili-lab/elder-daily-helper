# macOS Android 最小环境安装步骤

本指南记录独立 JDK 和 Android SDK 的最小安装方案及 2026-10-03 的实际执行结果。不安装 Android Studio、模拟器、全局 Gradle 或 Kotlin，不修改 Homebrew、IntelliJ、Toolbox、OrbStack、Xcode 或 shell 启动文件。

## 1. 版本与目录

- 本机已确认是 Apple Silicon（arm64）。
- 本次推荐独立 Temurin JDK 21；工程随后必须选择支持它的 Gradle Wrapper（Gradle 8.5 及以上），并同时核对 Android Gradle Plugin 的兼容要求。
- SDK 先安装 Platform 36 和 Build-Tools 36.0.0。最低支持 Android 12（minSdk 31）不要求另装 Platform 31；编译平台和最低支持版本不是同一个概念。
- 后续工程先以 compileSdk 36 为构建基线；targetSdk 在创建工程时确定。
- 不使用 IntelliJ 内置 JBR 25 作为项目 JDK。

所有新增工具集中到：

```text
~/Developer/android-toolchain/
├── downloads/       下载原包
├── staging/         解压临时目录
├── jdk-21/          整个 macOS JDK bundle
├── sdk/             Android SDK
├── gradle-user/     Gradle 下载和依赖缓存
├── android-user/    Android 工具用户配置（支持该变量的工具）
└── env.zsh          手动加载的环境配置
```

采用每次开发时手动 `source`，不改 `~/.zprofile` 或 `~/.zshrc`，因此不会让新 JDK 默认影响其他终端或软件。图形应用不会自动继承它；如果以后在 IntelliJ 中构建，需单独设置 Gradle JDK 和 SDK 路径。

## 2. 下载两个官方压缩包

在浏览器打开以下链接，下载后先保留在“下载”目录。不要选择 `.pkg` 安装器。

### JDK

[Temurin 官方发布页](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1)

[下载 JDK 21.0.12.1+1，macOS aarch64，tar.gz](https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz)

文件名：`OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz`

SHA-256：`3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998`

### Android command-line tools

打开 [Android 官方下载页](https://developer.android.com/studio#command-line-tools-only)，找到 **Command line tools only → Mac (ARM)**，阅读并接受下载许可后下载：

文件名：`commandlinetools-mac_arm64-15859902_latest.zip`

SHA-256：`835b62a26162b229b441d1f6d4680383815a270809eb33522c0d480fa5002c4e`

如果页面文件名或版本发生变化，停在这里，重新核对该文件的官方校验码；不要拿旧校验码检查新包。浏览器可能自动解压 ZIP，请在下载设置中关闭自动解压，或保留原 ZIP 再继续。

## 3. 建目录、校验下载

打开终端。先检查目标目录：

```zsh
test ! -e "$HOME/Developer/android-toolchain" && echo '可以新建' || echo '目录已存在：停止，先检查内容'
```

只有显示“可以新建”才继续，避免覆盖已有安装。

```zsh
mkdir -p "$HOME/Developer/android-toolchain"/{downloads,staging/jdk,staging/android,sdk/cmdline-tools,gradle-user,android-user}
cp -n "$HOME/Downloads/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz" "$HOME/Developer/android-toolchain/downloads/"
cp -n "$HOME/Downloads/commandlinetools-mac_arm64-15859902_latest.zip" "$HOME/Developer/android-toolchain/downloads/"
shasum -a 256 "$HOME/Developer/android-toolchain/downloads/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz"
shasum -a 256 "$HOME/Developer/android-toolchain/downloads/commandlinetools-mac_arm64-15859902_latest.zip"
```

逐一与第 2 节校验码比较；完全一致才解压。找不到文件时先检查浏览器保存位置或重命名，不继续运行后面的命令。

## 4. 解压并整理目录

```zsh
tar -xzf "$HOME/Developer/android-toolchain/downloads/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz" -C "$HOME/Developer/android-toolchain/staging/jdk"
unzip -q "$HOME/Developer/android-toolchain/downloads/commandlinetools-mac_arm64-15859902_latest.zip" -d "$HOME/Developer/android-toolchain/staging/android"
open "$HOME/Developer/android-toolchain/staging/jdk"
```

在访达中，把刚解出的 **包含 `Contents/Home` 的整个 JDK 文件夹**移到 `~/Developer/android-toolchain/`，并重命名为 `jdk-21`。不同压缩包解出的顶层目录名可能不同，不猜原名，也不要只移动 `Contents/Home`。

Android 包执行：

```zsh
mv "$HOME/Developer/android-toolchain/staging/android/cmdline-tools" "$HOME/Developer/android-toolchain/sdk/cmdline-tools/latest"
test -x "$HOME/Developer/android-toolchain/jdk-21/Contents/Home/bin/java" && echo 'JDK 层级正确'
test -x "$HOME/Developer/android-toolchain/sdk/cmdline-tools/latest/bin/sdkmanager" && echo 'SDK 层级正确'
```

两项都应成功。正确路径是 `sdk/cmdline-tools/latest/bin/sdkmanager`，不是 `latest/cmdline-tools/bin/sdkmanager`。

## 5. 创建手动加载的环境文件

```zsh
nano "$HOME/Developer/android-toolchain/env.zsh"
```

在编辑器中输入：

```zsh
export ANDROID_TOOLCHAIN="$HOME/Developer/android-toolchain"
export JAVA_HOME="$ANDROID_TOOLCHAIN/jdk-21/Contents/Home"
export ANDROID_HOME="$ANDROID_TOOLCHAIN/sdk"
export ANDROID_USER_HOME="$ANDROID_TOOLCHAIN/android-user"
export GRADLE_USER_HOME="$ANDROID_TOOLCHAIN/gradle-user"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

按 Control+O、Enter 保存，Control+X 退出。不要设置已弃用的 `ANDROID_SDK_ROOT`，也不要使用 `sudo`。

加载环境后检查 JDK；Command-line Tools 版本直接读取随包文件，不为查询版本运行 Android CLI 或 `sdkmanager`：

```zsh
source "$HOME/Developer/android-toolchain/env.zsh"
command -v java
java -version
javac -version
file "$JAVA_HOME/bin/java"
sed -n '1,3p' "$ANDROID_HOME/cmdline-tools/latest/source.properties"
```

预期：`java` 路径指向专用目录，版本为 21；`file` 显示 arm64；`source.properties` 显示 Command-line Tools 版本。当前 `bin/android --version` 会联网下载并执行另一套 CLI，`sdkmanager --version` 也可能补写 SDK 元数据，因此二者都不是纯只读检查。若提示恶意软件或安全拦截，停止并保留完整提示，不关闭 Gatekeeper，也不批量清除隔离属性。

## 6. 接受许可、安装 SDK 包

需要联网。官方当前将 `sdkmanager` 标为弃用，但仍提供该工具及以下安装/许可证命令；本次不增加 Android CLI 安装。

第 6、7 节沿用第 5 节已执行 `source` 的同一个终端；如果换了终端，先重新加载 `env.zsh`，再执行安装和检查。

```zsh
sdkmanager --sdk_root="$ANDROID_HOME" --licenses
```

阅读许可，只在同意时输入 `y`。不要用 `yes | ...` 自动同意。

```zsh
sdkmanager --sdk_root="$ANDROID_HOME" --channel=0 "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

如再次提示许可，阅读后确认。下载失败或包不可用时，保留错误，不改用不明镜像、不设置系统代理、不盲目更换版本。

## 7. 验收环境

```zsh
adb version
aapt2 version
test -f "$ANDROID_HOME/platforms/android-36/android.jar" && echo 'Platform 36 已安装'
test -x "$ANDROID_HOME/build-tools/36.0.0/aapt2" && echo 'Build-Tools 已安装'
sed -n '1,10p' "$ANDROID_HOME/platform-tools/source.properties"
sed -n '1,10p' "$ANDROID_HOME/platforms/android-36/source.properties"
sed -n '1,10p' "$ANDROID_HOME/build-tools/36.0.0/source.properties"
printf 'JAVA_HOME=%s\nANDROID_HOME=%s\nGRADLE_USER_HOME=%s\nANDROID_USER_HOME=%s\n' "$JAVA_HOME" "$ANDROID_HOME" "$GRADLE_USER_HOME" "$ANDROID_USER_HOME"
```

确认三个 `source.properties` 分别对应 Platform-Tools、API 36 和 Build-Tools 36.0.0，`adb`、`aapt2` 可运行，打印的路径全部指向专用目录。此时完成的是环境安装，不是 APK 构建验证；工程尚未创建。

每次新开开发终端，先执行：

```zsh
source "$HOME/Developer/android-toolchain/env.zsh"
```

之后在项目目录使用 `./gradlew`，不运行全局 Gradle。第一次构建会下载 Gradle 和依赖，写入 `gradle-user`；项目本身仍会生成 `.gradle/` 和各模块 `build/`。

没有 Android Studio 就没有 Compose 可视化预览和工程向导；工程配置由我们维护。没有模拟器且暂时没有真机，不能宣称已经验证页面或锁屏效果。

## 8. 卸载和保留范围

现在不执行卸载。以后需要卸载时：

1. 如果已经运行项目，在加载环境的终端、项目目录运行 `./gradlew --stop`；如果使用过 ADB，运行 `adb kill-server`。
2. 关闭已加载环境的终端及使用该 JDK 的构建程序。
3. 在访达中将 `~/Developer/android-toolchain` 移到废纸篓；它包含 JDK、SDK、env.zsh、下载原包和集中缓存。
4. 本指南不修改 shell 配置，不需要删除整个 `~/.zprofile` 或 `~/.zshrc`。如果以后添加自动加载区块，只删除对应标记区块，保留 Toolbox、OrbStack 等现有配置。
5. 检查项目的 `.gradle/`、根目录和模块 `build/`、`local.properties` 中的 `sdk.dir`，以及 IDE 中指向这套 JDK/SDK 的设置；仅清理本项目的生成文件或路径引用，不删代码或签名密钥。
6. 检查 `~/.gradle/` 和 `~/.android/` 是否出现额外缓存。环境变量能减少分散文件，但不保证所有版本的 ADB/Android 工具都遵守同一路径；尤其要检查 ADB 密钥。只清理确认属于本次环境的文件，不能删除其他项目共用内容。删除 ADB 密钥后手机需要重新授权。
7. 下载目录可能还留有两个原包，按实际文件名清理；系统临时目录也可能保留工具临时文件，不承诺零残留。

不删除 `/opt/homebrew`、IntelliJ、JetBrains Toolbox、其内置 JBR、OrbStack、Xcode Command Line Tools 或它们的配置。本方案“便于卸载”指新增内容有明确范围，不等于所有 macOS 软件都能只删目录卸载。

## 9. 本机实际安装记录（2026-10-03）

- 工具根目录：`仓库外的专用 Android 工具链目录`；没有修改 `~/.zprofile` 或 `~/.zshrc`，也没有使用 `sudo` 或清除 Gatekeeper 隔离属性。
- JDK：Temurin `21.0.12.1+1`，路径为 `jdk-21/Contents/Home`；下载包 SHA-256 为 `3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998`，代码签名身份为 Eclipse Foundation, Inc.（`JCDTMS22B4`），`java` 与 `javac` 冒烟通过。
- Command-line Tools：`22.0`，路径为 `sdk/cmdline-tools/latest`；下载包 SHA-256 为 `835b62a26162b229b441d1f6d4680383815a270809eb33522c0d480fa5002c4e`。ZIP 的 330 个条目已核对，无符号链接、绝对路径、`..`、setuid 或加密条目；原生 `bin/android` 严格验签通过，但未执行，因为它会联网下载另一套 CLI。
- 许可证：只接受了 `android-sdk-license`；Google TV、Android XR、ARM DBT、SDK Preview、Glass GDK 和 MIPS 镜像许可证均拒绝。
- 已安装组件：Platform-Tools `37.0.1`、Android SDK Platform 36 revision 2、Build-Tools `36.0.0`。`adb version` 输出 `37.0.1-15733141`；`aapt2 version` 输出 `2.20-13193326`；`platforms/android-36/android.jar` 已存在。
- 隔离边界：工具配置和仓库缓存写入 `android-user/.android/`；宿主 `用户主目录下的 .android` 不存在。SDK 安装还生成了各组件的 `package.xml` 及 SDK 内部临时目录，均位于工具链根目录内。
- 安全检查：Platform-Tools 和 Build-Tools 中识别出的全部 macOS Mach-O 文件均通过 `codesign --verify --strict`。组件由隔离环境中的 `sdkmanager` 从默认 Google HTTPS 仓库安装；该工具对新下载不强制执行本地哈希比对，因此来源信任仍依赖 TLS、Google 仓库索引和安装后的代码签名，不能宣称 100% 安全。

## 官方依据

- [Android 下载页](https://developer.android.com/studio#command-line-tools-only)
- [Temurin 下载元数据](https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=aarch64&image_type=jdk&os=mac)
- [Gradle Java 兼容矩阵](https://docs.gradle.org/current/userguide/compatibility.html)
- [SDK Manager 文档](https://developer.android.com/tools/sdkmanager)
- [Android 环境变量](https://developer.android.com/tools/variables)
