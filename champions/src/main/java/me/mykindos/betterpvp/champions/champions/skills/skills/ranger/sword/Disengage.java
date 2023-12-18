package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Disengage extends ChannelSkill implements CooldownSkill, InteractSkill {

    private final WeakHashMap<UUID, Long> handRaisedTime = new WeakHashMap<>();

    private double baseSlowDuration;

    private double slowDurationIncreasePerLevel;

    private double baseChannelDuration;

    private double channelDurationincreasePerLevel;

    private int slowStrength;

    @Inject
    public Disengage(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Disengage";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "If you are attacked while channeling for less than <stat>" + getChannelDuration(level) + "</stat> seconds,",
                "you successfully disengage, leaping backwards",
                "and giving your attacker <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength + 1) + "</effect> for",
                "<val>" + getSlowDuration(level) + "</val> seconds",
                "",
                "Cooldown: <stat>" + getCooldown(level)};
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + level * slowDurationIncreasePerLevel;
    }

    public double getChannelDuration(int level) {
        return baseChannelDuration + level * channelDurationincreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (!active.contains(damagee.getUniqueId())) return;

        int level = getLevel(damagee);

        long startTime = handRaisedTime.getOrDefault(damagee.getUniqueId(), 0L);
        if (!UtilTime.elapsed(startTime, (long) getChannelDuration(level) * 1000L)) {
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2.0f, 2.0f);
            LivingEntity ent = event.getDamager();
            Vector vec = ent.getLocation().getDirection();

            event.setKnockback(false);
            event.setDamage(0);
            UtilVelocity.velocity(damagee, vec, 3D, true, 0.0D, 0.4D, 1.5D, true);
            championsManager.getEffects().addEffect(damagee, EffectType.NOFALL, 3000);
            ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getSlowDuration(level) * 20), slowStrength));
            UtilMessage.message(damagee, getClassType().getName(), "You successfully disengaged.");

            active.remove(damagee.getUniqueId());
            handRaisedTime.remove(damagee.getUniqueId());
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdateEffect() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                if (player.isHandRaised()) {
                    player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.IRON_BLOCK);
                }
            } else {
                it.remove();
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            UUID playerId = it.next();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isHandRaised()) {
                resetPlayerState(it, playerId, player);
                continue;
            }

            long startTime = handRaisedTime.getOrDefault(playerId, System.currentTimeMillis());

            if (UtilTime.elapsed(startTime, (long) getChannelDuration(getLevel(player)) * 1000L)) {
                resetPlayerState(it, playerId, player);
            } else {
                handRaisedTime.put(playerId, startTime);
            }
        }
    }

    private void resetPlayerState(Iterator<UUID> iterator, UUID playerId, Player player) {
        iterator.remove();
        handRaisedTime.remove(playerId);
        if (player != null) {
            UtilMessage.message(player, getClassType().getName(), "Your disengage failed.");
        }
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - level * cooldownDecreasePerLevel;
    }

    @Override
    public void activate(Player player, int level) {
        UUID playerId = player.getUniqueId();
        active.add(playerId);
        handRaisedTime.put(playerId, System.currentTimeMillis());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 1.0, Double.class);

        baseChannelDuration = getConfig("baseChannelDuration", 1.0, Double.class);
        channelDurationincreasePerLevel =  getConfig("channelDurationincreasePerLevel", 0.0, Double.class);

        slowStrength = getConfig("slowStrength", 3, Integer.class);
    }
}
