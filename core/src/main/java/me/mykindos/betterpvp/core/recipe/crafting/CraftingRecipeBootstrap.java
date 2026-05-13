package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Triggers {@link CraftingRecipeRegistry#registerMinecraftDefaults()} on {@link ServerLoadEvent},
 * after every plugin has finished its enable cycle. Running earlier would convert vanilla recipes
 * before each module's {@code ItemLoader.load(...)} has registered its
 * {@link me.mykindos.betterpvp.core.item.FallbackItem}-annotated items, so ingredient sets would
 * pin to transient {@code VanillaItem} instances and recipes like minecraft:torch would silently
 * produce no output at craft time.
 */
@BPvPListener
@Singleton
public class CraftingRecipeBootstrap implements Listener {

    private final CraftingRecipeRegistry registry;

    @Inject
    private CraftingRecipeBootstrap(CraftingRecipeRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        registry.registerMinecraftDefaults();
    }
}
