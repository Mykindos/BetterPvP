package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class PinDown extends PrepareArrowSkill {

    @Inject
    public PinDown(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Pin Down";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a bow to instantly fire",
                "an arrow, which gives anybody hit ",
                "Slowness IV for " + net.md_5.bungee.api.ChatColor.GREEN + (level * 1.5) + net.md_5.bungee.api.ChatColor.GRAY + " seconds.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        UtilInventory.remove(player, Material.ARROW, 1);

        Arrow proj = player.launchProjectile(Arrow.class);
        arrows.add(proj);

        proj.setVelocity(player.getLocation().getDirection().multiply(1.6D));
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((level * 1.5) * 20), 3));
    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(128, 0, 128).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return 13 - ((level - 1) * 1.5);
    }

}
