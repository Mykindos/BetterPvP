package me.mykindos.betterpvp.progression.profession.fishing.bait;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class BaitWeapon extends Weapon implements InteractWeapon {

    protected double radius;
    protected double multiplier;

    @Getter
    @Setter
    protected long duration;

    protected BaitWeapon(Progression plugin, String key) {
        super(plugin, key);
    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        lore.add(UtilMessage.deserialize("<gray>Radius: <green>%.1f</green>", radius));
        lore.add(UtilMessage.deserialize("<gray>Multiplier: <green>%.1fx</green> catch speed", multiplier));
        lore.add(UtilMessage.deserialize("<gray>Duration: <green>%d</green> seconds", duration));
        lore.addAll(List.of(Component.empty(), UtilMessage.DIVIDER, Component.empty()));
        lore.add(Component.text(""));
        lore.add(UtilMessage.deserialize("<yellow>Right Click <gray>to throw bait"));
        return lore;
    }

    @Override
    public void activate(Player player) {
        UtilInventory.remove(player, getMaterial(), 1);
        player.swingMainHand();
    }

    @Override
    public boolean canUse(Player player) {
        return isHoldingWeapon(player);
    }

    @Override
    public void loadWeaponConfig() {
        radius = getConfig("radius", 5.0, Double.class);
        multiplier = getConfig("multiplier", 1.0, Double.class);
        duration = getConfig("duration", 180, Integer.class);
    }

    @Override
    public boolean preventPlace() {
        return true;
    }

}
