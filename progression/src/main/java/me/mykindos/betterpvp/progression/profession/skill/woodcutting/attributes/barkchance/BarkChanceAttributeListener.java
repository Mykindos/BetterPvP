package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.barkchance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerStripLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.item.TreeBark;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@BPvPListener
@Singleton
public class BarkChanceAttributeListener implements Listener {

    private final BarkChanceAttribute barkChanceAttribute;
    private final BlockTagManager blockTagManager;
    private final ItemFactory itemFactory;
    private final TreeBark treeBark;

    @Inject
    public BarkChanceAttributeListener(BarkChanceAttribute barkChanceAttribute, BlockTagManager blockTagManager,
                                       ItemFactory itemFactory, TreeBark treeBark) {
        this.barkChanceAttribute = barkChanceAttribute;
        this.blockTagManager = blockTagManager;
        this.itemFactory = itemFactory;
        this.treeBark = treeBark;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerStripLog(PlayerStripLogEvent event) {
        if (event.wasEventDeniedAndCancelled()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.ADVENTURE) return;
        if (!player.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) return;

        Block block = event.getStrippedLog();
        if (blockTagManager.isPlayerPlaced(block)) return;

        double chance = barkChanceAttribute.getChance(player);
        if (chance <= 0) return;
        if (Math.random() >= chance) return;

        ItemStack itemStack = itemFactory.create(treeBark).createItemStack();
        UtilServer.callEvent(new BarkDropEvent(player, itemStack));

        Location location = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1));
        Item item = player.getWorld().dropItem(location, itemStack);
        item.setVelocity(player.getLocation().getDirection().multiply(-0.2));
        UtilItem.reserveItem(item, player, 10);
    }

    @Getter
    public static class BarkDropEvent extends CustomEvent {

        private static final HandlerList HANDLERS = new HandlerList();

        private final Player player;
        private final ItemStack itemStack;

        public BarkDropEvent(Player player, ItemStack itemStack) {
            this.player = player;
            this.itemStack = itemStack;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
