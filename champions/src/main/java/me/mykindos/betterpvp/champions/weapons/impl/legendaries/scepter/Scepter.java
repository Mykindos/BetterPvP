package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@CustomLog
public class Scepter extends Weapon implements InteractWeapon, CooldownWeapon, LegendaryWeapon {

    protected final WeakHashMap<Player, List<BlackHole>> blackHoles = new WeakHashMap<>();
    protected final WeakHashMap<Player, List<MeridianBeam>> beams = new WeakHashMap<>();

    protected double blackHoleRadius;
    protected double blackHoleSpeed;
    protected double blackHoleHitbox;
    protected double blackHolePullStrength;
    protected double blackHolePullRadius;
    protected double blackHoleAliveSeconds;
    protected double blackHoleExpandSeconds;
    protected double blackHoleTravelSeconds;
    protected double beamCooldown;
    protected double beamDamage;
    protected double beamSpeed;
    protected double beamHitbox;
    protected double beamTravelSeconds;

    private final CooldownManager cooldownManager;

    @Inject
    public Scepter(Champions champions, CooldownManager cooldownManager) {
        super(champions, "scepter");
        this.cooldownManager = cooldownManager;
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Legend says that this scepter was", NamedTextColor.WHITE));
        lore.add(Component.text("retrieved from the deepest trench in", NamedTextColor.WHITE));
        lore.add(Component.text("all of Minecraftia. It is said that he", NamedTextColor.WHITE));
        lore.add(Component.text("who wields this scepter holds the power", NamedTextColor.WHITE));
        lore.add(Component.text("of Poseidon himself.", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click <white>to use <green>Black Hole"));
        lore.add(UtilMessage.deserialize("<yellow>Left-Click <white>to use <green>Meridian Beam"));
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
                blackHoleExpandSeconds,
                (long) (blackHoleTravelSeconds * 1000L));
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
        blackHolePullStrength = getConfig("blackHolePullStrength", 0.08, Double.class);
        blackHolePullRadius = getConfig("blackHolePullRadius", 5.0, Double.class);
        blackHoleAliveSeconds = getConfig("blackHoleAliveSeconds", 1.5, Double.class);
        blackHoleExpandSeconds = getConfig("blackHoleExpandSeconds", 0.75, Double.class);
        blackHoleTravelSeconds = getConfig("blackHoleTravelSeconds", 2.0, Double.class);
        beamCooldown = getConfig("beamCooldown", 1.0, Double.class);
        beamDamage = getConfig("beamDamage", 4.0, Double.class);
        beamSpeed = getConfig("beamSpeed", 4.0, Double.class);
        beamHitbox = getConfig("beamHitbox", 0.5, Double.class);
        beamTravelSeconds = getConfig("beamTravelSeconds", 0.3, Double.class);
    }

    protected void tryUseBeam(Player player) {
        if (!this.cooldownManager.use(player, MeridianBeam.NAME, beamCooldown, false, true, false, gmr -> isHoldingWeapon(player), 900)) {
            return;
        }

        final Location location = player.getEyeLocation();
        final MeridianBeam beam = new MeridianBeam(player,
                location,
                beamHitbox,
                beamSpeed,
                (long) (beamTravelSeconds * 1000L),
                beamDamage);
        beam.setSpeed(beamSpeed);
        beam.redirect(player.getLocation().getDirection());
        beams.computeIfAbsent(player, p -> new ArrayList<>()).add(beam);
    }
}
