package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
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
    private double energyRegen;

    @Inject
    public EnergyApple(Champions champions, EnergyHandler energyHandler) {
        super(champions, "energy_apple");
        this.energyHandler = energyHandler;
    }

    @Override
    public void activate(Player player) {
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
    public void loadWeaponConfig() {
        energyRegen = getConfig("energyRegen", 0.50, Double.class);
    }

    @Override
    public boolean showCooldownOnItem() {
        return true;
    }
}
