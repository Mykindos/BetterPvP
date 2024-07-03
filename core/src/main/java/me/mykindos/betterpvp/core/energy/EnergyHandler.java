package me.mykindos.betterpvp.core.energy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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

    public static final double BASE_ENERGY = 150.0D;
    public static final double PLAYER_ENERGY = 0.0D;
    public static final double BASE_ENERGY_REGEN = 0.006D;
    public static final double NERFED_ENERGY_REGEN = 0.0008D;
    public static final long UPDATE_RATE = 50L;

    public double getEnergy(Player player) {
        return player.getExp();
    }

    public void setEnergy(Player player, float energy) {
        player.setExp(energy);
    }

    public double getMax(Player player) {
        return BASE_ENERGY;
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

        double energy = BASE_ENERGY_REGEN;

        if (nerfEnergyRegen) {
            if (cur.isSprinting() || UtilBlock.isInLiquid(cur) || cur.isGliding()) {
                energy = NERFED_ENERGY_REGEN;
            }
        }

        Bukkit.getPluginManager().callEvent(new RegenerateEnergyEvent(cur, energy));


    }

}
