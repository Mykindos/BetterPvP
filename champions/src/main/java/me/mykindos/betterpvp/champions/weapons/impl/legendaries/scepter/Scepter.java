package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
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
public class Scepter extends Weapon implements InteractWeapon, CooldownWeapon, LegendaryWeapon {

    protected final WeakHashMap<Player, List<BlackHole>> blackHoles = new WeakHashMap<>();

    protected double blackHoleRadius;
    protected double blackHoleSpeed;
    protected double blackHoleHitbox;
    protected double blackHolePullStrength;
    protected double blackHolePullRadius;
    protected double blackHoleAliveSeconds;
    protected double blackHoleExpandSeconds;

    @Inject
    public Scepter(Champions champions, final ClientManager clientManager) {
        super(champions, "scepter");
    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Legend says that this scepter was", NamedTextColor.WHITE));
        lore.add(Component.text("retrieved from the deepest trench in", NamedTextColor.WHITE));
        lore.add(Component.text("all of Minecraftia. It is said that he", NamedTextColor.WHITE));
        lore.add(Component.text("who wields this scepter holds the power", NamedTextColor.WHITE));
        lore.add(Component.text("of Poseidon himself.", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Black Hole"));
        return lore;
    }

    @Override
    public void activate(Player player) {
        final Location location = player.getEyeLocation();
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
        return cooldown;
    }

    @Override
    public void loadWeaponConfig() {
        blackHoleRadius = getConfig("blackHoleRadius", 0.8, Double.class);
        blackHoleSpeed = getConfig("blackHoleSpeed", 3.0, Double.class);
        blackHoleHitbox = getConfig("blackHoleHitbox", 0.5, Double.class);
        blackHolePullStrength = getConfig("blackHolePullStrength", 0.12, Double.class);
        blackHolePullRadius = getConfig("blackHolePullRadius", 5.0, Double.class);
        blackHoleAliveSeconds = getConfig("blackHoleAliveSeconds", 1.5, Double.class);
        blackHoleExpandSeconds = getConfig("blackHoleExpandSeconds", 0.75, Double.class);
    }
}
