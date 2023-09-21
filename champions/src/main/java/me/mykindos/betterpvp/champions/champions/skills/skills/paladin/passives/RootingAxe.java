package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class RootingAxe extends Skill implements PassiveSkill, CooldownSkill {

    private double duration;
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
                "Your axe rips players downward into",
                "the earth disrupting their movement,",
                "and stops them from jumping for <val>" + duration + "</val> seconds",
                "",
                "Internal Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!UtilPlayer.isHoldingItem(damager, SkillWeapons.AXES)) return;
        if (event.getDamagee() instanceof Wither) return;
        if (!UtilBlock.isGrounded(event.getDamagee())) return;

        int level = getLevel(damager);
        if (level > 0) {
            Block block = event.getDamagee().getLocation().getBlock().getRelative(0, -1, 0);

            LivingEntity damagee = event.getDamagee();
            if (damager.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().name().contains("_SLAB") || block.getType().name().contains("_STEP") || block.getType() == Material.OAK_SLAB || block.getType().name().contains("STAIR")) {
                return;
            }

            Block blockMoreUnder = damagee.getLocation().getBlock().getRelative(0, -2, 0);
            if (blockMoreUnder.getType().name().contains("LADDER") || blockMoreUnder.getType().name().contains("GATE")
                    || !UtilBlock.solid(blockMoreUnder.getType())) {
                return;
            }

            Block blockUnder = damagee.getEyeLocation().getBlock().getRelative(0, -1, 0);
            if (UtilBlock.airFoliage(blockUnder) && !UtilBlock.airFoliage(blockMoreUnder)) {
                if (!UtilBlock.airFoliage(block) && !block.isLiquid() && !blockMoreUnder.isLiquid()) {

                    if (championsManager.getCooldowns().add(damager, getName(), 11 - (level * 1.5), false)) {
                        damagee.teleport(damagee.getLocation().add(0, -0.9, 0));
                        damagee.getWorld().playEffect(damagee.getLocation(), Effect.STEP_SOUND, damagee.getLocation().getBlock().getType());
                        damagee.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) duration * 20, -5));
                    }
                }
            }
        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 2);
    }

    public void loadSkillConfig() {
        duration = getConfig("duration", 2.0, Double.class);
    }
}
