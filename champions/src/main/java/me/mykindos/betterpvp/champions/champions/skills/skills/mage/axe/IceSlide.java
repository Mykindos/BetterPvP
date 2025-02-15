package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * This skill is designed to give Mage more movement and area-denial.
 * But, this skill's movement is not as immediate as other kits because
 * there is a wind-up animation played before the ice slide activates to
 * make it more visually appealing.
 */
@Singleton
@BPvPListener
public class IceSlide extends Skill implements InteractSkill, EnergySkill, MovementSkill, DebuffSkill, ThrowableListener, Listener {

    /**
     * Determines how fast the player slides/moves.
     * Higher velocity means the player will slide further.
     * Like all other velocity config values in this plugin, this value is not mentioned to the user.
     */
    private double velocityStrength;

    /**
     * At the moment, {@link me.mykindos.betterpvp.core.effects.types.negative.FreezeEffect}'s level
     * does not matter; this is simply here to future-proof
     */
    private int freezeStrength;

    /**
     * Represents the duration that {@link me.mykindos.betterpvp.core.effects.types.negative.FreezeEffect} will be
     * applied to an enemy for.
     * This value is NOT mentioned in the skill description because the user can find out by simply using the
     * skill; not everything has to be told to them.
     * This value is measured in seconds.
     */
    private double freezeDuration;

    /**
     * Represents how long the icy trail that follows the player will linger (or stay-around) for.
     * This value is measured in seconds.
     */
    private double trailLingerDuration;

    /**
     * Represents how long the wind-up stage/animation of this skill will last; can be arbitrarily long.
     * This value is measured in ticks.
     */
    private long windUpAnimationLength;

    /**
     * Represents how much the wind-up animation's {@link Location}'s will be offset by.
     * Higher values means the animation happens farther out from the player.
     */
    private double windUpAnimationOffset;

    /**
     * Represents whether this skill's velocity component should apply a ground boost during the slide
     */
    private boolean doGroundBoost;

    /**
     * Represents how long we want the icy trail to keep generating for. More specifically, this is just the value
     * determines how long we keep the player in the currentlyUsingSkill Map.
     * This value is measured in ticks.
     */
    private long slidingStageLength;

    /**
     * Maps a player's unique id to a {@link CurrentSkillState}.
     * <li>The key represents a player's {@link UUID}</li>
     * <li>The value represents what stage they have reached using this skill</li>
     */
    private final Map<UUID, CurrentSkillState> currentlyUsingSkill = new HashMap<>();

    /**
     * Represents the stages that are reached when using this skill.
     */
    enum CurrentSkillState {
        WIND_UP,
        SLIDING
    }

    /**
     * This sound effect is played when the user is in the <b>wind-up</b> animation of this skill
     */
    private final SoundEffect WIND_UP_SFX = new SoundEffect(Sound.BLOCK_GLASS_STEP, 1.5F, 2.0F);

    /**
     * This sound effect is played when the user is in the <b>sliding</b> animation of this skill
     */
    private final SoundEffect SLIDING_SFX = new SoundEffect(Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);

