package kuku.dli;

import kuku.lang.LanguageManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DliSetup {
    private record Attempt(ItemStack stack, long timestamp) {}
    private static final Map<UUID, Attempt> DROP_ATTEMPTS = new HashMap<>();
    private static final Map<UUID, Attempt> PLACE_ATTEMPTS = new HashMap<>();
    private static final long TIMEOUT_MS = 30_000;

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!DliSettings.isEnabled(sp.getUUID())) return InteractionResult.PASS;

            if (stack.has(DataComponents.CUSTOM_NAME) && stack.getItem() instanceof BlockItem) {
                UUID uid = sp.getUUID();
                long now = System.currentTimeMillis();

                // 清理過期條目
                cleanExpired(PLACE_ATTEMPTS, now);

                Attempt last = PLACE_ATTEMPTS.get(uid);
                if (last != null && now - last.timestamp <= TIMEOUT_MS && ItemStack.matches(last.stack, stack)) {
                    PLACE_ATTEMPTS.remove(uid);
                    return InteractionResult.PASS;
                } else {
                    PLACE_ATTEMPTS.put(uid, new Attempt(stack.copy(), now));
                    sendConfirmMessage(sp, "dli.confirm.place", stack);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean onDropItem(ServerPlayer player, ItemStack stack) {
        if (!DliSettings.isEnabled(player.getUUID())) return true;
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            UUID uid = player.getUUID();
            long now = System.currentTimeMillis();

            // 清理過期條目
            cleanExpired(DROP_ATTEMPTS, now);

            Attempt last = DROP_ATTEMPTS.get(uid);
            if (last != null && now - last.timestamp <= TIMEOUT_MS && ItemStack.matches(last.stack, stack)) {
                DROP_ATTEMPTS.remove(uid);
                return true;
            } else {
                DROP_ATTEMPTS.put(uid, new Attempt(stack.copy(), now));
                sendConfirmMessage(player, "dli.confirm.drop", stack);
                return false;
            }
        }
        return true;
    }

    private static void cleanExpired(Map<UUID, Attempt> map, long now) {
        Iterator<Map.Entry<UUID, Attempt>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Attempt> entry = it.next();
            if (now - entry.getValue().timestamp > TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    private static void sendConfirmMessage(ServerPlayer player, String langKey, ItemStack stack) {
        String itemName = stack.getHoverName().getString();
        String confirmText = LanguageManager.translate(langKey, player, itemName);
        MutableComponent msg = LanguageManager.component("prefix.function.generic", player)
                .append(Component.literal(confirmText));
        player.sendSystemMessage(msg);
    }
}