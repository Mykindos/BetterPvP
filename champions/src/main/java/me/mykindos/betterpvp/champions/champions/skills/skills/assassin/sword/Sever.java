package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

@Singleton
@BPvPListener
public class Sever extends PrepareSkill implements CooldownSkill, Listener {

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
                "Right click with a Sword to prepare",
                "",
                "Your next hit applies a <val>" + (level) + "</val> second <effect>bleed</effect>",
                "dealing <stat>1</stat> heart per second",
                "",
                "Cooldown: <val>" + getCooldown(level)
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
        UtilMessage.simpleMessage(player, getClassType().getName(), "You severed <alt>" + damagee.getName() + "</alt>.");
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "You have been severed by <alt>" + player.getName() + "</alt>.");
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
                                DamageCause.CUSTOM, 1.5, false, getName()));
                    }
                    count++;
                }
            }
        }.runTaskTimer(champions, 20, 20);
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
