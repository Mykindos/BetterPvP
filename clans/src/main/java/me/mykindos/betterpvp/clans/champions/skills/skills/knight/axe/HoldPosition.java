package me.mykindos.betterpvp.clans.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class HoldPosition extends Skill implements InteractSkill, CooldownSkill, Listener {

    @Inject
    public HoldPosition(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Hold Position";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold your position, gaining",
                "Protection II, Slow III and no",
                "knockback for " + ChatColor.GREEN + (5 + ((level - 1) * 0.5)) + ChatColor.GRAY + " seconds.",
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
        return SkillType.AXE;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;

        if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
            event.setKnockback(false);
        }
    }

    @Override
    public double getCooldown(int level) {

        return getSkillConfig().getCooldown() - ((level - 1) * 2);
    }


    @Override
    public void activate(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectType.RESISTANCE, 1, (long) (5 + ((level - 1) * 0.5)) * 1000);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int) (100 + (((level - 1) * 0.5) * 20)), 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (100 + (((level - 1) * 0.5) * 20)), 3));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1F, 0.5F);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
