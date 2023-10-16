package me.mykindos.betterpvp.progression.progression.perks;


import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import me.mykindos.betterpvp.progression.tree.fishing.event.PlayerStopFishingEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

@BPvPListener
@Singleton
@Slf4j
public class DropMultiplierFishingPerk implements Listener, ProgressionPerk {

    private Progression progression;


    private Fishing fishing;

    @Override
    public String getName() {
        return "Drop Multiplier Fishing";
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Fishing.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCatch(PlayerStopFishingEvent event) {
        if (event.getReason() != PlayerStopFishingEvent.FishingResult.CATCH) return;
        int multiplier = 5;
        PlayerInventory inventory = event.getPlayer().getInventory();
        for (int i = 1; i < 5; i++) {
            inventory.addItem(event.getLoot().processCatch(event.getPlayerFishEvent()));
        }
    }
}