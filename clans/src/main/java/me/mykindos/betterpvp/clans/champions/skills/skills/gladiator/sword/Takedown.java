package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Takedown extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();

    @Inject
    public Takedown(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Takedown";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to activate.",
                "",
                "Hurl yourself towards an opponent.",
                "If you collide with them, you " + ChatColor.WHITE + "both",
                "take damage and receive Slow 4",
                "for " + ChatColor.GREEN + (1 + level) + ChatColor.GRAY + " seconds.",
                "",
                "Cannot be used while grounded.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2);
    }


    @UpdateEvent
    public void checkCollision() {

        Iterator<Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (player.isDead()) {
                it.remove();
                continue;
            }

            if(isCollision(player)) {
                it.remove();
                continue;
            }


            if (UtilBlock.isGrounded(player)) {
                if (!player.hasPotionEffect(PotionEffectType.SLOW)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 2));
                }
                if (UtilTime.elapsed(next.getValue(), 750L)) {
                    it.remove();
                }
            }
        }

    }

    public boolean isCollision(Player player) {
        for (Player other : UtilPlayer.getNearbyEnemies(player, player.getLocation(), 1.5)) {
            if (other.isDead()) continue;

            if (UtilMath.offset(player, other) < 1.5) {

                doTakedown(player, other);
                return true;

            }
        }

        return false;
    }


    public void doTakedown(Player player, Player target) {
        UtilMessage.message(player, getClassType().getName(), "You hit " + ChatColor.GREEN + target.getName() + ChatColor.GRAY + " with " + ChatColor.GREEN + getName());

        UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, 10, false, "Takedown"));


        UtilMessage.message(target, getClassType().getName(), ChatColor.GREEN + player.getName() + ChatColor.GRAY + " hit you with " + ChatColor.GREEN + getName());
        UtilDamage.doCustomDamage(new CustomDamageEvent(player, target, null, DamageCause.CUSTOM, 10, false, "Takedown Recoil"));

        PotionEffect pot = new PotionEffect(PotionEffectType.SLOW, (int) (1 + (getLevel(player) * 0.5)) * 20, 2);
        player.addPotionEffect(pot);
        target.addPotionEffect(pot);
    }

    @Override
    public boolean canUse(Player p) {

        if (UtilBlock.isGrounded(p)) {
            UtilMessage.message(p, getClassType().getName(), "You cannot use " + ChatColor.GREEN + getName() + ChatColor.GRAY + " while grounded.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int leel) {
        Vector vec = player.getLocation().getDirection();
        UtilVelocity.velocity(player, vec, 1.8D, false, 0.0D, 0.4D, 0.6D, false);
        active.put(player, System.currentTimeMillis());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