    @Inject
    public IceSlide(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Dash forward leaving an icy trail",
                "behind you that <effect>Freezes</effect> enemies",
                "and lingers for <val>" + UtilFormat.formatNumber(trailLingerDuration) + "</val> seconds",
                "",
                "Energy Cost: <val>" + UtilFormat.formatNumber(energy) + "</val>",
                "",
                EffectTypes.FREEZE.getDescription(0)
        };
    }

    @Override
    public float getEnergy() {
        return (float) energy;
    }

    @Override
    public boolean canUse(Player player) {

        // This restriction is here because we don't want players jumping before the skill is used or during the
        // wind-up/sliding stages because then the skill won't be a "slide"
        if (!UtilBlock.isGrounded(player, 2)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player) {
        // playerUUID will never change, so it is safe to assign it here
        final UUID playerUUID = player.getUniqueId();
        currentlyUsingSkill.put(playerUUID, CurrentSkillState.WIND_UP);

        // Effect duration is measured in milliseconds while windUpAnimationLength is measured in ticks; thus,
        // that is why that number it is being multiplied by was chosen
        long windUpAnimationNoJumpDuration = 50L * windUpAnimationLength;

        // Same delay as wind-up animation
        championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, windUpAnimationNoJumpDuration);

        // Wind-up animation
        for (long delay = 1L; delay <= windUpAnimationLength; delay++) {
            UtilServer.runTaskLater(champions, () -> {
                Location playerLocForWindUpParticle = player.getLocation().add(0, 1, 0);

                // Use clone as to not re-use the same Location object
                Location posX = playerLocForWindUpParticle.clone().add(windUpAnimationOffset, 0, 0);
                Location negX = playerLocForWindUpParticle.clone().subtract(windUpAnimationOffset, 0, 0);
                Location posZ = playerLocForWindUpParticle.clone().add(0, 0, windUpAnimationOffset);
                Location negZ = playerLocForWindUpParticle.clone().subtract(0, 0, windUpAnimationOffset);

                // Spawn wind-up particles around the player in all 4 cardinal directions
                Stream.of(posX, negX, posZ, negZ).forEach(loc -> Particle.CLOUD.builder()
                        .extra(0)
                        .location(loc)
                        .receivers(60)
                        .spawn());

                WIND_UP_SFX.play(player.getLocation());
            }, delay);
        }

        // Sliding stage of skill
        UtilServer.runTaskLater(champions, () -> {
            currentlyUsingSkill.put(playerUUID, CurrentSkillState.SLIDING);

            final Vector vec = player.getLocation().getDirection();
            final VelocityData velocityData = new VelocityData(
                    vec, velocityStrength, false, 0.0D, 0.0D, 0.0D, doGroundBoost
            );

            UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

            // End skill usage entirely
            UtilServer.runTaskLater(champions, () -> currentlyUsingSkill.remove(playerUUID), slidingStageLength);

            // We want to run this right after the wind-up ends
        }, windUpAnimationLength);
    }

    /**
     * This update event's purpose is to monitor every player that is currently using the skill and determine
     * if they should be removed from the currentlyUsingSkill Map.
     */
    @UpdateEvent
    public void monitorActives() {
        currentlyUsingSkill.keySet().removeIf(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return true;

            // If they don't have the skill, then they cant be using it
            return !hasSkill(player);
        });
    }

    /**
     * This update event's purpose is to look through every player that is in the sliding stage of this skill and
     * apply {@link me.mykindos.betterpvp.core.effects.types.negative.NoJumpEffect} to them as well as spawn ab
     * icy trail behind them (the user)
     */
    @UpdateEvent
    public void spawnIcyTrailAndApplyNoJump() {
        currentlyUsingSkill.keySet().forEach(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);

            // If null, it will get removed in `monitorActives`
            if (player != null) {

                if (currentlyUsingSkill.get(playerUUID).equals(CurrentSkillState.SLIDING)) {

                    //Since updateEvent runs every 50ms, this well be refreshed every (other?) update event
                    championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, 100L);

                    World world = player.getWorld();
                    Location playerLoc = player.getLocation();

                    // omfg do you know how good these next 10 lines would look in kotlin??!?!
                    Item iceThrowable = world.dropItem(playerLoc.add(0.0D, 0.25D, 0.0D), new ItemStack(Material.BLUE_ICE));
                    ThrowableItem throwableItem = new ThrowableItem(
                            this, iceThrowable, player, getName(), (long) (trailLingerDuration * 1000L)
                    );

                    throwableItem.setRemoveInWater(true);
                    championsManager.getThrowables().addThrowable(throwableItem);

                    iceThrowable.setVelocity(new Vector((Math.random() - 0.5D), Math.random() / 5D, (Math.random() - 0.5D)));

                    SLIDING_SFX.play(playerLoc);
                }
            }
        });
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (!(thrower instanceof Player)) return;
        if (hit.getFreezeTicks() > 0) return;

        championsManager.getEffects().addEffect(hit, EffectTypes.FREEZE, freezeStrength, (long) (freezeDuration*1000L));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
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
    public String getName() {
        return "Ice Slide";
    }

    @Override
    public void loadSkillConfig() {
        velocityStrength = getConfig("velocityStrength", 3.1, Double.class);
        freezeStrength = getConfig("freezeStrength", 1, Integer.class);
        freezeDuration = getConfig("freezeDuration", 1.0, Double.class);
        trailLingerDuration = getConfig("trailLingerDuration", 4.0, Double.class);
        windUpAnimationLength = getConfig("windUpAnimationLength", 10L, Long.class);
        windUpAnimationOffset = getConfig("windUpAnimationOffset ", 1.0, Double.class);
        doGroundBoost = getConfig("doGroundBoost", true, Boolean.class);
        slidingStageLength = getConfig("slidingStageLength", 10L, Long.class);
    }
}