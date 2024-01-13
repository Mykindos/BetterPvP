package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class  HiltSmash extends Skill implements CooldownSkill, Listener {

    private WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double baseDuration;

    private double durationIncreasePerLevel;

    @Inject
    public HiltSmash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hilt Smash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Smash the hilt of your sword into",
                "your opponent, dealing <val>" + getDamage(level) + "</val> damage,",
                "applying <effect>Shock</effect> for <val>" + getDuration(level) + "</val> seconds,",
                "and <effect>Silence</effect> the enemy for <val>" + getDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        rightClicked.put(event.getPlayer(), true);
        if (event.getRightClicked() instanceof LivingEntity entity) {
            onInteract(event.getPlayer(), entity);
        } else {
            onInteract(event.getPlayer(), null);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !event.getAction().isRightClick()) return;
        if (!rightClicked.getOrDefault(event.getPlayer(), false)) { // This means onInteract wasn't called through onEntityInteract
            onInteract(event.getPlayer(), null);
        }
        rightClicked.remove(event.getPlayer()); // Reset the flag for next interactions
    }

    @Override
    public void invalidatePlayer(Player player) {
        rightClicked.remove(player);
    }

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level <= 0) {
            return; // Skill not active
        }

        final PlayerUseSkillEvent event = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
        if (event.isCancelled()) {
            return; // Skill was cancelled
        }

        if (ent != null && UtilMath.offset(player, ent) <= 3.0) {
            Player damagee = (ent instanceof Player) ? (Player) ent : null;
            if (!(damagee != null && UtilPlayer.getRelation(player, damagee) == EntityProperty.FRIENDLY)) {
                // Apply the effect and messages
                if (damagee != null) {
                    UtilMessage.simpleMessage(damagee, getClassType().getName(), "<yellow>%s<gray> hit you with <green>%s<gray>.", player.getName(), getName() + " " + level);
                    championsManager.getEffects().addEffect(damagee, EffectType.SHOCK, (long) (getDuration(level) * 1000L));
                    championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (long) (getDuration(level) * 1000L));
                }

                UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <yellow>%s<gray> with <green>%s<gray>.", ent.getName(), getName() + " " + level);
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, DamageCause.ENTITY_ATTACK, 3 + level, false, getName()));
                ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.2F);
            } else {
                UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);
            }
        } else {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);
        }
        player.swingMainHand();
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - level * cooldownDecreasePerLevel;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 0.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
    }
}