package me.mykindos.betterpvp.clans.champions.skills.skills.warlock.axe;

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
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class Cleanse extends Skill implements InteractSkill, CooldownSkill, Listener {

    private int distance;
    private double duration;

    @Inject
    public Cleanse(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Cleanse";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Sacrifice " + ChatColor.GREEN + UtilMath.round(100 - ((0.50 + (level * 0.05)) * 100), 2) + "%" + ChatColor.GRAY + " of your health to purge",
                "all negative effects from yourself and allies within " + ChatColor.GREEN + (distance + level) + ChatColor.GRAY + " blocks.",
                "",
                "You and your allies also receive an immunity against",
                "negative effects for " + ChatColor.GREEN + (duration + (level / 2)) + ChatColor.GRAY + " seconds.",
                "",
                "Recharge: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }


    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        double healthReduction = 0.50 + (level * 0.05);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        if (proposedHealth <= 0.5) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You do not have enough health to use <green>%s %d<gray>", getName(), level);
            return false;
        }

        return true;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - level;
    }

    @Override
    public void activate(Player player, int level) {
        double healthReduction = 0.50 + (level * 0.05);
        double proposedHealth = player.getHealth() - (20 - (20 * healthReduction));

        player.setHealth(Math.max(0.5, proposedHealth));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 1.0f, 0.9f);
        championsManager.getEffects().addEffect(player, EffectType.IMMUNETOEFFECTS, (long) ((duration + (level / 2)) * 1000L));

        for (var data : UtilPlayer.getNearbyPlayers(player, player.getLocation(), (distance + level), EntityProperty.FRIENDLY)) {
            Player target = data.get();
            championsManager.getEffects().addEffect(target, EffectType.IMMUNETOEFFECTS, (long) ((duration + (level / 2)) * 1000L));
            UtilMessage.message(target, "Cleanse", "You were cleansed of negative by " + ChatColor.GREEN + player.getName());

        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        distance = getConfig("distance", 5, Integer.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
