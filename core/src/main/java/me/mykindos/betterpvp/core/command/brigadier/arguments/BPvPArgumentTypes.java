package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.UUIDItemArgumentType;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@CustomLog
@Singleton
public class BPvPArgumentTypes {
    //TODO UUIDItems, Clans, CustomItems, CustomEffects
    @Getter
    private final static List<BPvPArgumentType<?, ?>> argumentTypes = new ArrayList<>();

    private static UUIDItemArgumentType UUIDItem;
    private static PlayerNameArgumentType PlayerName;

    @Inject
    public BPvPArgumentTypes(Core plugin) {
        BPvPArgumentTypes.UUIDItem = (UUIDItemArgumentType) createArgumentType(plugin, UUIDItemArgumentType.class);
        BPvPArgumentTypes.PlayerName = (PlayerNameArgumentType) createArgumentType(plugin, PlayerNameArgumentType.class);
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
     * @return the {@link UUIDItemArgumentType}
     */
    public static UUIDItemArgumentType UUIDItem() {
        return UUIDItem;
    }

    /**
     * Ensures that the return value is a valid Minecraft Player name.
     * @return the {@link PlayerNameArgumentType}
     */
    public static PlayerNameArgumentType playerName() {
        return PlayerName;
    };
}
