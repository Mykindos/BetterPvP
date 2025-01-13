package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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
public class HiltSmash extends Skill implements CooldownSkill, Listener, OffensiveSkill, DamageSkill, DebuffSkill {

    private final WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();
    @Getter
    private double damage;
    @Getter
    private double duration;

    private int slowStrength;
    private double hitDistance;

    @Inject
    public HiltSmash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hilt Smash";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Smash the hilt of your sword into",
                "your opponent, dealing <val>" + getDamage() + "</val> damage and",
                "applying <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown()
        };
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
        if (event.getRightClicked() instanceof LivingEntity entity) {
            rightClicked.put(event.getPlayer(), true);
            onInteract(event.getPlayer(), entity);
            event.setCancelled(true);
        }

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

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        rightClicked.remove(player);
    }

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        if (!hasSkill(player)) {
            return;
        }

        final PlayerUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerUseSkillEvent(player, this));
        if (skillEvent.isCancelled()) {
            return;
        }

        boolean withinRange = ent != null && UtilMath.offset(player, ent) <= hitDistance;
        boolean isFriendly = false;

        if (ent instanceof Player damagee) {
            isFriendly = UtilEntity.getRelation(player, damagee) == EntityProperty.FRIENDLY;
        }

        if (ent == null || !withinRange || isFriendly) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s</green>.", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0F, 0.0F);
        } else {
            UtilMessage.simpleMessage(ent, getClassType().getName(), "<yellow>%s<gray> hit you with <green>%s<gray>.", player.getName(), getName());
            championsManager.getEffects().addEffect(ent, player, EffectTypes.SLOWNESS, slowStrength, (long) (getDuration() * 1000));
            UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <yellow>%s<gray> with <green>%s<gray>.", ent.getName(), getName());
            UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, DamageCause.ENTITY_ATTACK, getDamage(), false, getName()));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 1.2F);
        }
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 3.0, Double.class);
        duration = getConfig("duration", 0.5, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);
        hitDistance = getConfig("hitDistance", 4.0, Double.class);
    }
}
