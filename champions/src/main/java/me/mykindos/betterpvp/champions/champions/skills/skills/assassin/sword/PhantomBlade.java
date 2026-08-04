package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.PhantomBladeProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class PhantomBlade extends Skill implements InteractSkill, CooldownSkill, Listener, MovementSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, PhantomBladeProjectile> blades = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> recallTime = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double speed;
    private double flightDuration;
    private double hitboxSize;
    private double anchorDuration;
    private double hitWindow;
    private double baseCooldownReduction;
    private double cooldownReductionPerLevel;

    @Inject
    public PhantomBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Phantom Blade";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component anchor = getValueComponent(this::getAnchorDuration, level);
        Component window = getValueComponent(this::getHitWindow, level);
        Component reduction = getValueComponent(this::getCooldownReduction, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.assassin.phantom-blade.description",
                damage,
                anchor,
                window,
                reduction,
                cooldown
        );
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getAnchorDuration(int level) {
        return anchorDuration;
    }

    public double getHitWindow(int level) {
        return hitWindow;
    }

    public double getCooldownReduction(int level) {
        return baseCooldownReduction + ((level - 1) * cooldownReductionPerLevel);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    /**
     * The second click of the skill recalls to a blade that is already in the world, so it has to bypass the
     * cooldown that the throw put the skill on.
     */
    @Override
    public boolean canUse(Player player) {
        final PhantomBladeProjectile blade = blades.get(player);
        if (blade == null) {
            return true;
        }

        recall(player, blade);
        return false;
    }

    @Override
    public boolean activate(Player player, int level) {
        final ItemStack blade = player.getInventory().getItemInMainHand();
        final Location origin = player.getEyeLocation().add(player.getLocation().getDirection());

        final PhantomBladeProjectile projectile = new PhantomBladeProjectile(
                player,
                hitboxSize,
                origin,
                (long) (flightDuration * 1000),
                (long) (getAnchorDuration(level) * 1000),
                blade,
                getDamage(level),
                this
        );
        projectile.redirect(player.getLocation().getDirection().multiply(speed));
        blades.put(player, projectile);

        new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.6f, 0.8f).play(origin);
        new SoundEffect(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.6f).play(origin);
        return true;
    }

    private void recall(Player player, PhantomBladeProjectile blade) {
        final Location from = player.getLocation().clone();
        final Location target = blade.getAnchorLocation();
        final Vector direction = target.toVector().subtract(from.toVector());
        final int level = getLevel(player);

        UtilLocation.teleportToward(player, direction, direction.length(), false, success -> {
            if (!Boolean.TRUE.equals(success)) {
                UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.failed",
                        getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
                return;
            }

            discard(player);
            recallTime.put(player, System.currentTimeMillis());

            drawRecallTrail(from.add(0, player.getHeight() / 2, 0), player.getLocation().add(0, player.getHeight() / 2, 0));
            new SoundEffect(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.8f).play(from);
            new SoundEffect(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 0.8f).play(player.getLocation());
        });
    }

    private void drawRecallTrail(Location from, Location to) {
        final Vector step = to.toVector().subtract(from.toVector());
        final double distance = step.length();
        if (distance <= 0) {
            return;
        }

        step.normalize().multiply(0.4);
        final Location point = from.clone();
        for (double travelled = 0; travelled < distance; travelled += 0.4) {
            final Color color = Math.random() > 0.5 ? Color.fromRGB(184, 56, 207) : Color.fromRGB(174, 52, 179);
            Particle.DUST.builder()
                    .location(point)
                    .count(1)
                    .extra(0.5)
                    .data(new Particle.DustOptions(color, 1.3f))
                    .receivers(60)
                    .spawn();
            point.add(step);
        }
    }

    @UpdateEvent
    public void tick() {
        final Iterator<Map.Entry<Player, PhantomBladeProjectile>> iterator = blades.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, PhantomBladeProjectile> entry = iterator.next();
            final Player player = entry.getKey();
            final PhantomBladeProjectile blade = entry.getValue();

            if (blade == null) {
                iterator.remove();
                continue;
            }

            if (player == null || !player.isOnline() || !hasSkill(player) || blade.isMarkForRemoval() || blade.hasFaded()) {
                blade.fade();
                blade.remove();
                iterator.remove();
                continue;
            }

            blade.tick();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMeleeHit(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player)) return;
        if (!hasSkill(player)) return;

        final Long recalled = recallTime.get(player);
        if (recalled == null || UtilTime.elapsed(recalled, (long) (getHitWindow(getLevel(player)) * 1000))) return;

        recallTime.remove(player);
        championsManager.getCooldowns().reduceCooldown(player, getName(), getCooldownReduction(getLevel(player)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        discard(event.getPlayer());
        recallTime.remove(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        discard(event.getEntity());
        recallTime.remove(event.getEntity());
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        discard(player);
        recallTime.remove(player);
    }

    private void discard(Player player) {
        final PhantomBladeProjectile blade = blades.remove(player);
        if (blade != null) {
            blade.remove();
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        speed = getConfig("speed", 20.0, Double.class);
        flightDuration = getConfig("flightDuration", 0.7, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
        anchorDuration = getConfig("anchorDuration", 5.0, Double.class);
        hitWindow = getConfig("hitWindow", 3.0, Double.class);
        baseCooldownReduction = getConfig("baseCooldownReduction", 3.0, Double.class);
        cooldownReductionPerLevel = getConfig("cooldownReductionPerLevel", 1.0, Double.class);
    }
}
