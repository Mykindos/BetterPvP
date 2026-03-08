package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
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

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EnergyService {

    @Inject
    @Config(path = "energy.nerf-energy-regen", defaultValue = "false")
    private boolean nerfEnergyRegen;

    @Inject
    @Config(path = "energy.consumption-regen-delay", defaultValue = "1.0")
    private double consumptionRegenDelay;

    @Getter
    @Inject
    @Config(path = "energy.max-energy", defaultValue = "150.0")
    public double maxEnergy = 150.0D;

    @Getter
    @Inject
    @Config(path = "energy.energy-per-second", defaultValue = "10.0")
    public double energyPerSecond = 10D;

    @Getter
    @Inject
    @Config(path = "energy.energy-per-tick-nerfed", defaultValue = "8.0")
    public double nerfedEnergyPerSecond = 8D;

    @Getter
    private final Map<UUID, Energy> energyMap = new ConcurrentHashMap<>();
    private final CooldownManager cooldownManager;

    @Inject
    public EnergyService(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
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

    /**
     * Attempts to use the specified amount of energy for the player. If the player does not have enough energy, it will return false and optionally inform the player.
     * @param player the player using the energy
     * @param ability the name of the ability being used (for messaging purposes)
     * @param amount the amount of energy to use
     * @param inform whether to inform the player if they do not have enough energy
     * @return {@code true} if the energy was successfully used, {@code false} if the player did not have enough energy
     */
    public boolean use(Player player, String ability, double amount, boolean inform) {
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        final Energy energy = getEnergyObject(player.getUniqueId());
        if (amount > energy.getCurrent()) {
            if (inform && this.cooldownManager.use(player, ability + "_no_energy", 0.5, false)) {
                UtilMessage.simpleMessage(player, "Energy", "You are too exhausted to use <green>" + ability + "</green>.");
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
            }

            return false;
        }

        degenerateEnergy(player, amount, EnergyEvent.Cause.USE);

        return true;
    }

    public void regenerateEnergy(Player player, double energy, EnergyEvent.Cause cause) {
        RegenerateEnergyEvent regenerateEnergyEvent = new RegenerateEnergyEvent(player, energy, cause);
        if (!regenerateEnergyEvent.callEvent()) return;

        addEnergy(player.getUniqueId(), energy);
    }

    public void degenerateEnergy(Player player, double energy, EnergyEvent.Cause cause) {
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

            double regen = energyPerSecond;

            if (nerfEnergyRegen) {
                if (player.isSprinting() || UtilBlock.isInLiquid(player) || player.isGliding()) {
                    regen = nerfedEnergyPerSecond;
                }
            }
            regen = regen / 20;
            RegenerateEnergyEvent regenerateEnergyEvent = new RegenerateEnergyEvent(player, regen, EnergyEvent.Cause.NATURAL);
            if (!regenerateEnergyEvent.callEvent()) continue;
            energy.addEnergy(regenerateEnergyEvent.getEnergy());
        }
    }

    /**
     * Updates the max amount of energy for a player
     * @param player the player to update the max energy for
     */
    public void updateMax(Player player) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            final UpdateMaxEnergyEvent event = new UpdateMaxEnergyEvent(player, maxEnergy);
            double max = event.callEvent() ? event.getNewMax() : maxEnergy;
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
            if (player == null) return new Energy(maxEnergy, maxEnergy, 0);
            final UpdateMaxEnergyEvent event = new UpdateMaxEnergyEvent(player, maxEnergy);
            double max = event.callEvent() ? event.getNewMax() : maxEnergy;
            return new Energy(max, max, 0);
        });
    }

}
