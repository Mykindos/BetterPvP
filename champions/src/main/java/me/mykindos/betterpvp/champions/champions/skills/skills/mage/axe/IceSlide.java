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
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
@BPvPListener
public class IceSlide extends Skill implements InteractSkill, EnergySkill, MovementSkill, DebuffSkill, ThrowableListener, Listener {


    // TODO rename
    private double velocityStrength;
    private int freezeStrength;
    private double duration;

    private final Map<UUID, CurrentSkillState> currentlyUsingSkill = new HashMap<>();

    enum CurrentSkillState {
        WIND_UP,
        SLIDING
    }

    private final SoundEffect SFX = new SoundEffect(Sound.BLOCK_GLASS_STEP, 1.5F, 2.0F);

    @Inject
    public IceSlide(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public float getEnergy() {
        return (float) energy;
    }

    @Override
    public boolean canUse(Player player) {

        // might be able to remove this restriction
        if (!UtilBlock.isGrounded(player, 2)) {
            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player) {

        // playerUUID never changes so it is safe to assign it here
        final UUID playerUUID = player.getUniqueId();
        currentlyUsingSkill.put(playerUUID, CurrentSkillState.WIND_UP);
        final long windUpAnimationLength = 10L;  // (in ticks)

        // Same delay as wind-up animation
        championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, 50L * windUpAnimationLength);

        // Wind-up animation
        for (long delay = 1L; delay <= windUpAnimationLength; delay++) {
            UtilServer.runTaskLater(champions, () -> {
                Location playerLocForWindUpParticle = player.getLocation().add(0, 1, 0);

                double directionMod = 1.0;

                // Use clone as to not re-use the same Location object
                Location posX = playerLocForWindUpParticle.clone().add(directionMod, 0, 0);
                Location negX = playerLocForWindUpParticle.clone().subtract(directionMod, 0, 0);
                Location posZ = playerLocForWindUpParticle.clone().add(0, 0, directionMod);
                Location negZ = playerLocForWindUpParticle.clone().subtract(0, 0, directionMod);

                // Spawn wind-up particles around the player in all 4 cardinal directions
                Stream.of(posX, negX, posZ, negZ).forEach(loc -> Particle.CLOUD.builder()
                        .extra(0)
                        .location(loc)
                        .receivers(60)
                        .spawn());

                SFX.play(player.getLocation());
            }, delay);
        }

        UtilServer.runTaskLater(champions, () -> {
            currentlyUsingSkill.put(playerUUID, CurrentSkillState.SLIDING);

            final Location playerLoc = player.getLocation();
            final Vector vec = playerLoc.getDirection();
            final VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 0.0D, 0.0D, 0.0D, true);

            UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

            UtilServer.runTaskLater(champions, () -> {
                currentlyUsingSkill.remove(playerUUID);
            }, 10L);

        }, windUpAnimationLength);
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent event) {
        if (event.isCancelled()) return;
        return;
    }

    @UpdateEvent
    public void monitorActives() {
        currentlyUsingSkill.keySet().removeIf(playerUUID -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) return true;

            // If they don't have the skill, then they cant be using it
            return !hasSkill(player);
        });
    }

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
                    Location playerLocation = player.getLocation();

                    Item blueIce = world.dropItem(playerLocation.add(0.0D, 0.25D, 0.0D), new ItemStack(Material.BLUE_ICE));
                    ThrowableItem throwableItem = new ThrowableItem(this, blueIce, player, getName(), (long) (duration * 1000L));
                    throwableItem.setRemoveInWater(true);
                    championsManager.getThrowables().addThrowable(throwableItem);

                    blueIce.setVelocity(new Vector((Math.random() - 0.5D), Math.random() / 5D, (Math.random() - 0.5D)));

                    world.playSound(playerLocation, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                }
            }
        });
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (!(thrower instanceof Player)) return;
        if (hit.getFreezeTicks() > 0) return;

        championsManager.getEffects().addEffect(hit, EffectTypes.FREEZE, freezeStrength, 2*1000L);
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
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Dash forward leaving an icy trail",
                "behind you that <effect>Freezes</effect> enemies",
                "and lingers for <val>" + UtilFormat.formatNumber(duration) + "</val> seconds",
                "",
                "Energy Cost: <val>" + UtilFormat.formatNumber(energy) + "</val>",
                "",
                EffectTypes.FREEZE.getDescription(0)
        };
    }

    @Override
    public void loadSkillConfig() {
        velocityStrength = getConfig("velocityStrength ", 3.1, Double.class);
        freezeStrength = getConfig("freezeStrength", 1, Integer.class);
        duration = getConfig("duration", 4.0, Double.class);
    }
}