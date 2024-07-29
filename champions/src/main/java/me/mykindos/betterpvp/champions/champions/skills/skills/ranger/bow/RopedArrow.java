package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RopedArrow extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill {

    private final TaskScheduler taskScheduler;

    private double fallDamageLimit;
    private double velocityStrength;
    private final WeakHashMap<Arrow, Player> arrows = new WeakHashMap<>();

    @Inject
    public RopedArrow(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Roped Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to activate",
                "",
                "Your next arrow will pull you",
                "towards the location it hits",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
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
    public boolean canUse(Player player) {
        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            UtilMessage.message(player, getName(), "You need at least <alt2>1 Arrow</alt2> to use this skill.");
            return false;
        }

        return super.canUse(player);
    }

    @Override
    public void activate(Player player, int level) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.CROSSBOW) {
            CrossbowMeta crossbowMeta = (CrossbowMeta) itemInHand.getItemMeta();
            if (crossbowMeta == null || crossbowMeta.getChargedProjectiles().isEmpty()) {
                UtilMessage.message(player, getName(), "Your crossbow must be loaded to use this skill.");
                return;
            }
            crossbowMeta.setChargedProjectiles(null);
            itemInHand.setItemMeta(crossbowMeta);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0F, 1.0F);
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            UtilInventory.remove(player, Material.ARROW, 1);
        }


        Arrow proj = player.launchProjectile(Arrow.class);
        proj.setShooter(player);
        arrows.put(proj, player);

        proj.setVelocity(player.getLocation().getDirection().multiply(1.6D));
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.containsKey(arrow)) return;
        if (!hasSkill(player)) return;

        Vector vec = UtilVelocity.getTrajectory(player, arrow);

        VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 1.0D, 0.5D, 1.2D, true);
        UtilVelocity.velocity(player, null, velocityData);

        arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        arrows.remove(arrow);

        taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
            Player target = Bukkit.getPlayer(uuid);
            if(target != null) {
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                        50L, true, true, UtilBlock::isGrounded);
            }
        }, 1000));

    }

    @UpdateEvent
    public void onTick() {
        arrows.entrySet().removeIf(entry -> {
            final Arrow arrow = entry.getKey();
            final Player shooter = entry.getValue();
            if (arrow.isDead() || arrow.isOnGround() || shooter == null || !shooter.isOnline()) {
                return true;
            }

            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1);
            new ParticleBuilder(Particle.DUST)
                    .location(arrow.getLocation())
                    .count(1)
                    .offset(0.1, 0.1, 0.1)
                    .extra(0)
                    .receivers(60)
                    .data(dustOptions)
                    .spawn();

            return false;
        });
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public void loadSkillConfig() {
        fallDamageLimit = getConfig("fallDamageLimit", 8.0, Double.class);
        velocityStrength = getConfig("velocityStrength", 2.0, Double.class);
    }
}
