package kuku;

import kuku.command.*;
import kuku.command.TreeAutoCommand;
import kuku.config.*;
import kuku.home.HomeManager;
import kuku.lang.LanguageManager;
import kuku.lang.PlayerLanguageManager;
import kuku.rightclick.RightClickHarvest;
import kuku.tree.TreeAuto;
import kuku.util.MessageDisplayManager;
import kuku.warp.WarpManager;
import kuku.back.BackManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Function implements ModInitializer {
	public static final String MOD_ID = "function";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Function Mod initializing...");
		MessageDisplayManager.initStorage();
		TreeAuto.register();
		RightClickHarvest.register();

		// 註冊命令（Home, TPA, Warp, Back）與控制台重載
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LanguageManager.load();
			HomeCommand.register(dispatcher);
			TpaCommand.register(dispatcher);
			WarpCommand.register(dispatcher);
			BackCommand.register(dispatcher);
			LangCommand.register(dispatcher);
			TreeAutoCommand.register(dispatcher);

			dispatcher.register(Commands.literal("function")
					.then(Commands.literal("reload")
							.executes(ctx -> {
								if (ctx.getSource().getEntity() != null) {
									ctx.getSource().sendFailure(Component.literal("[Function] ")
											.withStyle(ChatFormatting.GOLD)
											.append(LanguageManager.component("function.error.console_only", null)
													.withStyle(ChatFormatting.RED)));
									return 0;
								}

								// 重載各個模組配置
								HomeConfig newHome = HomeConfig.load();
								HomeConfig.getInstance().setEnabled(newHome.isEnabled());
								HomeConfig.getInstance().setMaxHomes(newHome.getMaxHomes());
								HomeConfig.getInstance().setDefaultHomeName(newHome.getDefaultHomeName());

								TpaConfig newTpa = TpaConfig.load();
								TpaConfig.getInstance().setEnabled(newTpa.isEnabled());
								TpaConfig.getInstance().setTimeout(newTpa.getTimeout());

								WarpConfig newWarp = WarpConfig.load();
								WarpConfig.getInstance().setEnabled(newWarp.isEnabled());
								WarpConfig.getInstance().setMaxWarps(newWarp.getMaxWarps());

								BackConfig newBack = BackConfig.load();
								BackConfig.getInstance().setEnabled(newBack.isEnabled());

								RightClickConfig newRightClick = RightClickConfig.load();
								RightClickConfig.getInstance().setEnabled(newRightClick.isEnabled());

								ctx.getSource().sendSuccess(() -> Component.literal("[Function] ")
										.withStyle(ChatFormatting.GOLD)
										.append(LanguageManager.component("function.success.reload", null)
												.withStyle(ChatFormatting.GREEN)), true);
								return 1;
							})
					)
			);

			dispatcher.register(Commands.literal("chat")
					.then(Commands.literal("action")
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								MessageDisplayManager.setMode(player.getUUID(),
										MessageDisplayManager.DisplayMode.ACTION_BAR);
								String msg = LanguageManager.translate("chat.set.action", player);
								ctx.getSource().sendSuccess(() ->
												Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
														.append(Component.literal(msg).withStyle(ChatFormatting.GREEN)),
										false);
								return 1;
							})
					)
					.then(Commands.literal("chatscreen")
							.executes(ctx -> {
								ServerPlayer player = ctx.getSource().getPlayerOrException();
								MessageDisplayManager.setMode(player.getUUID(),
										MessageDisplayManager.DisplayMode.CHAT);
								String msg = LanguageManager.translate("chat.set.chatscreen", player);
								ctx.getSource().sendSuccess(() ->
												Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
														.append(Component.literal(msg).withStyle(ChatFormatting.GREEN)),
										false);
								return 1;
							})
					)
			);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.getPlayer().getUUID();
			BackManager.removeAll(playerId);
			TreeAuto.removePlayer(playerId);
		});

		// 監聽玩家死亡，記錄死亡位置
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				BackManager.recordDeath(player);
			}
		});

		// 伺服器啟動完成後載入數據
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
			HomeManager.load(configDir);
			WarpManager.load(configDir);
			PlayerLanguageManager.load(configDir);
			MessageDisplayManager.load();
			LOGGER.info("Loaded all mod data and configs.");
		});

		// 伺服器關閉前保存數據
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			var configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
			HomeManager.save(configDir);
			WarpManager.save(configDir);
			PlayerLanguageManager.save(configDir);
			MessageDisplayManager.save();
			LOGGER.info("Saved home & warp data.");
		});

	}
}