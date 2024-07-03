package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class RootingAxe extends Skill implements PassiveSkill, CooldownSkill, DebuffSkill, OffensiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;
    @Inject
    public RootingAxe(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Rooting Axe";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your axe rips players downward into the earth,",
                "disrupting their movement, and stopping them",
                "from jumping for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    private double getDuration(int level) {
        return baseDuration + ((level-1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!SkillWeapons.isHolding(damager, SkillType.AXE)) return;
        if (event.getDamagee() instanceof Wither) return;
        if (!UtilBlock.isGrounded(event.getDamagee())) return;

        int level = getLevel(damager);
        if (level > 0) {
            Block block = event.getDamagee().getLocation().getBlock().getRelative(0, -1, 0);

            LivingEntity damagee = event.getDamagee();
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Slab || blockData instanceof Openable || blockData instanceof Ladder) {
                return;
            }

            Block blockMoreUnder = damagee.getLocation().getBlock().getRelative(0, -2, 0);
            if(!isRootable(blockMoreUnder)) {
                return;
            }

            Block blockUnder = damagee.getEyeLocation().getBlock().getRelative(0, -1, 0);
            if (UtilBlock.airFoliage(blockUnder) && !UtilBlock.airFoliage(blockMoreUnder)) {
                if (!UtilBlock.airFoliage(block) && !block.isLiquid() && !blockMoreUnder.isLiquid()) {

                    if (championsManager.getCooldowns().use(damager, getName(), getCooldown(level), false)) {
                        damagee.teleport(damagee.getLocation().add(0, -1, 0));
                        damagee.getWorld().playEffect(damagee.getLocation(), Effect.STEP_SOUND, damagee.getLocation().getBlock().getType());
                        championsManager.getEffects().addEffect(damagee, damager, EffectTypes.NO_JUMP, (long) (getDuration(level) * 1000));
                    }
                }
            }
        }
    }

    private boolean isRootable(Block block) {
        BlockData blockData = block.getBlockData();
        return !(blockData instanceof Slab || blockData instanceof Openable || blockData instanceof Ladder) && UtilBlock.solid(block);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 2.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
    }
}
