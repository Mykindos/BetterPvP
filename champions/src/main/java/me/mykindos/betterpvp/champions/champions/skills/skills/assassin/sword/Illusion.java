package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.UtilitySkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.health.EntityHealthService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Sends a running copy of the caster off in whatever direction they are looking, for as long as they keep their sword
 * raised. The cooldown starts when the illusion ends rather than when it is cast, so holding the channel out to its
 * full duration costs the caster the whole time twice over.
 */
@Singleton
@BPvPListener
public class Illusion extends ChannelSkill implements InteractSkill, CrowdControlSkill, DebuffSkill, UtilitySkill {

    private final Map<UUID, IllusionDecoy> decoys = new HashMap<>();
    private final EntityHealthService healthService;

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double speed;
    private double turnRate;
    private double health;
    private int slownessStrength;
    private double slownessRadius;
    private double slownessDuration;

    @Inject
    public Illusion(Champions champions, ChampionsManager championsManager, EntityHealthService healthService) {
        super(champions, championsManager);
        this.healthService = healthService;
    }

    @Override
    public String getName() {
        return "Illusion";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component slowness = Translations.component("champions.skill.effect.slowness.name")
                .append(Component.text(" " + UtilFormat.getRomanNumeral(slownessStrength))).color(NamedTextColor.WHITE);
        Component vanish = Translations.component("champions.skill.effect.vanish.name").color(NamedTextColor.WHITE);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.illusion.description",
                duration,
                slowness,
                Component.text(String.valueOf(slownessDuration), NamedTextColor.GREEN),
                Component.text(String.valueOf(slownessRadius), NamedTextColor.GREEN),
                cooldown,
                vanish
        );
        Component[] detail = Translations.componentLines("champions.skill.effect.vanish.detail", vanish);
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public boolean activate(Player player, int level) {
        if (decoys.containsKey(player.getUniqueId())) {
            return true;
        }

        if (championsManager.getCooldowns().hasCooldown(player, getName())) {
            UtilMessage.message(player, "core.prefix.cooldown", "champions.skill.cooldown",
                    getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)),
                    Component.text(String.valueOf(Math.max(0, championsManager.getCooldowns().getAbilityRecharge(player, getName()).getRemaining())), NamedTextColor.GREEN));
            return false;
        }

        active.add(player.getUniqueId());
        decoys.put(player.getUniqueId(), new IllusionDecoy(player, level, health, healthService, (long) (getDuration(level) * 1000L)));

        // Named after this skill so ending an illusion never lifts a Smoke Bomb the caster is also under.
        championsManager.getEffects().addEffect(player, EffectTypes.VANISH, getName(), 1,
                (long) (getDuration(level) * 1000L));

        new SoundEffect(Sound.BLOCK_COMPARATOR_CLICK, 1.4f, 1.0f).play(player);
        return true;
    }

    @UpdateEvent
    public void runIllusions() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            final Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            final IllusionDecoy decoy = decoys.get(player.getUniqueId());
            final Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (decoy == null || decoy.isExpired() || getLevel(player) <= 0 || !gamer.isHoldingRightClick() || !isHolding(player)) {
                endIllusion(player);
                iterator.remove();
                continue;
            }

            decoy.tick(player.getLocation().getYaw(), turnRate, speed);
        }
    }

    /**
     * Despawns the illusion, punishes whoever was standing next to it, and only then starts the cooldown.
     * <p>
     * Idempotent, because a channel can end either from the update loop above or from {@link #onCancel(Player)} - and a
     * death ends it both ways at once.
     */
    private void endIllusion(Player player) {
        final IllusionDecoy decoy = decoys.remove(player.getUniqueId());
        if (decoy == null) {
            return;
        }

        final Location location = decoy.getLocation();
        decoy.remove();
        championsManager.getEffects().removeEffect(player, EffectTypes.VANISH, getName());

        Particle.LARGE_SMOKE.builder()
                .location(location.clone().add(0, 1, 0))
                .receivers(60)
                .count(40)
                .offset(0.4, 0.7, 0.4)
                .extra(0.02)
                .spawn();
        Particle.CAMPFIRE_COSY_SMOKE.builder()
                .location(location.clone().add(0, 1, 0))
                .receivers(60)
                .count(12)
                .offset(0.5, 0.5, 0.5)
                .extra(0.01)
                .spawn();
        new SoundEffect(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 1.0f).play(location);
        new SoundEffect(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.6f, 1.0f).play(player.getLocation());

        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, location, slownessRadius)) {
            championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, getName(),
                    slownessStrength, (long) (slownessDuration * 1000L), true);
        }

        championsManager.getCooldowns().use(player, getName(), getCooldown(decoy.getLevel()), true,
                true, false, isHolding(player));
    }

    /**
     * Cutting the illusion down ends it exactly as letting go would - smoke, slowness and all. Whoever swung is
     * standing next to it when it bursts, so calling the bluff still costs them the slow.
     */
    @EventHandler
    public void onDecoyDeath(EntityDeathEvent event) {
        for (Map.Entry<UUID, IllusionDecoy> entry : decoys.entrySet()) {
            if (!entry.getValue().isBody(event.getEntity())) {
                continue;
            }

            final Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner != null) {
                cancel(owner);
            }
            return;
        }
    }

    @Override
    public void onCancel(Player player) {
        endIllusion(player);
    }

    @Override
    public boolean canUse(Player player) {
        return isHolding(player);
    }

    @Override
    public boolean isShieldInvisible() {
        return true;
    }

    @Override
    public boolean shouldShowShield(Player player) {
        return !championsManager.getCooldowns().hasCooldown(player, getName());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        // Blocks per tick. A sprinting player on flat ground settles at 0.2806, or 5.612 blocks a second.
        speed = getConfig("speed", 0.2806, Double.class);
        turnRate = getConfig("turnRate", 16.0, Double.class);
        health = getConfig("health", 20.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 2, Integer.class);
        slownessRadius = getConfig("slownessRadius", 4.0, Double.class);
        slownessDuration = getConfig("slownessDuration", 3.0, Double.class);
        cooldown = getConfig("cooldown", 18.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 2.0, Double.class);
    }
}
