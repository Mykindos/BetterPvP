package me.mykindos.betterpvp.clans.clans.insurance;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Set;

@BPvPListener
public class InsuranceListener extends ClanListener {

    @Inject
    @Config(path = "clans.insurance.durationInHours", defaultValue = "24")
    private int expiryTime;

    @Inject
    @Config(path = "clans.insurance.enabled", defaultValue = "true")
    private boolean enabled;
    private final Set<Material> nonRestorables = Set.of(Material.IRON_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK,
            Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.ENCHANTING_TABLE, Material.BEEHIVE,
            Material.TNT, Material.REDSTONE_BLOCK, Material.WATER, Material.LAVA, Material.ICE
    );

    @Inject
    public InsuranceListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
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

    @UpdateEvent(delay = 1)
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
                if (!shouldRestoreBlock(insurance.getBlockMaterial())) continue;

                blockLocation.getBlock().setType(insurance.getBlockMaterial());
                blockLocation.getBlock().setBlockData(Bukkit.createBlockData(insurance.getBlockData()));

            }
        }


    }

    private boolean shouldRestoreBlock(Material material) {
        if (nonRestorables.contains(material)) return false;
        return !material.isAir();
    }

}
