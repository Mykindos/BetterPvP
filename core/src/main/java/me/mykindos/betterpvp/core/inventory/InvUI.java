// Credits to https://github.com/NichtStudioCode/InvUI
// License: https://github.com/NichtStudioCode/InvUI?tab=MIT-1-ov-file#readme
package me.mykindos.betterpvp.core.inventory;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry.PAPER_PLUGIN_CLASS_LOADER_CLASS;
import static me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry.PAPER_PLUGIN_CLASS_LOADER_GET_LOADED_JAVA_PLUGIN_METHOD;
import static me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry.PLUGIN_CLASS_LOADER_CLASS;
import static me.mykindos.betterpvp.core.inventory.inventoryaccess.util.ReflectionRegistry.PLUGIN_CLASS_LOADER_PLUGIN_FIELD;

@Singleton
@CustomLog
public class InvUI implements Listener {
    
    private static InvUI instance;
    
    private final List<Runnable> disableHandlers = new ArrayList<>();
    private Plugin plugin;
    
    private InvUI() {
    }
    
    public static @NotNull InvUI getInstance() {
        if(instance == null) {
            instance = new InvUI();
        }

        return instance;
    }
    
    public @NotNull Plugin getPlugin() {
        if (plugin == null) {
            setPlugin(tryFindPlugin());
            
            if (plugin == null)
                throw new IllegalStateException("Plugin is not set. Set it using InvUI.getInstance().setPlugin(plugin);");
        }
        
        return plugin;
    }
    
    private @Nullable Plugin tryFindPlugin() {
        ClassLoader loader = getClass().getClassLoader();
        
        try {
            if (PLUGIN_CLASS_LOADER_CLASS.isInstance(loader)) {
                return ReflectionUtils.getFieldValue(PLUGIN_CLASS_LOADER_PLUGIN_FIELD, loader);
            } else if (PAPER_PLUGIN_CLASS_LOADER_CLASS != null && PAPER_PLUGIN_CLASS_LOADER_CLASS.isInstance(loader)) {
                return ReflectionUtils.invokeMethod(PAPER_PLUGIN_CLASS_LOADER_GET_LOADED_JAVA_PLUGIN_METHOD, loader);
            }
        } catch (Exception ex) {
            log.error("Failed to find plugin for InvUI", ex).submit();
        }
        
        return null;
    }
    
    public void setPlugin(@Nullable Plugin plugin) {
        if (this.plugin != null)
            throw new IllegalStateException("Plugin is already set");
        
        if (plugin == null)
            return;
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }
    
    public @NotNull Logger getLogger() {
        return getPlugin().getLogger();
    }
    
    public void addDisableHandler(@NotNull Runnable runnable) {
        disableHandlers.add(runnable);
    }
    
    @EventHandler
    private void handlePluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            disableHandlers.forEach(Runnable::run);
        }
    }
    
}
