package me.mykindos.betterpvp.clans.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;


@Singleton
@BPvPListener
public class HiltSmash extends Skill implements CooldownSkill, Listener {

    @Inject
    public HiltSmash(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Hilt Smash";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a sword to activate.",
                "",
                "Smash the hilt of your sword into",
                "your opponent, dealing " + ChatColor.GREEN + (3 + (level)) + ChatColor.GRAY + " damage",
                "and applying shock for " + ChatColor.GREEN + (level / 2) + ChatColor.GRAY + " seconds.",
                "Silences enemy for " + ChatColor.GREEN + (level / 2) + ChatColor.GRAY + " seconds",
                "",
                "Recharge: " + ChatColor.GREEN + getCooldown(level)
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
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) return;


        int level = getLevel(player);
        if (level > 0) {

            if (event.getRightClicked() instanceof LivingEntity ent) {
                if (UtilMath.offset(player, ent) <= 3.0) {

                    PlayerUseSkillEvent playerUseSkillEvent = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
                    if(playerUseSkillEvent.isCancelled()) return;

                    if (ent instanceof Player damagee) {
                        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<yellow>%s<gray> hit you with <green>%s<gray>.",
                                player.getName(), getName() + " " + level);

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
            }
        }

    }

    @Override
    public double getCooldown(int level) {
        return cooldown;
    }
}
