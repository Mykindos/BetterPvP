package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

@Singleton
@BPvPListener
public class Swim extends Skill implements PassiveSkill, EnergySkill, MovementSkill {

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
    public Component[] getDescription(int level) {
        Component energy = getValueComponent(this::getEnergy, level);
        return Translations.componentLines(
                "champions.skill.global.swim.description",
                energy
        );
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

        if (championsManager.getEffects().hasEffect(player, EffectTypes.SILENCE)) {
            UtilMessage.message(player, getName(), "champions.skill.global.swim.silenced");
            return;
        }

        int level = getLevel(player);
        if (level > 0) {
            if (championsManager.getCooldowns().use(player, getName(), internalCooldown, false)){
                if (championsManager.getEnergy().use(player, getName(), getEnergy(level), true)) {
                    VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), 0.6D, false, 0.0, 0.2D, 0.6D, false);
                    UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.3F, 2.0F);
                }
            }
        }

    }

    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1) * energyDecreasePerLevel));
    }

    @Override
    public void loadSkillConfig(){
        internalCooldown = getConfig("internalCooldown", 0.35, Double.class);
    }

}


