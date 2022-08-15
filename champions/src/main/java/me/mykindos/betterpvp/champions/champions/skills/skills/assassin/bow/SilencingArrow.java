package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.bow;

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

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@BPvPListener
public class SilencingArrow extends PrepareArrowSkill {

    @Inject
    public SilencingArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Silencing Arrow";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{"Your next arrow will silence your", "target for " + ChatColor.GREEN + (3 + level) + ChatColor.GRAY + " seconds.", "Making them unable to use any active skills", "", "Cooldown: " + ChatColor.GREEN + getCooldown(level)

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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 0.5);
    }



    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        if (!(target instanceof Player damagee)) return;
        championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (3 + level * 1000L));
        if (championsManager.getEffects().hasEffect(damagee, EffectType.IMMUNETOEFFECTS)) {
            UtilMessage.message(damager, getClassType().getName(), ChatColor.GREEN + damagee.getName() + ChatColor.GRAY + " is immune to your silence!");
        }
    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(125, 0, 125).count(3).extra(0).receivers(60, true).spawn();
    }


    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }
}
