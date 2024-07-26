package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Tag;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;

import java.util.*;

@Singleton
@BPvPListener
public class RunedPickaxe extends Weapon implements  LegendaryWeapon, Listener {

    private double miningSpeed;

    @Inject
    public RunedPickaxe(Champions champions) {
        super(champions, "runed_pickaxe");
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("What an interesting design this pickaxe seems to have", NamedTextColor.WHITE));
        lore.add(Component.text("A pickaxe of legendary power, now with", NamedTextColor.WHITE));
        lore.add(Component.text("faster speed and unmatched strength!", NamedTextColor.WHITE));
        return lore;
    }

    @Override
    public void onInitialize(ItemMeta meta) {
        ToolComponent toolComponent = meta.getTool();
        toolComponent.addRule(Tag.MINEABLE_PICKAXE, (float) miningSpeed, true);
        meta.setTool(toolComponent);
    }

    @Override
    public void loadWeaponConfig() {
        miningSpeed = getConfig("miningSpeed", 30.0, Double.class);
    }
}
