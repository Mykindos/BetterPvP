package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.sword;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class Sever extends PrepareSkill implements CooldownSkill, Listener {

    @Inject
    public Sever(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Sever";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to activate",
                "",
                "Your next hit applies a " + ChatColor.GREEN + (level) + ChatColor.GRAY + " second bleed",
                "dealing 1 heart per second",
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
    public void onChange(RoleChangeEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {

        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!active.contains(player.getUniqueId())) return;
        if (!UtilPlayer.isHoldingItem(player, getItemsBySkillType())) return;

        // TODO
        //Weapon w = WeaponManager.getWeapon(player.getInventory().getItemInMainHand());
        //if (w != null && w instanceof ILegendary) return;

        int level = getLevel(player);
        runSever(player, damagee, level);
        UtilMessage.message(player, getClassType().getName(), "You severed " + ChatColor.GREEN + damagee.getName() + ChatColor.GRAY + ".");
        UtilMessage.message(damagee, getClassType().getName(), "You have been severed by " + ChatColor.GREEN + player.getName() + ChatColor.GRAY + ".");
        active.remove(player.getUniqueId());

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().add(player, getName(), getCooldown(level), showCooldownFinished());
    }

    private void runSever(Player damager, Player damagee, int level) {
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= (level) || damagee == null || damager == null || damagee.getHealth() <= 0) {
                    this.cancel();
                } else {
                    if (championsManager.getCooldowns().add(damagee, "Sever-Damage", 0.75, false)) {
                        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null,
                                DamageCause.CUSTOM, 1.5, false, "Sever"));
                    }
                    count++;
                }
            }
        }.runTaskTimer(clans, 20, 20);
    }


    @Override
    public double getCooldown(int level) {
        return cooldown;
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
