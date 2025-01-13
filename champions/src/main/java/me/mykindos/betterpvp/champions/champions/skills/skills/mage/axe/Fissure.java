package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissureBlock;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissureCast;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.FissurePath;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;


@Singleton
@BPvPListener
public class Fissure extends Skill implements InteractSkill, CooldownSkill, Listener, DamageSkill, DebuffSkill, OffensiveSkill, WorldSkill {

    private final WorldBlockHandler blockHandler;
    @Getter
    private int fissureDistance;
    private double fissureExpireDuration;
    private double damagePerBlock;
    private double effectDuration;
    private int slownessLevel;
    private List<String> forbiddenBlockTypes;

    private final List<FissureCast> activeCasts = new ArrayList<>();

    @Inject
    public Fissure(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Fissure";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Fissure the earth in front of you,",
                "creating an impassable wall",
                "",
                "Players struck by the wall will receive",
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slownessLevel) + "</effect> for <val>" + getSlowDuration() + "</val> seconds and take",
                getDamage() + " damage for every block fissure",
                "has travelled",
                "",
                "Cooldown: <val>" + getCooldown()
        };
    }

    public double getDamage(int blocksTraveled) {
        return getDamage() * blocksTraveled;
    }

    public double getDamage() {
        return (damagePerBlock);
    }

    public double getSlowDuration() {
        return effectDuration;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public boolean canUse(Player player) {
        if (!UtilBlock.isGrounded(player)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You can only use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    public void activate(Player player) {
        activeCasts.add(createFissure(player));
    }

    public FissureCast createFissure(Player player) {
        FissureCast fissureCast = new FissureCast(this, player, getFissureDistance());
        FissurePath fissurePath = new FissurePath();
        fissurePath.createPath(fissureCast);
        fissureCast.setFissurePath(fissurePath);

        if (!fissurePath.getFissureBlocks().isEmpty()) {
            for (FissureBlock fissureBlock : fissurePath.getFissureBlocks()) {
                Block block = fissureBlock.getBlock();

                player.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
                player.getWorld().playEffect(block.getLocation().add(0, 1, 0), Effect.STEP_SOUND, block.getType());

                for (LivingEntity entity : fissureBlock.getNearbyEntities()) {
                    if (entity.getLocation().getBlockY() == block.getY() + 1 || entity.getLocation().getBlock().equals(block)) {
                        championsManager.getEffects().addEffect(entity, player, EffectTypes.SLOWNESS, slownessLevel, (long) (getSlowDuration() * 1000));
                        championsManager.getEffects().addEffect(entity, player, EffectTypes.NO_JUMP, (long) (getSlowDuration() * 1000));

                    }
                }
            }
        }

        return fissureCast;
    }

    @UpdateEvent
    public void processFissures() {
        activeCasts.removeIf(fissureCast -> {
            if (fissureCast.isFinished()) {
                return true;
            }

            fissureCast.process();
            return false;
        });
    }

    public void doBlockUpdate(FissureCast fissureCast, FissureBlock fissureBlock) {
        Block targetBlock = fissureBlock.getBlock();
        if (UtilBlock.airFoliage(targetBlock)) {
            Material materialToSet = fissureBlock.getMaterialToSet();

            blockHandler.addRestoreBlock(fissureCast.getPlayer(), targetBlock, fissureBlock.getBlockData(), materialToSet, (long) (fissureExpireDuration * 1000), true, "Fissure");
            targetBlock.getWorld().playEffect(targetBlock.getLocation(), Effect.STEP_SOUND, materialToSet);

            Location startLocation = fissureCast.getFissurePath().getStartLocation();
            Location blockLocation = fissureBlock.getBlock().getLocation();

            processCollision(fissureCast, fissureBlock, (int) UtilMath.offset2d(startLocation, blockLocation));
        }
    }

    private void processCollision(FissureCast fissureCast, FissureBlock fissureBlock, int distance) {

        for (LivingEntity livingEntity : fissureBlock.getNearbyEntities()) {
            if (fissureCast.getEntitiesHit().contains(livingEntity.getUniqueId())) continue;

            double damage = getDamage(distance);
            UtilDamage.doCustomDamage(new CustomDamageEvent(livingEntity, fissureCast.getPlayer(), null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, "Fissure"));

            UtilMessage.simpleMessage(fissureCast.getPlayer(), getClassType().getName(), "You hit <alt2>" + livingEntity.getName() + "</alt2> with <alt>" + getName());
            UtilMessage.simpleMessage(livingEntity, getClassType().getName(), "<alt2>" + fissureCast.getPlayer().getName() + "</alt2> hit you with <alt>" + getName());

            fissureCast.getEntitiesHit().add(livingEntity.getUniqueId());
        }

    }

    public boolean isForbiddenBlockType(Block block) {
        if (block.getState() instanceof Container) {
            return true;
        }

        if (block.getBlockData() instanceof Openable || block.getBlockData() instanceof Directional || block.getBlockData() instanceof Ageable) {
            return true;
        }

        if (!UtilBlock.solid(block)) {
            return true;
        }

        return forbiddenBlockTypes.stream().anyMatch(material -> block.getType().name().toLowerCase().contains(material.toLowerCase()));

    }


    @SuppressWarnings("unchecked")
    @Override
    public void loadSkillConfig() {
        fissureDistance = getConfig("fissureDistance", 14, Integer.class);
        fissureExpireDuration = getConfig("fissureExpireDuration", 10.0, Double.class);
        damagePerBlock = getConfig("extraDamagePerBlock", 0.6, Double.class);
        effectDuration = getConfig("effectDuration", 1.0, Double.class);
        slownessLevel = getConfig("slownessLevel", 2, Integer.class);

        var forbidden = List.of("TNT", "ENCHANTING_TABLE");
        forbiddenBlockTypes = getConfig("fissureForbiddenBlocks", forbidden, List.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
