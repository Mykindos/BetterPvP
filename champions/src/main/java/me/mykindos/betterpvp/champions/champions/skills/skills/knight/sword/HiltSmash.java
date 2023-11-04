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
import me.mykindos.betterpvp.core.utilities.*;
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
public class HiltSmash extends Skill implements CooldownSkill, Listener {

    private WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();

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
                "your opponent, dealing <val>" + (3 + (level)) + "</val> damage,",
                "applying <effect>Shock</effect> for <val>" + (level / 2.0) + "</val> seconds,",
                "and <effect>Silence</effect> the enemy for <val>" + (level / 2.0) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
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
        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) return;

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
                    UtilMessage.simpleMessage(damagee, getClassType().getName(), "<yellow>%s<gray> hit you with <green>%s<gray>.", player.getName(), getName() + " " + level);
                    championsManager.getEffects().addEffect(damagee, EffectType.SHOCK, (level * 1000L) / 2);
                    championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (((level * 1000L) / 2)));
                }

                UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <yellow>%s<gray> with <green>%s<gray>.",
                        ent.getName(), getName() + " " + level);

                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, player, null, DamageCause.ENTITY_ATTACK, 3 + level, false, getName()));
                ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.2F);
            } else {
                UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1F, 0.1F);
            }
        } else {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s", getName());
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1F, 0.1F);
        }
        player.swingMainHand();
    }

    @Override
    public double getCooldown(int level) {
        return cooldown;
    }
}