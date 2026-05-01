package kuku;

import kuku.command.HomeCommand;
import kuku.config.HomeConfig;
import kuku.home.HomeManager;
import kuku.command.TpaCommand;
import kuku.config.TpaConfig;
import kuku.command.WarpCommand;
import kuku.config.WarpConfig;
import kuku.warp.WarpManager;
import kuku.command.BackCommand;          // 新增
import kuku.config.BackConfig;          // 新增
import kuku.back.BackManager;         // 新增（如果在主类用到）

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;  // 新增
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Function implements ModInitializer {
	public static final String MOD_ID = "function";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Function Mod initializing...");

		// 注册命令（Home, TPA, Warp, Back）与控制台重载
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			HomeCommand.register(dispatcher);
			TpaCommand.register(dispatcher);
			WarpCommand.register(dispatcher);
			BackCommand.register(dispatcher);   // 新增

			dispatcher.register(Commands.literal("function")
					.then(Commands.literal("reload")
							.executes(ctx -> {
								if (ctx.getSource().getEntity() != null) {
									ctx.getSource().sendFailure(Component.literal("[Function] ")
											.withStyle(ChatFormatting.GOLD)
											.append(Component.literal("此命令只能由控制台执行。").withStyle(ChatFormatting.RED)));
									return 0;
								}

								// 重载各个模块配置
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

								BackConfig newBack = BackConfig.load();        // 新增
								BackConfig.getInstance().setEnabled(newBack.isEnabled());

								ctx.getSource().sendSuccess(() -> Component.literal("[Function] ")
										.withStyle(ChatFormatting.GOLD)
										.append(Component.literal("配置已重新加载。").withStyle(ChatFormatting.GREEN)), true);
								return 1;
							})
					)
			);
		});

		// 监听玩家死亡，记录死亡位置
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				BackManager.record(player);
			}
		});

		// 服务器启动完成后加载数据
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
			HomeManager.load(configDir);
			WarpManager.load(configDir);
			LOGGER.info("Loaded all mod data and configs.");
		});

		// 服务器关闭前保存数据
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			var configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
			HomeManager.save(configDir);
			WarpManager.save(configDir);
			LOGGER.info("Saved home & warp data.");
		});
	}
}