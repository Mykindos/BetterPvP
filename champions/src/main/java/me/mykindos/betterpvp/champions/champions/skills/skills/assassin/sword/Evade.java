package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.delay.DamageDelayManager;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class Evade extends ChannelSkill implements InteractSkill, CooldownSkill, DefensiveSkill {

    private final HashMap<UUID, Long> handRaisedTime = new HashMap<>();

    private final DamageDelayManager damageDelayManager;

    private double activeBaseDuration;
    private double activeDurationIncreasePerLevel;
    private double baseDamageDelay;
    private double damageDelayIncreasePerLevel;
    private double successBaseCooldown;
    private double successCooldownDecreasePerLevel;
    private double channelTimeCooldownMultiplier;
    private double channelTimeCooldownMultiplierDecreasePerLevel;

    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public Evade(Champions champions, ChampionsManager championsManager, DamageDelayManager damageDelayManager) {
        super(champions, championsManager);
        this.damageDelayManager = damageDelayManager;
    }

    @Override
    public String getName() {
        return "Evade";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getActiveDuration, level);
        Component cooldown = getValueComponent(this::getSuccessCooldown, level);
        Component baseCooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.assassin.evade.description",
                duration,
                cooldown,
                baseCooldown
        );
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    public double getActiveDuration(int level) {
        return activeBaseDuration + (level - 1) * activeDurationIncreasePerLevel;
    }

    public double getDamageDelay(int level) {
        return baseDamageDelay + (level - 1) * damageDelayIncreasePerLevel;
    }

    public double getSuccessCooldown(int level) {
        return successBaseCooldown - (level - 1) * successCooldownDecreasePerLevel;
    }

    public double getChannelTimeCooldownMultiplier(int level) {
        return channelTimeCooldownMultiplier - (level - 1) * channelTimeCooldownMultiplierDecreasePerLevel;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onEvade(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        int level = getLevel(player);
        if (level <= 0) return;
        LivingEntity ent = event.getDamager();

        event.setKnockback(false);
        event.cancel("Skill Evade");
        player.setVelocity(new Vector(0, 0, 0));
        damageDelayManager.addDelay(event.getDamager(), event.getDamagee(), event.getCause(), (long) (getDamageDelay(level) * 1000L));

        Particle.LARGE_SMOKE.builder()
                .offset(0.3, 0.3, 0.3)
                .count(3)
                .location(player.getLocation().add(0, player.getHeight() / 2, 0))
                .receivers(60)
                .extra(0)
                .spawn();

        final Vector direction = ent.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        double distance = ent.getLocation().distance(player.getLocation()) + 1.5;
        final boolean isReverse = player.isSneaking();
        if (isReverse) {
            distance = 1.5;
            direction.multiply(new Vector(-1, 1, -1)); // flip horizontal
        }

        UtilLocation.teleportToward(player, direction, distance, false, success -> {
            if (!Boolean.TRUE.equals(success)) {
                return;
            }
            player.setVelocity(new Vector(0, 0, 0));
            cooldownManager.removeCooldown(player, getName(), true);

            long channelTime = System.currentTimeMillis() - handRaisedTime.get(player.getUniqueId());
            double channelTimeInSeconds = channelTime / 1000.0;
            double newCooldown = getSuccessCooldown(level) + channelTimeInSeconds * getChannelTimeCooldownMultiplier(level);

            if (!isReverse) {
                if (!UtilLocation.isInFront(ent, player.getLocation())) {
                    player.setRotation(ent.getLocation().getYaw(), ent.getLocation().getPitch());
                }
            }

            cooldownManager.use(player, getName(), newCooldown, true);
            handRaisedTime.remove(player.getUniqueId());

            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.used", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));

            UtilMessage.message(ent, getClassType().getDisplayName(), "champions.skill.assassin.evade.target-used", this.championsManager.getDisplayNameAsComponent(player, ent), getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));

            active.remove(player.getUniqueId());
        });
    }

    @EventHandler
    public void onCustomVelocity(CustomEntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;

        if (hasSkill(player)) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                int level = getLevel(player);
                if (level > 0) {
                    if (!player.isHandRaised()) {
                        handRaisedTime.remove(player.getUniqueId());
                        it.remove();
                        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.failed", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
                        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                    } else if (!handRaisedTime.containsKey(player.getUniqueId())) {
                        it.remove();
                    } else if (!isHolding(player)) {
                        it.remove();
                    } else if (UtilBlock.isInLiquid(player)) {
                        it.remove();
                    } else if (championsManager.getEffects().hasEffect(player, EffectTypes.SILENCE)) {
                        it.remove();
                    } else if (championsManager.getEffects().hasEffect(player, EffectTypes.STUN)) {
                        it.remove();
                    } else if (UtilTime.elapsed(handRaisedTime.get(player.getUniqueId()), (long) (getActiveDuration(level) * 1000))) {
                        handRaisedTime.remove(player.getUniqueId());
                        UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.failed", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(getLevel(player)), NamedTextColor.GREEN));
                        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                        it.remove();
                    }
                    spawnParticles(player);
                } else {
                    it.remove();
                }

            } else {
                it.remove();
            }
        }
    }

    public void spawnParticles(Player player) {
        final Location location = UtilPlayer.getMidpoint(player);
        new SoundEffect(Sound.ENTITY_PLAYER_SMALL_FALL, 1.0f, 0.2f).play(location);
        Particle.BLOCK.builder()
                .count(10)
                .location(location)
                .offset(0.3, 0.3, 0.3)
                .data(Material.BEDROCK.createBlockData())
                .receivers(60)
                .spawn();
    }

    @EventHandler
    public void onDamage(DamageEvent e) {
        if (e.getDamager() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                e.cancel("Skill: Evade");
            }
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1);
    }

    @Override
    public boolean activate(Player player, int level) {
        active.add(player.getUniqueId());
        handRaisedTime.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        activeBaseDuration = getConfig("activeBaseDuration", 0.6, Double.class);
        activeDurationIncreasePerLevel = getConfig("activeDurationIncreasePerLevel", 0.0, Double.class);
        baseDamageDelay = getConfig("baseDamageDelay", 0.4, Double.class);
        damageDelayIncreasePerLevel = getConfig("damageDelayIncreasePerLevel", 0.0, Double.class);
        successBaseCooldown = getConfig("successBaseCooldown", 0.8, Double.class);
        successCooldownDecreasePerLevel = getConfig("successCooldownDecreasePerLevel", 0.1, Double.class);
        channelTimeCooldownMultiplier = getConfig("channelTimeCooldownMultiplier", 1.0, Double.class);
        channelTimeCooldownMultiplierDecreasePerLevel = getConfig("channelTimeCooldownMultiplierDecreasePerLevel", 0.0, Double.class);
    }
}
