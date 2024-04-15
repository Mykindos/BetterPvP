package me.mykindos.betterpvp.clans.clans.insurance;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.List;

@BPvPListener
public class InsuranceListener extends ClanListener {

    @Inject
    @Config(path = "clans.insurance.durationInHours", defaultValue = "24")
    private int expiryTime;

    @Inject
    @Config(path = "clans.insurance.enabled", defaultValue = "true")
    private boolean enabled;

    @Inject
    @Config(path = "clans.insurance.nonRestorableBlocks", defaultValue = "TNT,ENCHANTING_TABLE,RED_BED")
    private List<String> nonRestorableBlocks;

    @Inject
    public InsuranceListener(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @UpdateEvent(delay = 38400, isAsync = true)
    public void cleanClanInsurance() {
        if (!enabled) return;
        long expiryTimeMs = (long) expiryTime * 60 * 60 * 1000;
        clanManager.getObjects().values().forEach(clan -> {
            clan.getInsurance().removeIf(insurance -> UtilTime.elapsed(insurance.getTime(), expiryTimeMs));
        });

        clanManager.getRepository().deleteExpiredInsurance(expiryTimeMs);
    }

    @UpdateEvent
    public void processInsuranceQueue() {
        if (!enabled) return;
        if (clanManager.getInsuranceQueue().isEmpty()) return;

        for (int i = 0; i < 3; i++) {
            Insurance insurance = clanManager.getInsuranceQueue().poll();
            if (insurance == null) continue;
            if (UtilTime.elapsed(insurance.getTime(),  (long) expiryTime * 60 * 60 * 1000)) continue;

            Location blockLocation = insurance.getBlockLocation();

            if (insurance.getInsuranceType() == InsuranceType.PLACE) {
                blockLocation.getBlock().setType(Material.AIR);
            } else {
                if (blockLocation.getBlock().getType() == insurance.getBlockMaterial()) continue;
                if (shouldNotRestoreBlock(insurance.getBlockMaterial())) continue;

                blockLocation.getBlock().setType(insurance.getBlockMaterial());
                blockLocation.getBlock().setBlockData(Bukkit.createBlockData(insurance.getBlockData()));

            }
        }


    }

    private boolean shouldNotRestoreBlock(Material material) {
        for(String nonRestorable : nonRestorableBlocks){
            if(material.name().equalsIgnoreCase(nonRestorable)){
                return true;
            }
        }

        return material.isAir();
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onUpdateLore(ItemUpdateLoreEvent event) {

        Material material = event.getItem().getMaterial();
        if(shouldNotRestoreBlock(material)){
            event.getItemLore().add(Component.text("This block will not be restored after a siege.", NamedTextColor.DARK_GRAY));
        }
    }

}
