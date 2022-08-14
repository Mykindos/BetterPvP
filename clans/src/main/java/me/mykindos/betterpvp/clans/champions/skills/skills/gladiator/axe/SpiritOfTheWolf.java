package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.axe;

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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
public class SpiritOfTheWolf extends Skill implements InteractSkill, CooldownSkill {

    private int radius;
    private double duration;

    @Inject
    public SpiritOfTheWolf(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Spirit of the Wolf";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a axe to activate.",
                "",
                "Call upon the spirit of the wolf",
                "granting all allies within " + ChatColor.GREEN + (radius + (level)) + ChatColor.GRAY + " blocks",
                "Speed II for " + ChatColor.GREEN + duration + ChatColor.GRAY + " seconds.",
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

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }


    @Override
    public void activate(Player player,int level) {
        player.getWorld().playSound(player.getLocation().add(0.0, -1.0, 0.0), Sound.ENTITY_WOLF_HOWL, 0.5F, 1.0F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 1));

        for (Player target : UtilPlayer.getNearbyAllies(player, player.getLocation(), (radius + level))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), 1));
            UtilMessage.message(target, getClassType().getName(), "You received the spirit of the wolf!");
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 5, Integer.class);
        duration = getConfig("duration", 9.0, Double.class);
    }
}
