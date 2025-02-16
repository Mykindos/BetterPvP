package me.mykindos.betterpvp.core.command.brigadier.arguments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;

import java.util.ArrayList;
import java.util.List;


@CustomLog
@Singleton
public class BPvPArgumentTypes {
    private static BPvPPlugin plugin;
    //TODO UUIDItems, Clans, CustomItems, CustomEffects
    @Getter
    private final static List<BPvPArgumentType<?, ?>> argumentTypes = new ArrayList<>();

    public static BPvPArgumentType<?, ?> UUIDItem;

    @Inject
    public BPvPArgumentTypes(Core plugin) {
        BPvPArgumentTypes.plugin = plugin;
        BPvPArgumentTypes.UUIDItem = createArgumentType(UUIDItemArgumentType.class);
    }


    public static BPvPArgumentType<?, ?> createArgumentType(Class<? extends BPvPArgumentType<?, ?>> clazz) {

        BPvPArgumentType<?, ?> argumentType = plugin.getInjector().getInstance(clazz);
        plugin.getInjector().injectMembers(argumentType);
        log.info("Added custom brigadier argument type: {}", argumentType.getName()).submit();
        argumentTypes.add(argumentType);
        return argumentType;
    }

}
