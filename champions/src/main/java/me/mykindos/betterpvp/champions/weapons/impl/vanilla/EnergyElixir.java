package me.mykindos.betterpvp.champions.weapons.impl.vanilla;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class EnergyElixir extends Weapon implements InteractWeapon, CooldownWeapon {

    private final EnergyHandler energyHandler;
    private double energyRegen;

    @Inject
    public EnergyElixir(Champions champions, EnergyHandler energyHandler) {
        super(champions, "energy_elixir");
        this.energyHandler = energyHandler;
    }

    @Override
    public void activate(Player player) {
        energyHandler.regenerateEnergy(player, energyRegen);
        UtilMessage.message(player, "Item",
                Component.text("You consumed an ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ITEM_HONEY_BOTTLE_DRINK, 1.2f, 1f, false);
        UtilInventory.remove(player, getMaterial(), 1);
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Energy: <yellow>50.0", baseDamage));
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
        energyRegen = getConfig("energyRegen", 0.50, Double.class);
    }

    @Override
    public boolean showCooldownOnItem() {
        return true;
    }
}

