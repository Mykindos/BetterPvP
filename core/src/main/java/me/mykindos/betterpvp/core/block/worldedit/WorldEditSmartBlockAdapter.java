package me.mykindos.betterpvp.core.block.worldedit;

import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.bukkit.World;

@PluginAdapter("WorldEdit")
public class WorldEditSmartBlockAdapter {

    private final SmartBlockFactory factory;

    @Inject
    private WorldEditSmartBlockAdapter(SmartBlockFactory factory) {
        this.factory = factory;
        WorldEdit.getInstance().getEventBus().register(this);
    }

    // This is a hook to disable smart blocks changes in WorldEdit.
    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        World world = BukkitAdapter.adapt(event.getWorld());
        event.setExtent(new SmartBlockExtent(event.getExtent(), world, factory));
    }

}
