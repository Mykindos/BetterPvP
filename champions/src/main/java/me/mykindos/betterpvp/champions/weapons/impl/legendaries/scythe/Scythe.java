package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@Slf4j
public class Scythe extends Weapon implements InteractWeapon, CooldownWeapon, LegendaryWeapon {

    protected final WeakHashMap<Player, List<BlackHole>> blackHoles = new WeakHashMap<>();

    @Inject
    @Config(path = "weapons.scythe.base-damage", defaultValue = "8.0", configName = "weapons/legendaries")
    protected double baseDamage;

    @Inject
    @Config(path = "weapons.scythe.heal-per-hit", defaultValue = "1.0", configName = "weapons/legendaries")
    protected double healPerHit;

    @Inject
    @Config(path = "weapons.scythe.black-hole-cooldown", defaultValue = "12.0", configName = "weapons/legendaries")
    protected double blackHoleCooldown;

    @Inject
    @Config(path = "weapons.scythe.black-hole-radius", defaultValue = "0.8", configName = "weapons/legendaries")
    protected double blackHoleRadius;

    @Inject
    @Config(path = "weapons.scythe.black-hole-speed", defaultValue = "3.0", configName = "weapons/legendaries")
    protected double blackHoleSpeed;

    @Inject
    @Config(path = "weapons.scythe.black-hole-hitbox", defaultValue = "0.5", configName = "weapons/legendaries")
    protected double blackHoleHitbox;

    @Inject
    @Config(path = "weapons.scythe.black-hole-pull-strength", defaultValue = "0.12", configName = "weapons/legendaries")
    protected double blackHolePullStrength;

    @Inject
    @Config(path = "weapons.scythe.black-hole-pull-radius", defaultValue = "5.0", configName = "weapons/legendaries")
    protected double blackHolePullRadius;

    @Inject
    @Config(path = "weapons.scythe.black-hole-alive-seconds", defaultValue = "1.5", configName = "weapons/legendaries")
    protected double blackHoleAliveSeconds;

    @Inject
    @Config(path = "weapons.scythe.black-hole-expand-seconds", defaultValue = "0.75", configName = "weapons/legendaries")
    protected double blackHoleExpandSeconds;

    @Inject
    public Scythe(final ClientManager clientManager) {
        super("scythe");
    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("An old blade fashioned of nothing more", NamedTextColor.WHITE));
        lore.add(Component.text("stray bones, brave adventurers have", NamedTextColor.WHITE));
        lore.add(Component.text("imbued it with the remnant powers of a", NamedTextColor.WHITE));
        lore.add(Component.text("dark and powerful foe.", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Black Hole"));
        lore.add(UtilMessage.deserialize("<yellow>Attack <white>to use <green>Life Steal"));
        return lore;
    }

    @Override
    public void activate(Player player) {
        final Location location = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.8));
        final BlackHole hole = new BlackHole(player,
                location,
                blackHoleHitbox,
                blackHoleRadius,
                blackHolePullStrength,
                blackHolePullRadius,
                blackHoleAliveSeconds,
                blackHoleExpandSeconds);
        hole.setSpeed(blackHoleSpeed);
        hole.redirect(player.getLocation().getDirection());
        blackHoles.computeIfAbsent(player, p -> new ArrayList<>()).add(hole);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public double getCooldown() {
        return blackHoleCooldown;
    }
}
