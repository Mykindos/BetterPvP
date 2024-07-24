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
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

@Singleton
@BPvPListener
public class Evade extends ChannelSkill implements InteractSkill, CooldownSkill, DefensiveSkill {

    private final HashMap<UUID, Long> handRaisedTime = new HashMap<>();

    public double duration;
    public int forcedDamageDelay;
    public double internalCooldown;
    public double internalCooldownDecreasePerLevel;

    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public Evade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Evade";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "If a player hits you while Evading, you",
                "will teleport behind the attacker and your",
                "cooldown will be set to a minimum of " + getValueString(this::getInternalCooldown, level) + " seconds ",
                "",
                "Hold crouch while Evading to teleport backwards",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    public double getInternalCooldown(int level){
        return internalCooldown - ((level - 1) * internalCooldownDecreasePerLevel);
    }


    @EventHandler (priority = EventPriority.LOW)
    public void onEvade(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;

        LivingEntity ent = event.getDamager();

        event.setKnockback(false);
        event.cancel("Skill Evade");
        event.setForceDamageDelay(forcedDamageDelay);

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

            int level = getLevel(player);
            cooldownManager.removeCooldown(player, getName(), true);

            long channelTime = System.currentTimeMillis() - handRaisedTime.get(player.getUniqueId());
            double channelTimeInSeconds = channelTime / 1000.0;
            double newCooldown = getInternalCooldown(level) + channelTimeInSeconds;

            if (!isReverse) {
                player.setRotation(ent.getLocation().getYaw(), ent.getLocation().getPitch());
            }

            cooldownManager.use(player, getName(), newCooldown, true);
            handRaisedTime.remove(player.getUniqueId());

            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %s<gray>.", getName(), level);

            if (ent instanceof Player temp) {
                UtilMessage.simpleMessage(temp, getClassType().getName(), "<yellow>%s<gray> used <green>%s %s</green>!", player.getName(), getName(), level);
            }

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
                Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                int level = getLevel(player);
                if (level > 0) {
                    if (!gamer.isHoldingRightClick()) {
                        handRaisedTime.remove(player.getUniqueId());
                        it.remove();
                        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("You failed <green>%s %d</green>", getName(), level));
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
                    } else if (UtilTime.elapsed(handRaisedTime.get(player.getUniqueId()), (long) duration * 1000)) {
                        handRaisedTime.remove(player.getUniqueId());
                        UtilMessage.simpleMessage(player, getClassType().getName(),"You failed <green>%s %d</green>", getName(), getLevel(player));
                        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                        it.remove();
                    }
                }

            } else {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent e) {
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
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        handRaisedTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 1.25, Double.class);
        forcedDamageDelay = getConfig("forcedDamageDelay", 400, Integer.class);
        internalCooldown = getConfig("internalCooldown", 0.6, Double.class);
        internalCooldownDecreasePerLevel = getConfig("internalCooldownDecreasePerLevel", 0.1, Double.class);
    }
}
