package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;


@Singleton
@BPvPListener
public class MarkedForDeath extends PrepareArrowSkill {


    private double baseDuration;

    @Inject
    public MarkedForDeath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Marked for Death";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your next arrow will mark players",
                "for death, giving them Vulnerability I",
                "for " + ChatColor.GREEN + (baseDuration + level) + ChatColor.GRAY + " seconds",
                "Causing them to take 25% additional damage",
                "from all targets.",
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
        return SkillType.BOW;
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        if (!(target instanceof Player damagee)) return;

        championsManager.getEffects().addEffect(damagee, EffectType.VULNERABILITY, 1, (long) ((baseDuration + level) * 1000L));
        UtilMessage.message(damagee, getClassType().getName(), "%s hit you with %s",
                ChatColor.YELLOW + damager.getName() + ChatColor.GRAY, ChatColor.GREEN + getName());
    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(5, 5, 5).count(3).extra(0).receivers(60, true).spawn();
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 2);
    }

    @Override
    public boolean isCancellable() {
        return true;
    }


    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseDuration = getConfig("baseDuration", 6.0, Double.class);
    }
}
