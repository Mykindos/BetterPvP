package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.sapphire;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.UpdateMaxEnergyEvent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class SapphireGemHandler implements Listener {

    private final EnergyService energyService;
    private final ComponentLookupService componentLookupService;

    @Inject
    public SapphireGemHandler(EnergyService energyService, ComponentLookupService componentLookupService) {
        this.energyService = energyService;
        this.componentLookupService = componentLookupService;
    }

    @EventHandler
    public void onEnergyUpdate(UpdateMaxEnergyEvent event) {
        Player player = event.getPlayer();
        double bonus = 0;

        EntityEquipment equipment = player.getEquipment();
        for (ItemStack item : equipment.getArmorContents()) {
            bonus += getEnergyBonus(item);
        }

        if (bonus > 0) {
            event.setNewMax(event.getNewMax() + bonus);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEquip(EntityEquipmentChangedEvent event) {
        if (event.getEntity() instanceof Player player) {
            energyService.updateMax(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        energyService.updateMax(event.getPlayer());
    }

    private double getEnergyBonus(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        return getSocketableContainer(item)
                .map(container -> {
                    double totalBonus = 0;
                    for (Socketable socketable : container.getSocketables()) {
                        for (ItemStat<?> stat : socketable.getStats()) {
                            if (stat.getType().equals(StatTypes.ENERGY)) {
                                totalBonus += (Double) stat.getValue();
                            }
                        }
                    }
                    return totalBonus;
                })
                .orElse(0.0);
    }

    private Optional<SocketableContainerComponent> getSocketableContainer(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        return componentLookupService.getComponent(item, SocketableContainerComponent.class);
    }
}
