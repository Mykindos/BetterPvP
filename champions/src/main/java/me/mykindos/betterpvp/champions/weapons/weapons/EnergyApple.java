package me.mykindos.betterpvp.champions.weapons.weapons;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.CooldownWeapon;
import me.mykindos.betterpvp.champions.weapons.types.InteractWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
public class EnergyApple extends Weapon implements InteractWeapon, CooldownWeapon {

    private final EnergyHandler energyHandler;

    @Inject
    @Config(path = "weapons.energy-apple.cooldown", defaultValue = "10.0")
    private double cooldown;

    @Inject
    @Config(path = "weapons.energy-apple.energy-regen", defaultValue = "0.50")
    private double energyRegen;

    @Inject
    public EnergyApple(EnergyHandler energyHandler) {
        super(Material.APPLE, Component.text("Energy Apple", NamedTextColor.LIGHT_PURPLE));
        this.energyHandler = energyHandler;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
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
}
