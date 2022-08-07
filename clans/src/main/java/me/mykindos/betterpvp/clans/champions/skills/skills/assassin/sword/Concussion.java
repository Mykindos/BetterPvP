package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.sword;


import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@BPvPListener
public class Concussion extends PrepareSkill implements CooldownSkill, Listener {

    @Inject
    public Concussion(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Concussion";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to activate.",
                "",
                "Your next hit blinds the target for " + ChatColor.GREEN + (level) + ChatColor.GRAY + " seconds.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
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
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill() == this) {
            active.remove(event.getPlayer().getUniqueId());
        }
    }


    @Override
    public double getCooldown(int level) {

        return getSkillConfig().getCooldown() - ((level - 1) * 3);
    }

    @EventHandler
    public void onDamage(CustomDamageEvent e) {
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getDamagee() instanceof Player damagee)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        if (active.contains(damager.getUniqueId())) {
            e.setReason("Concussion");
            damagee.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (level * 20) + 20, 0));
            UtilMessage.message(damager, getName(), "You gave " + ChatColor.GREEN + damagee.getName() + ChatColor.GRAY + " a concussion.");
            UtilMessage.message(damagee, getName(), ChatColor.GREEN + damager.getName() + ChatColor.GRAY + " gave you a concussion.");
            active.remove(damager.getUniqueId());
        }

    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) {
            if (e.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            UtilMessage.message(player, getClassType().getName(), ChatColor.GREEN + getName() + ChatColor.GRAY + " is already active.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
