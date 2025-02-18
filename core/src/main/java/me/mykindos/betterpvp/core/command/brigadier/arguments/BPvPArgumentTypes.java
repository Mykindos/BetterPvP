package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.UUIDItemArgumentType;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;

import java.util.ArrayList;
import java.util.List;


@CustomLog
@Singleton
public class BPvPArgumentTypes {
    //TODO UUIDItems, Clans, CustomItems, CustomEffects
    @Getter
    private final static List<BPvPArgumentType<?, ?>> argumentTypes = new ArrayList<>();

    public static UUIDItemArgumentType UUIDItem;
    public static PlayerNameArgumentType PlayerName;

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
}
