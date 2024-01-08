package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Concussion extends Skill implements CooldownSkill, Listener {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();


    @Inject
    public Concussion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Concussion";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "Your next hit will <effect>Blind</effect> the target for <val>" + (durationIncreasePerLevel * level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * level);
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
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 3);
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

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level <= 0) {
            return; // Skill not active
        }

        // Cooldown's applied in the event monitor
        final PlayerUseSkillEvent event = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
        if (event.isCancelled()) {
            return; // Skill was cancelled
        }

        if (ent != null) {
            if (UtilMath.offset(player, ent) <= 3.0) {
                if (ent instanceof Player damagee) {
                    UtilMessage.simpleMessage(ent, getName(), "You gave <alt>" + damagee.getName() + "</alt> a concussion.");
                    UtilMessage.simpleMessage(damagee, getName(), "<alt>" + ent.getName() + "</alt> gave you a concussion.");
                }
                ent.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) getDuration(level) * 20, 0));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2f, 1.0f);
            } else {
                UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
            }
        } else {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
        }
        player.swingMainHand();
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 0.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
    }
}
