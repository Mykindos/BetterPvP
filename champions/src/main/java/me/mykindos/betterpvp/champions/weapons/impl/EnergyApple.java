package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

@Singleton
public class EnergyApple extends Weapon implements InteractWeapon, CooldownWeapon {

    private final EnergyService energyService;
    private double energyRegen;

    @Inject
    public EnergyApple(Champions champions, EnergyService energyService) {
        super(champions, "energy_apple");
        this.energyService = energyService;
    }

    @Override
    public void activate(Player player) {
        energyService.regenerateEnergy(player, energyRegen, EnergyEvent.CAUSE.CUSTOM);
        UtilMessage.message(player, "Item",
                Component.text("You consumed an ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        UtilInventory.remove(player, getMaterial(), 1);
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Energy: <yellow>25.0", baseDamage));
        return lore;
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
        energyRegen = getConfig("energyRegen", 25.0, Double.class);
    }

    @Override
    public boolean showCooldownOnItem() {
        return true;
    }
}
