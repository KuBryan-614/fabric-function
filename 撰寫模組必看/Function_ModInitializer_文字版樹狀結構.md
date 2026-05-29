# kuku.Function (ModInitializer) 文字版樹狀結構

## Init Features

`onInitialize()` 直接初始化或註冊的功能：

```text
kuku.Function
├── MessageDisplayManager.initStorage() — 建立訊息顯示模式儲存空間
├── TreeAuto.register()                 — 自動種樹/玩家斷線清除
├── RightClickHarvest.register()        — 右鍵收穫成熟作物
├── EasyBoxHandler.register()           — 手持界伏盒對空氣右鍵開啟
├── ChestNamingSetup.init()             — 蹲下手持命名牌右鍵容器命名/清除名稱
├── NameFeature.init()                  — 實體特殊命名功能
├── DebugStickActions.init()            — Function 除錯棒功能
├── TickTracker.register()              — TPS/MSPT 計時
├── SlabMiningHandler.register()        — 蹲下挖雙半磚時只挖掉一半
├── RecipeChecker.init()                — 配方錯誤檢查
├── DliSetup.init()                     — 命名物品丟棄/放置確認
├── PTVHandler.init()                   — 村民、貓、狼攻擊保護
└── BLPHandler.init()                   — 原木剝皮保護
```

`UpdateChecker.checkForUpdate()` 不在 init 直接執行，而是在 `SERVER_STARTED` 事件中執行。

## Commands

透過 `CommandRegistrationCallback.EVENT` 註冊：

```text
Commands
├── HomeCommand             — /home, /sethome, /delhome, /homes
├── TpaCommand              — /tpa, /tpahere, /tpaccept, /tpadeny
├── WarpCommand             — /warp, /setwarp, /delwarp, /renamewarp, /warps
├── BackCommand             — /back 返回死亡點或傳送前位置
├── LangCommand             — /lang 查看或切換語言
├── TreeAutoCommand         — /treeauto on|off
├── RemoveBindingCommand    — /removebinding 脫下綁定詛咒裝備
├── NameFeature.registerCommand()
├── ModslashCommands        — /mods, /tps, /mspt, /ping, /nc, /rct
├── DebugStickCommand       — /debug function
├── DliCommand              — /dli
├── PTVCommand              — /ptv
├── BLPCommand              — /blp
├── DemagicCommand          — /demagic 移除手上物品的綁定/消失詛咒
├── /function reload        — 僅主控台可用，重載部分 config
└── /chat action|chatscreen — 切換訊息顯示位置
```

`/function reload` 目前只重載：

```text
HomeConfig
TpaConfig
WarpConfig
BackConfig
RightClickConfig
```

新增 config 時，如果需要支援熱重載，必須同步加入 `/function reload`。

## Data / Config

```text
Data / Config
├── HomeConfig + HomeManager          — 家的上限、預設名稱、存/載 JSON
├── TpaConfig + TpaManager            — TPA timeout 與請求狀態
├── WarpConfig + WarpManager          — 公共傳送點上限、存/載 JSON
├── BackConfig + BackManager          — 死亡/傳送記錄，玩家斷線清除
├── RightClickConfig                  — 右鍵收穫開關
├── LanguageManager                   — 語言包載入與翻譯
├── PlayerLanguageManager             — 玩家語言偏好，存/載 JSON
├── MessageDisplayManager             — ActionBar / Chat 顯示模式，存/載 JSON
├── RenameSessionManager              — Warp 重命名聊天攔截 session
├── DliSettings + DliSetup            — DLI 玩家狀態，斷線清除
├── PTVHandler + PTVSettings          — PTV 玩家狀態，斷線清除
└── BLPHandler + BLPSettings          — BLP 玩家狀態，斷線清除
```

## Mixins / AccessWidener

`src/main/resources/function.mixins.json`：

```text
kuku.mixin
├── chest.ChestBlockMixin
├── chest.ShulkerBoxBlockMixin
├── name.chestname.BaseContainerBlockEntityAccessor
├── name.WitherBossAccessor
├── name.LivingEntityMixin
├── name.EntityTagsAccessor
├── debug.ServerPlayerGameModeMixin
├── debug.IngredientMixin
├── modslash.ServerCommonPacketListenerImplAccessor
├── axolotl.AxolotlItemMixin
└── dli.ServerPlayerMixin
```

`src/client/resources/function.client.mixins.json`：

```text
kuku.client.mixin
└── ExampleClientMixin
```

`src/main/resources/function.accesswidener`：

```text
AxeItem#STRIPPABLES — accessible field
```

Mixin 注意事項：

- 每次 Minecraft 版本或 mappings 更新後，要重新確認目標 class、method descriptor、field 名稱。
- 不要只依賴 `build`，有 mixin 修改時建議跑 `clean build`，必要時再跑 `runClient`。
- 如果錯誤訊息提到 `could not find any targets matching`，優先檢查目標方法是否已改名、移動或簽名變更。

## Lifecycle Events

```text
Fabric API Events
├── SERVER_STARTED
│   ├── UpdateChecker.checkForUpdate()
│   ├── HomeManager.load(config/function)
│   ├── WarpManager.load(config/function)
│   ├── PlayerLanguageManager.load(config/function)
│   └── MessageDisplayManager.load()
├── SERVER_STOPPING
│   ├── HomeManager.save(config/function)
│   ├── WarpManager.save(config/function)
│   ├── PlayerLanguageManager.save(config/function)
│   ├── MessageDisplayManager.save()
│   └── RenameSessionManager.shutdown()
├── DISCONNECT #1
│   ├── BackManager.removeAll(playerId)
│   └── TreeAuto.removePlayer(playerId)
├── DISCONNECT #2
│   ├── PTVSettings.remove(playerId)
│   ├── DliSettings.remove(playerId)
│   └── BLPSettings.remove(playerId)
├── AFTER_DEATH
│   └── BackManager.recordDeath(player)
└── ALLOW_CHAT_MESSAGE
    └── RenameSessionManager.handleChat(player, content)
```

## 新增功能時必看清單

- 需要啟動時註冊：在 `Function#onInitialize()` 加 register/init。
- 需要指令：在 `CommandRegistrationCallback.EVENT` 裡註冊。
- 需要 config：建立 config class，決定是否加入 `/function reload`。
- 需要存檔：加入 `SERVER_STARTED` load 與 `SERVER_STOPPING` save。
- 需要玩家離線清除狀態：加入 `ServerPlayConnectionEvents.DISCONNECT`。
- 需要翻譯：同步更新 `zh_tw.json`、`zh_cn.json`、`en_us.json`。
- 需要 `/mods` 說明：同步更新三個語言檔的 `modslash.mods.help`。
- 需要 mixin：加入 `function.mixins.json` 或 `function.client.mixins.json`，並確認目標方法簽名。
- 需要 access widener：更新 `function.accesswidener`，並確認 `build.gradle` 已指向該檔。
- 需要 README 對外說明：同步更新 `README.md`。
- 完成後至少跑 `.\gradlew.bat build`；改過 mixin 時建議跑 `.\gradlew.bat clean build`。
