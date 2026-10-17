package me.mykindos.betterpvp.core.command.brigadier.arguments;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.BooleanArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.CustomEffectArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.CustomItemArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.DurationArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.UUIDItemArgumentType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDItem;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BPvPArgumentTypes {
    @Getter
    private static final List<BPvPArgumentType<?, ?>> argumentTypes = new ArrayList<>();

    private static final UUIDItemArgumentType UUIDITEM_ARGUMENT = (UUIDItemArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), UUIDItemArgumentType.class);
    private static final PlayerNameArgumentType PLAYER_NAME_ARGUMENT = (PlayerNameArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), PlayerNameArgumentType.class);
    private static final CustomEffectArgumentType CUSTOM_EFFECT_ARGUMENT = (CustomEffectArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), CustomEffectArgumentType.class);
    private static final BooleanArgumentType BOOLEAN_ARGUMENT = (BooleanArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), BooleanArgumentType.class);
    private static final CustomItemArgumentType CUSTOM_ITEM_ARGUMENT = (CustomItemArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), CustomItemArgumentType.class);
    private static final DurationArgumentType DURATION_ARGUMENT = (DurationArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), DurationArgumentType.class);

    /**
     * Loads this class, so every argument type registers while Core enables
     */
    public static void load() {
        log.info("Loaded {} brigadier argument types", argumentTypes.size()).submit();
    }

    public static BPvPArgumentType<?, ?> createArgumentType(BPvPPlugin plugin, Class<? extends BPvPArgumentType<?, ?>> clazz) {

        BPvPArgumentType<?, ?> argumentType = plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(argumentType);
        log.info("Added custom brigadier argument type: {}", argumentType.getName()).submit();
        argumentTypes.add(argumentType);
        return argumentType;
    }

    /**
     * Prompts the sender with a list of valid {@link UUID}'s. Guarantees the return value is a valid {@link UUIDItem}
     * <p>Casting class {@link UUIDItem}</p>
     * @return the {@link UUIDItemArgumentType}
     */
    public static UUIDItemArgumentType uuidItem() {
        return UUIDITEM_ARGUMENT;
    }

    /**
     * Ensures that the return value is a valid Minecraft Player name.
     * <p>Casting class {@link String}</p>
     * @return the {@link PlayerNameArgumentType}
     */
    public static PlayerNameArgumentType playerName() {
        return PLAYER_NAME_ARGUMENT;
    }

    /**
     * Suggest matching {@link EffectType}s, ensures return value is a valid {@link EffectType}
     * <p>Casting class {@link EffectType}</p>
     * @return the {@link CustomEffectArgumentType}
     */
    public static CustomEffectArgumentType customEffect() {
        return CUSTOM_EFFECT_ARGUMENT;
    }

    /**
     * Suggests {@code true} or {@code false}.
     * <p>Casting class {@link Boolean}</p>
     * @return the {@link BooleanArgumentType}
     * @see Boolean#getBoolean(String) 
     */
    public static BooleanArgumentType booleanType() {
        return BOOLEAN_ARGUMENT;
    }

    /**
     * Suggests {@link ItemRegistry#getItems()}  indentifiers}
     * <p>Casting class {@link BaseItem}</p>
     * @return the {@link CustomItemArgumentType}
     */
    public static CustomItemArgumentType customItemType() {
        return CUSTOM_ITEM_ARGUMENT;
    }

    /**
     * A length of time such as {@code 30m}, {@code 2d} or {@code perm}. Offers no suggestions, so the client shows the
     * argument name as a hint.
     * <p>Casting class {@link Long}, in milliseconds, with {@code perm} as -1</p>
     * @return the {@link DurationArgumentType}
     */
    public static DurationArgumentType duration() {
        return DURATION_ARGUMENT;
    }
}
