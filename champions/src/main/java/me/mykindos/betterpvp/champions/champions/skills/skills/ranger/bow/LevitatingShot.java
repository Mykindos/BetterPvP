package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class LevitatingShot extends PrepareArrowSkill implements OffensiveSkill, DebuffSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int levitationStrength;

    @Inject
    public LevitatingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Levitating Shot";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow is tipped with mysterious magic causing",
                "the target to receive <effect>Levitation " + UtilFormat.getRomanNumeral(levitationStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Players with levitation are unable to use abilities",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.LEVITATION.getDescription(levitationStrength),
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {

        championsManager.getEffects().addEffect(target, damager, EffectTypes.LEVITATION, levitationStrength, (int) (getDuration(level) * 1000));
        UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>.", target.getName(), getName(), level));
        UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));

    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.ENCHANTMENT_TABLE)
                .location(location)
                .count(3)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);

        levitationStrength = getConfig("levitationStrength", 4, Integer.class);
    }
}
