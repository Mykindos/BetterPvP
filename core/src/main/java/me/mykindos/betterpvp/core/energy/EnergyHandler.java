package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.energy.events.DegenerateEnergyEvent;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@Singleton
public class EnergyHandler {

    @Inject
    @Config(path = "energy.nerf-energy-regen", defaultValue = "false")
    private boolean nerfEnergyRegen;

    final public static double baseEnergy = 150.0D;
    final public static double playerEnergy = 0.0D;
    final public static double baseEnergyRegen = 0.006D;
    final public static double nerfedEnergyRegen = 0.0008D;
    final public static long updateRate = 50L;

    public double getEnergy(Player player) {
        return player.getExp();
    }

    public void setEnergy(Player player, float energy) {
        player.setExp(energy);
    }

    public double getMax(Player player) {
        return baseEnergy;
    }

    public boolean use(Player player, String ability, double amount, boolean inform) {
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        amount = 0.99999999999 * (amount / 100);

        if (amount > getEnergy(player)) {
            if (inform) {
                UtilMessage.simpleMessage(player, "Energy", "You are too exhausted to use <green>" + ability + "</green>.");
                player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
            }

            return false;
        }

        UtilServer.callEvent(new DegenerateEnergyEvent(player, amount));

        return true;
    }

    public void regenerateEnergy(Player player, double energy) {
        try {
            player.setExp(Math.min(0.999F, (float) getEnergy(player) + (float) energy));
        } catch (Exception ignored) {

        }
    }

    public void degenerateEnergy(Player player, double energy) {
        double eg = getEnergy(player);
        if (eg <= 0F) return;
        try {
            player.setExp(Math.max(0.001f, (float) eg - (float) energy));
        } catch (Exception ignored) {

        }
    }

    public void updateEnergy(Player cur) {
        if (cur.getExp() >= 0.999F) {
            return;
        }

        if (cur.isDead()) {
            return;
        }

        double energy = baseEnergyRegen;

        if (nerfEnergyRegen) {
            if (cur.isSprinting() || UtilBlock.isInLiquid(cur) || cur.isGliding()) {
                energy = nerfedEnergyRegen;
            }
        }

        Bukkit.getPluginManager().callEvent(new RegenerateEnergyEvent(cur, energy));


    }

}
