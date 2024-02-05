package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Singleton
public class EnergyApple extends Weapon implements InteractWeapon, CooldownWeapon {

    private final EnergyHandler energyHandler;

    @Inject
    @Config(path = "weapons.energy-apple.enabled", defaultValue = "true", configName = "weapons/standard")
    private boolean enabled;

    @Inject
    @Config(path = "weapons.energy-apple.cooldown", defaultValue = "10.0", configName = "weapons/standard")
    private double cooldown;

    @Inject
    @Config(path = "weapons.energy-apple.energy-regen", defaultValue = "0.50", configName = "weapons/standard")
    private double energyRegen;

    @Inject
    public EnergyApple(EnergyHandler energyHandler) {
        super("energy_apple");
        this.energyHandler = energyHandler;
    }

    @Override
    public void activate(Player player) {
        if (!enabled) {
            return;
        }
        energyHandler.regenerateEnergy(player, energyRegen);
        UtilMessage.message(player, "Item",
                Component.text("You consumed an ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
    }

    @Override
    public boolean canUse(Player player) {
        return isHoldingWeapon(player);
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }

    @Override
    public boolean isEnabled(){
        return enabled;
    }
}
