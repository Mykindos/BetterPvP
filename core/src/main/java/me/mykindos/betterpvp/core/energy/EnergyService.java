package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.energy.events.UpdateMaxEnergyEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@CustomLog
public class EnergyService {

    @Inject
    @Config(path = "energy.nerf-energy-regen", defaultValue = "false")
    private boolean nerfEnergyRegen;

    @Inject
    @Config(path = "energy.consumption-regen-delay", defaultValue = "1.5")
    private double consumptionRegenDelay;

    @Getter
    private final Map<UUID, Energy> energyMap = new ConcurrentHashMap<>();

    public static final double BASE_ENERGY = 150.0D;
    /**
     * Default energy regenerated per second
     */
    public static final double BASE_ENERGY_REGEN = 10D;
    public static final double NERFED_ENERGY_REGEN = 8D;
    public static final long UPDATE_RATE = 50L;

    @Inject
    public EnergyService() {
    }

    public Energy getEnergyObject(UUID id) {
        addToMap(id);
        return energyMap.get(id);
    }

    public double getEnergy(UUID id) {
        addToMap(id);
        return energyMap.get(id).getCurrent();
    }

    public void setEnergy(UUID id, double energy) {
        addToMap(id);
        energyMap.get(id).setCurrent(energy);
    }

    public void addEnergyCooldown(Player player) {
        energyMap.get(player.getUniqueId()).setLastUse(System.currentTimeMillis());
    }

    public boolean isOnRegenCooldown(Player player) {
        long lastUsed = energyMap.get(player.getUniqueId()).getLastUse();
        return !UtilTime.elapsed(lastUsed, (long) (consumptionRegenDelay * 1000L));
    }

    public void reduceEnergy(UUID id, double energy) {
        energyMap.get(id).reduceEnergy(energy);
    }

    public void addEnergy(UUID id, double energy) {
        energyMap.get(id).addEnergy(energy);
    }

    public double getMax(UUID id) {
        addToMap(id);
        return energyMap.get(id).getMax();
    }

    public boolean use(Player player, String ability, double amount, boolean inform) {
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (amount > getEnergy(player.getUniqueId())) {
            if (inform) {
                UtilMessage.simpleMessage(player, "Energy", "You are too exhausted to use <green>" + ability + "</green>.");
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
            }

            return false;
        }

        degenerateEnergy(player, amount, EnergyEvent.CAUSE.USE);

        return true;
    }

    public void regenerateEnergy(Player player, double energy, EnergyEvent.CAUSE cause) {
        RegenerateEnergyEvent regenerateEnergyEvent = new RegenerateEnergyEvent(player, energy, cause);
        if (!regenerateEnergyEvent.callEvent()) return;

        addEnergy(player.getUniqueId(), energy);
    }

    public void degenerateEnergy(Player player, double energy, EnergyEvent.CAUSE cause) {
        DegenerateEnergyEvent degenerateEnergyEvent = new DegenerateEnergyEvent(player, energy, cause);
        if (!degenerateEnergyEvent.callEvent()) return;
        reduceEnergy(player.getUniqueId(), degenerateEnergyEvent.getEnergy());
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Energy>> iterator = energyMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Energy> entry = iterator.next();
            final Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            final Energy energy = entry.getValue();

            if (player.isDead()) {
                continue;
            }

            double regen = BASE_ENERGY_REGEN;

            if (nerfEnergyRegen) {
                if (player.isSprinting() || UtilBlock.isInLiquid(player) || player.isGliding()) {
                    regen = NERFED_ENERGY_REGEN;
                }
            }
            regen = regen / ((double) 1000 / UPDATE_RATE);
            RegenerateEnergyEvent regenerateEnergyEvent = new RegenerateEnergyEvent(player, regen, EnergyEvent.CAUSE.NATURAL);
            if (!regenerateEnergyEvent.callEvent()) continue;
            energy.addEnergy(regenerateEnergyEvent.getEnergy());
            if (Bukkit.getCurrentTick() % (20 * 1) == 0) {
                log.info(energy.toString()).submit();
            }
        }
    }


    /**
     * Updates the max amount of energy for a player
     * @param player
     * @return
     */
    public void updateMax(Player player) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            final UpdateMaxEnergyEvent event = new UpdateMaxEnergyEvent(player, BASE_ENERGY);
            double max = event.callEvent() ? event.getNewMax() : BASE_ENERGY;
            energyMap.get(player.getUniqueId()).setMax(max);
        });

    }



    /**
     * Adds the ID to the map if it is not already
     * @param id
     */
    private void addToMap(UUID id) {
        energyMap.computeIfAbsent(id, (key) -> {
            final Player player = Bukkit.getPlayer(key);
            if (player == null) return new Energy(BASE_ENERGY, BASE_ENERGY, 0);
            final UpdateMaxEnergyEvent event = new UpdateMaxEnergyEvent(player, BASE_ENERGY);
            double max = event.callEvent() ? event.getNewMax() : BASE_ENERGY;
            return new Energy(max, max, 0);
        });
    }

}
