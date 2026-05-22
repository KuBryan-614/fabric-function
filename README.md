# Function

Function is a Minecraft Java Fabric server-side utility mod that combines teleportation, quality-of-life commands, container tweaks, item protection, crop/tree helpers, and small administration tools.

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2` or newer
- Java: `25` or newer
- Fabric API: required

## 繁體中文

### 簡介

Function 是一個 Fabric 伺服器功能模組，集中提供傳送、作物與樹木輔助、容器便利功能、物品保護、特殊命名、資訊查詢與管理工具。

### 安裝需求

- Minecraft Java `26.1.2`
- Fabric Loader `0.19.2+`
- Java `25+`
- Fabric API

將 `function-<version>.jar` 放入伺服器的 `mods` 資料夾後啟動伺服器。

### 主要功能

- Home：設定、刪除、列出與傳送到個人 home。
- TPA：玩家之間傳送請求、邀請對方傳送到自己身邊、接受或拒絕請求。
- Warp：公開傳送點、GUI 列表、改名、刪除與圖示修改。
- Back：返回上一次傳送前位置或死亡位置。
- 容器便利：箱子、木桶、界伏盒上方有方塊時仍可開啟。
- 容器命名：蹲下並手持命名牌右鍵容器，可設定或清除容器名稱。
- 作物輔助：右鍵收穫成熟作物並自動重種。
- 樹木輔助：砍完樹後可自動補種樹苗。
- 半磚挖掘：蹲下挖掘雙半磚時只移除一半。
- 特殊命名牌：使用關鍵字控制生物行為，例如無敵、不死亡、靜音、不移動等。
- 物品保護：命名物品丟棄或放置時需要二次確認。
- 生物保護：可切換村民、貓、狼的攻擊保護。
- 原木剝皮保護：可切換防止誤剝原木。
- 詛咒處理：移除手持物品詛咒，或脫下綁定詛咒裝備。
- 資訊工具：TPS、MSPT、延遲、座標換算、鐵砧懲罰查詢。
- 語言系統：依玩家客戶端語言或 `/lang` 設定顯示訊息。
- 自動更新檢查：伺服器啟動後檢查 GitHub Release 是否有新版，下載後於下次重啟使用。

### 指令

| 指令 | 說明 |
| --- | --- |
| `/sethome <name>` | 設定 home |
| `/home [name]` | 傳送到 home |
| `/homes` | 列出 home |
| `/delhome <name>` | 刪除 home |
| `/tpa <player>` | 請求傳送到玩家身邊 |
| `/tpahere <player>` | 請求玩家傳送到你身邊 |
| `/tpaccept` | 接受傳送請求 |
| `/tpadeny` | 拒絕傳送請求 |
| `/setwarp <name>` | 建立公開 warp |
| `/warp <name>` | 傳送到 warp |
| `/warps` | 開啟 warp GUI |
| `/delwarp <name>` | 刪除自己的 warp |
| `/renamewarp <old> <new>` | 重新命名 warp |
| `/back` | 返回上一個位置 |
| `/treeauto <on\|off>` | 切換自動補種樹苗 |
| `/dli` | 切換命名物品丟棄/放置確認 |
| `/ptv` | 切換村民、貓、狼攻擊保護 |
| `/blp peeler` | 切換原木剝皮保護 |
| `/demagic` | 移除手持物品的詛咒附魔 |
| `/removebinding <all\|head\|chest\|legs\|feet>` | 脫下綁定詛咒裝備 |
| `/name` | 顯示特殊命名牌規則 |
| `/mods` | 顯示模組功能清單 |
| `/tps` | 顯示伺服器 TPS |
| `/mspt` | 顯示伺服器 MSPT |
| `/ping` | 顯示玩家延遲 |
| `/nc [x] [z]` | 主世界與地獄座標換算 |
| `/rct` | 顯示手持物品鐵砧懲罰 |
| `/chat action` | 將訊息顯示改為快捷欄 |
| `/chat chatscreen` | 將訊息顯示改為聊天欄 |
| `/lang [language]` | 查看或設定語言 |
| `/debug function` | 取得 Function 除錯棒 |
| `/function reload` | 從控制台重新載入設定 |

### 設定與資料

模組資料與設定會儲存在伺服器 `config/function` 目錄中。  
玩家 home、warp、語言偏好與訊息顯示偏好會在伺服器停止時保存。

### 從原始碼建置

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

建置完成後，輸出檔案位於 `build/libs/`。

## 简体中文

### 简介

Function 是一个 Fabric 服务器功能模组，集中提供传送、作物与树木辅助、容器便利功能、物品保护、特殊命名、信息查询与管理工具。

### 安装需求

- Minecraft Java `26.1.2`
- Fabric Loader `0.19.2+`
- Java `25+`
- Fabric API

将 `function-<version>.jar` 放入服务器的 `mods` 文件夹后启动服务器。

### 主要功能

- Home：设置、删除、列出并传送到个人 home。
- TPA：玩家之间发送传送请求、邀请对方传送到自己身边、接受或拒绝请求。
- Warp：公开传送点、GUI 列表、改名、删除与图标修改。
- Back：返回上一次传送前位置或死亡位置。
- 容器便利：箱子、木桶、潜影盒上方有方块时仍可打开。
- 容器命名：蹲下并手持命名牌右键容器，可设置或清除容器名称。
- 作物辅助：右键收获成熟作物并自动补种。
- 树木辅助：砍完树后可自动补种树苗。
- 台阶挖掘：蹲下挖掘双台阶时只移除一半。
- 特殊命名牌：使用关键词控制生物行为，例如无敌、不死亡、静音、不移动等。
- 物品保护：命名物品丢弃或放置时需要二次确认。
- 生物保护：可切换村民、猫、狼的攻击保护。
- 原木剥皮保护：可切换防止误剥原木。
- 诅咒处理：移除手持物品诅咒，或脱下绑定诅咒装备。
- 信息工具：TPS、MSPT、延迟、坐标换算、铁砧惩罚查询。
- 语言系统：根据玩家客户端语言或 `/lang` 设置显示消息。
- 自动更新检查：服务器启动后检查 GitHub Release 是否有新版，下载后在下次重启时使用。

### 指令

| 指令 | 说明 |
| --- | --- |
| `/sethome <name>` | 设置 home |
| `/home [name]` | 传送到 home |
| `/homes` | 列出 home |
| `/delhome <name>` | 删除 home |
| `/tpa <player>` | 请求传送到玩家身边 |
| `/tpahere <player>` | 请求玩家传送到你身边 |
| `/tpaccept` | 接受传送请求 |
| `/tpadeny` | 拒绝传送请求 |
| `/setwarp <name>` | 创建公开 warp |
| `/warp <name>` | 传送到 warp |
| `/warps` | 打开 warp GUI |
| `/delwarp <name>` | 删除自己的 warp |
| `/renamewarp <old> <new>` | 重命名 warp |
| `/back` | 返回上一个位置 |
| `/treeauto <on\|off>` | 切换自动补种树苗 |
| `/dli` | 切换命名物品丢弃/放置确认 |
| `/ptv` | 切换村民、猫、狼攻击保护 |
| `/blp peeler` | 切换原木剥皮保护 |
| `/demagic` | 移除手持物品的诅咒附魔 |
| `/removebinding <all\|head\|chest\|legs\|feet>` | 脱下绑定诅咒装备 |
| `/name` | 显示特殊命名牌规则 |
| `/mods` | 显示模组功能清单 |
| `/tps` | 显示服务器 TPS |
| `/mspt` | 显示服务器 MSPT |
| `/ping` | 显示玩家延迟 |
| `/nc [x] [z]` | 主世界与下界坐标换算 |
| `/rct` | 显示手持物品铁砧惩罚 |
| `/chat action` | 将消息显示改为快捷栏 |
| `/chat chatscreen` | 将消息显示改为聊天栏 |
| `/lang [language]` | 查看或设置语言 |
| `/debug function` | 获取 Function 调试棒 |
| `/function reload` | 从控制台重新加载配置 |

### 设置与数据

模组数据与设置会保存在服务器 `config/function` 目录中。  
玩家 home、warp、语言偏好与消息显示偏好会在服务器停止时保存。

### 从源码构建

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

构建完成后，输出文件位于 `build/libs/`。

## English

### Overview

Function is a Fabric server utility mod that combines teleportation, crop and tree helpers, container conveniences, item protection, special name-tag behavior, information commands, and small administration tools.

### Requirements

- Minecraft Java `26.1.2`
- Fabric Loader `0.19.2+`
- Java `25+`
- Fabric API

Place `function-<version>.jar` in the server `mods` folder, then start the server.

### Features

- Home: set, delete, list, and teleport to personal homes.
- TPA: send teleport requests, request another player to teleport to you, accept or deny requests.
- Warp: public warp points, GUI list, rename, delete, and icon editing.
- Back: return to the previous teleport location or death location.
- Container convenience: open chests, barrels, and shulker boxes even when blocked above.
- Container naming: sneak and right-click containers with a name tag to set or clear names.
- Crop helper: right-click mature crops to harvest and replant.
- Tree helper: automatically replant saplings after trees are fully cut.
- Slab mining: sneak while mining a double slab to remove only half.
- Special name tags: control mob behavior with keywords such as invincible, cannot die, silent, or no movement.
- Item protection: require confirmation before dropping or placing named items.
- Mob protection: toggle protection for villagers, cats, and wolves.
- Log peeling protection: toggle protection against accidental log stripping.
- Curse tools: remove curse enchantments from held items or remove Curse of Binding equipment.
- Information tools: TPS, MSPT, latency, coordinate conversion, and repair-cost queries.
- Language system: messages follow the client language or `/lang` preference.
- Update check: checks GitHub Releases on server startup and prepares a newer jar for the next restart.

### Commands

| Command | Description |
| --- | --- |
| `/sethome <name>` | Set a home |
| `/home [name]` | Teleport to a home |
| `/homes` | List homes |
| `/delhome <name>` | Delete a home |
| `/tpa <player>` | Request to teleport to another player |
| `/tpahere <player>` | Request another player to teleport to you |
| `/tpaccept` | Accept a teleport request |
| `/tpadeny` | Deny a teleport request |
| `/setwarp <name>` | Create a public warp |
| `/warp <name>` | Teleport to a warp |
| `/warps` | Open the warp GUI |
| `/delwarp <name>` | Delete your own warp |
| `/renamewarp <old> <new>` | Rename a warp |
| `/back` | Return to the previous location |
| `/treeauto <on\|off>` | Toggle automatic tree replanting |
| `/dli` | Toggle named-item drop/place confirmation |
| `/ptv` | Toggle villager, cat, and wolf attack protection |
| `/blp peeler` | Toggle log peeling protection |
| `/demagic` | Remove curse enchantments from the held item |
| `/removebinding <all\|head\|chest\|legs\|feet>` | Remove Curse of Binding equipment |
| `/name` | Show special name-tag rules |
| `/mods` | Show the mod feature list |
| `/tps` | Show server TPS |
| `/mspt` | Show server MSPT |
| `/ping` | Show player latency |
| `/nc [x] [z]` | Convert overworld and nether coordinates |
| `/rct` | Show held-item anvil repair cost |
| `/chat action` | Show messages in the action bar |
| `/chat chatscreen` | Show messages in chat |
| `/lang [language]` | View or set language |
| `/debug function` | Get the Function debug stick |
| `/function reload` | Reload Function configs from console |

### Config And Data

Configuration and data are stored under the server `config/function` directory.  
Homes, warps, player language preferences, and message display preferences are saved when the server stops.

### Build From Source

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Build outputs are generated in `build/libs/`.
