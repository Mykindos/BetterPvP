package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

@Singleton
@BPvPListener
public class Swim extends Skill implements PassiveSkill, EnergySkill {

    private double internalCooldown;

    @Inject
    public Swim(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Swim";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Tap crouch to Swim forwards.",
                "",
                "Energy: <val>" + getEnergy(level)};
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @EventHandler
    public void onSwim(PlayerToggleSneakEvent event) {

        Player player = event.getPlayer();

        if (!UtilBlock.isInLiquid(player)) {
            return;
        }

        if (championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
            UtilMessage.message(player, getName(), "You cannot use Swim while silenced!");
            return;
        }

        int level = getLevel(player);
        if (level > 0) {
            if (championsManager.getCooldowns().add(player, getName(), internalCooldown, false)){
                if (championsManager.getEnergy().use(player, getName(), getEnergy(level), true)) {
                    UtilVelocity.velocity(player, 0.6D, 0.2D, 0.6D, false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.3F, 2.0F);
                }
            }
        }

    }

    @Override
    public float getEnergy(int level) {

        return energy - ((level - 1) * 2);
    }

    @Override
    public void loadSkillConfig(){
        internalCooldown = getConfig("internalCooldown", 0.35, Double.class);
    }

}


