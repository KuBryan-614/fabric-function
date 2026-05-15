package kuku.name;

import com.mojang.brigadier.CommandDispatcher;
import kuku.lang.LanguageManager;
import kuku.mixin.name.WitherBossAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.lang.reflect.Field;
import java.util.*;

public class NameFeature {

    public static final String TAG_PREFIX = "kuku_";
    private static final Field tagsField;

    static {
        // 反射取得 Entity.tags 欄位，完全避免 Accessor 不穩定性
        try {
            tagsField = Entity.class.getDeclaredField("tags");
            tagsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("找不到 Entity.tags 欄位，請檢查 Minecraft 版本。", e);
        }
    }

    private static final LinkedHashMap<String, String> KEYWORDS = new LinkedHashMap<>();

    static {
        // 按長度降序，避免子字串誤判（例如 ned 包含 nd）
        KEYWORDS.put("silent", "silent");
        KEYWORDS.put("nmove", "nmove");
        KEYWORDS.put("nbr", "nbr");
        KEYWORDS.put("ned", "ned");
        KEYWORDS.put("nd", "nd");
    }

    public static void init() {
        UseEntityCallback.EVENT.register(NameFeature::onUseEntity);
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof LivingEntity living) {
                applyEffectsFromTags(living);
            }
        });
    }

    // 取得實體的標籤集合（反射）
    @SuppressWarnings("unchecked")
    private static Set<String> getTags(Entity entity) {
        try {
            return (Set<String>) tagsField.get(entity);
        } catch (IllegalAccessException e) {
            return Collections.emptySet();
        }
    }

    public static boolean hasTag(LivingEntity entity, String key) {
        return getTags(entity).contains(TAG_PREFIX + key);
    }

    // 使用命名牌右鍵生物
    private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (!(world instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.NAME_TAG)) return InteractionResult.PASS;

        Component tagName = held.get(DataComponents.CUSTOM_NAME);
        if (tagName == null) return InteractionResult.PASS;

        if (!(target instanceof LivingEntity living)) return InteractionResult.PASS;

        String nameStr = tagName.getString().trim();
        if (nameStr.isEmpty()) return InteractionResult.PASS;

        living.setCustomName(tagName);

        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        Set<String> tags = getTags(target);
        // 清除舊有 kuku_ 標籤
        tags.removeIf(tag -> tag.startsWith(TAG_PREFIX));

        // 添加新標籤
        String lowerName = nameStr.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : KEYWORDS.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                tags.add(TAG_PREFIX + entry.getValue());
            }
        }

        applyEffectsFromTags(living);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    private static void applyEffectsFromTags(LivingEntity entity) {
        Set<String> tags = getTags(entity);

        // silent
        entity.setSilent(tags.contains(TAG_PREFIX + "silent"));

        if (entity instanceof Mob mob) {
            // nmove
            mob.setNoAi(tags.contains(TAG_PREFIX + "nmove"));

            // ✅ 只要有任何 kuku_ 標籤，防止生物自然消散
            boolean hasAnyKukuTag = tags.stream().anyMatch(t -> t.startsWith(TAG_PREFIX));
            if (hasAnyKukuTag) {
                mob.setPersistenceRequired();
            }
        }

        // nbr
        if (entity instanceof WitherBoss wither) {
            if (tags.contains(TAG_PREFIX + "nbr")) {
                ServerBossEvent bossEvent = ((WitherBossAccessor) wither).getBossEvent();
                bossEvent.setVisible(false);
                bossEvent.setDarkenScreen(false);
            }
        }
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("name")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
                    String help = LanguageManager.translate("name.help", player);
                    prefix.append(Component.literal(help));
                    ctx.getSource().sendSuccess(() -> prefix, false);
                    return 1;
                })
        );
    }
}