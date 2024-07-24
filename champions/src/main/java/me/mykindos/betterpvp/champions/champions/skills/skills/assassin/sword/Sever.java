package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Sever extends Skill implements CooldownSkill, Listener, OffensiveSkill, DebuffSkill {
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double hitDistance;
    private WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();

    @Inject
    public Sever(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sever";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Inflict <effect>Bleed</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.BLEED.getDescription(0)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
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
        if (UtilBlock.usable(event.getClickedBlock())) return;
        if (!rightClicked.getOrDefault(event.getPlayer(), false)) { // This means onInteract wasn't called through onEntityInteract
            if (championsManager.getCooldowns().hasCooldown(event.getPlayer(), "DoorAccess")) return;
            onInteract(event.getPlayer(), null);
        }
        rightClicked.remove(event.getPlayer()); // Reset the flag for next interactions
    }

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level <= 0) {
            return;
        }

        final PlayerUseSkillEvent event = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
        if (event.isCancelled()) {
            return;
        }

        if (ent == null) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s %s", getName(), level);
            return;
        }

        boolean withinRange = UtilMath.offset(player, ent) <= hitDistance;
        if (UtilPlayer.isCreativeOrSpectator(ent) || UtilEntity.getRelation(player, ent) == EntityProperty.FRIENDLY || !withinRange) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s %s", getName(), level);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
        } else {
            championsManager.getEffects().addEffect(ent, player, EffectTypes.BLEED, 1, (long) (getDuration(level) * 1000L));
            UtilMessage.simpleMessage(player, getClassType().getName(), "You severed <alt>" + ent.getName() + "</alt>.");
            UtilMessage.simpleMessage(ent, getClassType().getName(), "You have been severed by <alt>" + player.getName() + "</alt>.");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 1.5F);
        }

    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        hitDistance = getConfig("hitDistance", 4.0, Double.class);
    }
}
