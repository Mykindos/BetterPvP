package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.BooleanArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.CustomEffectArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.UUIDItemArgumentType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@CustomLog
@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BPvPArgumentTypes {
    //TODO UUIDItems, Clans, CustomItems, CustomEffects
    @Getter
    private final static List<BPvPArgumentType<?, ?>> argumentTypes = new ArrayList<>();

    private static final UUIDItemArgumentType UUIDItem = (UUIDItemArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), UUIDItemArgumentType.class);
    private static final PlayerNameArgumentType PlayerName = (PlayerNameArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), PlayerNameArgumentType.class);
    private static final CustomEffectArgumentType EFFECT_TYPE = (CustomEffectArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), CustomEffectArgumentType.class);
    private static final BooleanArgumentType BOOLEAN_TYPE = (BooleanArgumentType) createArgumentType(JavaPlugin.getPlugin(Core.class), BooleanArgumentType.class);

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
        return UUIDItem;
    }

    /**
     * Ensures that the return value is a valid Minecraft Player name.
     * <p>Casting class {@link String}</p>
     * @return the {@link PlayerNameArgumentType}
     */
    public static PlayerNameArgumentType playerName() {
        return PlayerName;
    };

    /**
     * Suggest matching {@link EffectType}s, ensures return value is a valid {@link EffectType}
     * <p>Casting class {@link EffectType}</p>
     * @return the {@link CustomEffectArgumentType}
     */
    public static CustomEffectArgumentType customEffect() {
        return EFFECT_TYPE;
    }

    /**
     * Suggests {@code true} or {@code false}.
     * <p>Casting class {@link Boolean}</p>
     * @return the {@link BooleanArgumentType}
     * @see Boolean#getBoolean(String) 
     */
    public static BooleanArgumentType booleanType() {
        return BOOLEAN_TYPE;
    }
}
