package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.VengeanceData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Vengeance extends Skill implements PassiveSkill, Listener, OffensiveSkill, DamageSkill {
    private final WeakHashMap<Player, VengeanceData> playerDataMap = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;

    /**
     * The number of melee hits a player can take before reaching max stacks. Once the player reaches max stacks,
     * they will not gain any more stacks until they use their next melee attack or the stacks expire.
     */
    private int maxStacks;

    /** The radius around the player in which enemies will be pulled inward when max stacks is reached. */
    private double pullInwardRadius;

    /** The strength of the pull inward when max stacks is reached. */
    private double pullInwardStrength;

    /** The delay between reaching max stacks and the pull inward actually happening, in milliseconds. */
    private long pullInwardDelayMillis;
    private long expirationTimeInMillis;

    private final PermanentComponent stacksActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable Player player = gamer.getPlayer();
                if (player == null || !player.isOnline()) return null;

                final @Nullable VengeanceData abilityData = playerDataMap.get(player);
                if (abilityData == null) return null;

                if (abilityData.isDoingInwardPull()) {
                    return Component.text("Pulling enemies inward...").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD);
                }

                final int hitsTaken = abilityData.getHitsTaken();
                final int hitsThatCanBeTaken = Math.max(0, maxStacks - hitsTaken);

                return Component.text("Hits Taken" + ":" + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                        .append(Component.text("\u25A0".repeat(hitsTaken)).color(NamedTextColor.GREEN))
                        .append(Component.text("\u25A0".repeat(hitsThatCanBeTaken)).color(NamedTextColor.RED));
            }
    );

    @Inject
    public Vengeance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Every hit you take will increase",
                "the damage of your next melee",
                "attack by " + getValueString(this::getDamage, level) + " damage.",
                "",
                "At max stacks, your melee attack",
                "pulls enemies in and resets stacks.",
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTakeDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)
                || !event.getBukkitCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) return;
        if (!(event.getDamagee() instanceof Player player)) return;

        final int level = getLevel(player);
        if (level <= 0) return;

        final @NotNull VengeanceData abilityData = playerDataMap.computeIfAbsent(player, p -> new VengeanceData());
        if (abilityData.getHitsTaken() >= maxStacks || abilityData.isDoingInwardPull()) return;

        abilityData.setLastTimeWhenTakenDamage(System.currentTimeMillis());
        abilityData.setHitsTaken(abilityData.getHitsTaken() + 1);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHit(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        final @Nullable VengeanceData abilityData = playerDataMap.get(player);
        if (abilityData == null || abilityData.getHitsTaken() <= 0 || abilityData.isDoingInwardPull()) return;

        final int level = getLevel(player);
        if (level <= 0) return;  // Shouldn't happen since the player has ability data but no harm in checking

        final double extraDamage = abilityData.getHitsTaken() * getDamage(level);
        event.addModifier(new SkillDamageModifier.Flat(this, extraDamage));

        final float pitch = 0.0f - (abilityData.getHitsTaken() * 0.5f);

        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, pitch);

        if (abilityData.getHitsTaken() >= maxStacks) {
            player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 2.0f);
            abilityData.setDoingInwardPull(true);
            abilityData.setInwardPullStartTime(System.currentTimeMillis());
            abilityData.setHitsTaken(0);
        }
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<Player> iterator = playerDataMap.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            if (!player.isOnline() || player.isDead() || !player.isValid()) {
                iterator.remove();
                continue;
            }

            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final @NotNull VengeanceData abilityData = playerDataMap.get(player);

            // If doing pull, don't worry about stacks expiring right now.
            if (!abilityData.isDoingInwardPull()) {
                if (UtilTime.elapsed(abilityData.getLastTimeWhenTakenDamage(), expirationTimeInMillis)) {
                    player.playSound(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    iterator.remove();
                }

                continue;
            }

            if (UtilTime.elapsed(abilityData.getInwardPullStartTime(), pullInwardDelayMillis)) {
                abilityData.setDoingInwardPull(false);
                doPullInward(player);
                iterator.remove();
                continue;
            }

            // Play some particles to signify inward pull
            Particle.GUST.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.5, 0.5, 0.5)
                    .location(player.getLocation())
                    .receivers(30)
                    .spawn();
        }
    }

    private void doPullInward(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 0.5f);

        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), pullInwardRadius)) {
            if (!player.hasLineOfSight(target.getLocation())) continue;

            final Vector direction = player.getLocation().toVector()
                    .subtract(target.getLocation().toVector())
                    .normalize();

            final VelocityData velocityData = new VelocityData(
                    direction, pullInwardStrength, true, 0.0D, 0.1D, 0.1D, true
            );

            UtilVelocity.velocity(target, player, velocityData, VelocityType.CUSTOM);
        }
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, stacksActionBar);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(stacksActionBar);
    }

    @Override
    public String getName() {
        return "Vengeance";
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 0.75, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.25, Double.class);
        maxStacks = getConfig("maxStacks", 3, Integer.class);
        pullInwardRadius = getConfig("pullInwardRadius", 5.0, Double.class);
        pullInwardStrength = getConfig("pullInwardStrength", 1.5, Double.class);

        final double pullInwardDelayInSeconds = getConfig("pullInwardDelay", 1.0, Double.class);
        pullInwardDelayMillis = (long) (pullInwardDelayInSeconds * 1000L);

        final double expirationTimeInSeconds = getConfig("expirationTime", 6.0, Double.class);
        expirationTimeInMillis = (long) (expirationTimeInSeconds * 1000L);
    }
}
