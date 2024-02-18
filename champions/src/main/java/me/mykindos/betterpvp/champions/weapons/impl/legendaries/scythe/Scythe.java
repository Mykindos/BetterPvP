package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.config.Config;
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
@Slf4j
public class Scythe extends ChannelWeapon implements LegendaryWeapon, Listener {

    protected final WeakHashMap<Player, ScytheData> tracked = new WeakHashMap<>();
    protected final Map<UUID, Soul> souls = new HashMap<>();
    private final ClientManager clientManager;

    @Inject
    @Config(path = "scythe.base-damage", defaultValue = "8.0", configName = "weapons/legendaries")
    protected double baseDamage;

    @Inject
    @Config(path = "scythe.max-souls-damage", defaultValue = "4.0", configName = "weapons/legendaries")
    protected double maxSoulsDamage;

    @Inject
    @Config(path = "scythe.max-souls",  defaultValue = "3", configName = "weapons/legendaries")
    protected int maxSouls;

    @Inject
    @Config(path = "scythe.soul-harvest-seconds", defaultValue = "1.5", configName = "weapons/legendaries")
    protected double soulHarvestSeconds;

    @Inject
    @Config(path = "scythe.soul-expiry-seconds", defaultValue = "10.0", configName = "weapons/legendaries")
    protected double soulExpirySeconds;

    @Inject
    @Config(path = "scythe.soul-expiry-per-second", defaultValue = "0.3", configName = "weapons/legendaries")
    protected double soulExpiryPerSecond;

    @Inject
    @Config(path = "scythe.soul-despawn-seconds", defaultValue = "7.5", configName = "weapons/legendaries")
    protected double soulDespawnSeconds;

    @Inject
    @Config(path = "scythe.soul-view-distance-blocks", defaultValue = "60", configName = "weapons/legendaries")
    protected int soulViewDistanceBlocks;

    @Inject
    @Config(path = "scythe.souls-per-player", defaultValue = "1.0", configName = "weapons/legendaries")
    protected double soulsPerPlayer;

    @Inject
    @Config(path = "scythe.souls-per-mob", defaultValue = "0.5", configName = "weapons/legendaries")
    protected double soulsPerMob;

    @Inject
    @Config(path = "scythe.summon-player-soul-chance", defaultValue = "1.0", configName = "weapons/legendaries")
    protected double summonPlayerSoulChance;

    @Inject
    @Config(path = "scythe.summon-mob-soul-chance", defaultValue = "0.2", configName = "weapons/legendaries")
    protected double summonMobSoulChance;

    @Inject
    @Config(path = "scythe.speed-amplifier-per-soul", defaultValue = "1.0", configName = "weapons/legendaries")
    protected double speedAmplifierPerSoul;

    @Inject
    @Config(path = "scythe.base-heal", defaultValue = "0.25", configName = "weapons/legendaries")
    protected double baseHeal;

    @Inject
    @Config(path = "scythe.heal-per-soul", defaultValue = "0.05", configName = "weapons/legendaries")
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
            soul.show(player, false);
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
}
