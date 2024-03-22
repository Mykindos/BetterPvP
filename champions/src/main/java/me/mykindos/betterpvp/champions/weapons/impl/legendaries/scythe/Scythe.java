package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class Scythe extends ChannelWeapon implements LegendaryWeapon, Listener {

    protected final WeakHashMap<Player, ScytheData> tracked = new WeakHashMap<>();
    protected final Map<UUID, Soul> souls = new HashMap<>();
    private final ClientManager clientManager;

    protected double maxSoulsDamage;
    protected int maxSouls;
    protected double soulHarvestSeconds;
    protected double soulExpirySeconds;
    protected double soulExpiryPerSecond;
    protected double soulDespawnSeconds;
    protected int soulViewDistanceBlocks;
    protected double soulsPerPlayer;
    protected double soulsPerMob;
    protected double summonPlayerSoulChance;
    protected double summonMobSoulChance;
    protected double speedAmplifierPerSoul;
    protected double baseHeal;
    protected double healPerSoul;

    @SuppressWarnings("DataFlowIssue")
    private final DisplayComponent actionBar = ChargeData.getActionBar(
            gmr -> gmr.isOnline() && tracked.containsKey(gmr.getPlayer()) && isHoldingWeapon(gmr.getPlayer()),
            gmr -> tracked.get(gmr.getPlayer()).getChargeData()
    );

    @Inject
    public Scythe(final Champions champions, final ClientManager clientManager) {
        super(champions, "scythe");
        this.clientManager = clientManager;
    }

    protected void active(Player player) {
        tracked.compute(player,
                (p, prev) -> {
                    if (prev != null) {
                        prev.setMarkForRemoval(false);
                        prev.getGamer().getActionBar().add(350, actionBar);
                        return prev;
                    }
                    final ScytheData data = new ScytheData(this, clientManager.search().online(p).getGamer());
                    data.getGamer().getActionBar().add(350, actionBar);
                    return data;
                });

        for (Soul soul : souls.values()) {
            soul.show(player, false, this);
        }
    }

    protected void pause(Player player, ScytheData data) {
        data.stopHarvesting();
        data.getGamer().getActionBar().remove(actionBar);

        for (Soul soul : souls.values()) {
            soul.hide(player);
        }
    }

    protected void deactivate(Player player) {
        final ScytheData data = tracked.get(player);
        if (data != null) {
            data.setMarkForRemoval(true);
            pause(player, data);
        }
    }

    public Optional<Soul> getTargetSoul(Player player) {
        for (Soul soul : souls.values()) {
            final Location loc = soul.getLocation();
            final BoundingBox box = new BoundingBox(loc.getX() - 1,
                    loc.getY() - 1,
                    loc.getZ() - 1,
                    loc.getX() + 1,
                    loc.getY() + 1,
                    loc.getZ() + 1);

            final RayTraceResult trace = box.rayTrace(player.getEyeLocation().toVector(),
                    player.getLocation().getDirection(),
                    soulViewDistanceBlocks);

            if (trace != null) {
                return Optional.of(soul);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean useShield(Player player) {
        return getTargetSoul(player).isPresent();
    }

    @Override
    public List<Component> getLore() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("An old blade fashioned of nothing more", NamedTextColor.WHITE));
        lore.add(Component.text("stray bones, brave adventurers have", NamedTextColor.WHITE));
        lore.add(Component.text("imbued it with the remnant powers of a", NamedTextColor.WHITE));
        lore.add(Component.text("dark and powerful foe.", NamedTextColor.WHITE));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Mobs and players drop souls on death"));
        lore.add(UtilMessage.deserialize("<white>At <yellow>100%%</yellow> souls, you deal an extra <yellow>%.1f damage<white>", maxSoulsDamage));
        lore.add(UtilMessage.deserialize("<white>on attack and gain a <green>Speed</green> effect."));
        lore.add(Component.empty());
        lore.add(UtilMessage.deserialize("<white>Deals <yellow>%.1f Damage <white>with attack", baseDamage));
        lore.add(UtilMessage.deserialize("<yellow>Right-Click on a Soul <white>to use <green>Soul Harvest"));
        lore.add(UtilMessage.deserialize("<yellow>Attack <white>to use <green>Life Steal"));
        return lore;
    }

    @Override
    public double getEnergy() {
        return 0; // No energy
    }

    @Override
    public void loadWeaponConfig() {
        maxSoulsDamage = getConfig("maxSoulsDamage", 4.0, Double.class);
        maxSouls = getConfig("maxSouls", 3, Integer.class);
        soulHarvestSeconds = getConfig("soulHarvestSeconds", 1.5, Double.class);
        soulExpirySeconds = getConfig("soulExpirySeconds", 10.0, Double.class);
        soulExpiryPerSecond = getConfig("soulExpiryPerSecond", 0.3, Double.class);
        soulDespawnSeconds = getConfig("soulDespawnSeconds", 7.5, Double.class);
        soulViewDistanceBlocks = getConfig("soulViewDistanceBlocks", 60, Integer.class);
        soulsPerPlayer = getConfig("soulsPerPlayer", 1.0, Double.class);
        soulsPerMob = getConfig("soulsPerMob", 1.0, Double.class);
        summonPlayerSoulChance = getConfig("summonPlayerSoulChance", 1.0, Double.class);
        summonMobSoulChance = getConfig("summonMobSoulChance", 0.4, Double.class);
        speedAmplifierPerSoul = getConfig("speedAmplifierPerSoul", 1.0, Double.class);
        baseHeal = getConfig("baseHeal", 0.25, Double.class);
        healPerSoul = getConfig("healPerSoul", 0.1, Double.class);
    }
}
