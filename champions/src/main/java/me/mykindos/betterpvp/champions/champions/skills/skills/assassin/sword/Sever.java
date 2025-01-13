package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
    @Getter
    private double duration;

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
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Inflict <effect>Bleed</effect> for <val>" + getDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.BLEED.getDescription(0)
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

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        if (!hasSkill(player)) {
            return;
        }

        final PlayerUseSkillEvent event = UtilServer.callEvent(new PlayerUseSkillEvent(player, this));
        if (event.isCancelled()) {
            return;
        }

        if (ent == null) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <alt>%s</alt>.", getName());
            return;
        }

        boolean withinRange = UtilMath.offset(player, ent) <= hitDistance;
        if (UtilPlayer.isCreativeOrSpectator(ent) || UtilEntity.getRelation(player, ent) == EntityProperty.FRIENDLY || !withinRange) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <alt>%s</alt>.", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
        } else {
            championsManager.getEffects().addEffect(ent, player, EffectTypes.BLEED, 1, (long) (getDuration() * 1000L));
            UtilMessage.simpleMessage(player, getClassType().getName(), "You severed <alt>" + ent.getName() + "</alt>.");
            UtilMessage.simpleMessage(ent, getClassType().getName(), "You have been severed by <alt>" + player.getName() + "</alt>.");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 1.5F);
        }

    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 1.0, Double.class);
        hitDistance = getConfig("hitDistance", 4.0, Double.class);
    }
}
