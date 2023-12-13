package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class RopedArrow extends PrepareArrowSkill {

    private static final int MAX_STRENGTH = 3;

    private final WeakHashMap<Player, Integer> strength = new WeakHashMap<>();

    // Action bar
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        // Only display charges in hotbar if holding the weapon
        if (player == null || !strength.containsKey(player) || !UtilPlayer.isHoldingItem(player, getItemsBySkillType())) {
            return null; // Skip if not online or not charging
        }

        final int curStrength = strength.get(player);
        return Component.text(getName() + " Strength ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(curStrength)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, MAX_STRENGTH - curStrength))).color(NamedTextColor.RED));
    });

    @Inject
    public RopedArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Roped Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow will pull you",
                "towards the location it hits",
                "",
                "Left click when your shot is already",
                "prepared to cycle pull strengths. Your",
                "cooldown will be doubled for each strength",
                "level",
                "",
                "Cooldown: <val>" + getCooldown(level) + "</val>"
        };
    }

    @Override
    public void invalidatePlayer(Player player) {
        strength.remove(player);
        // Action bar
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void trackPlayer(Player player) {
        // Action bar
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(900, actionBarComponent);
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
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        super.processEntityShootBowEvent(event, player, level, arrow);
        final int strengthLvl = this.strength.getOrDefault(player, 1);
        final double cdMult = Math.pow(2, strengthLvl - 1d);
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level) * cdMult,
                showCooldownFinished(),
                false,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
        strength.put(player, 1);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;

        Vector vec = UtilVelocity.getTrajectory(player, arrow);
        final int curStrength = this.strength.getOrDefault(player, 0);
        double mult = arrow.getVelocity().length() / 3.0D;

        UtilVelocity.velocity(player,
                vec,
                2.5D + mult * curStrength,
                false,
                0.8D,
                0.3D * mult,
                1.5D * mult,
                true);

        arrow.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        arrows.remove(arrow);
        strength.remove(player);
        championsManager.getEffects().addEffect(player, EffectType.NOFALL, 5000);
    }

    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            // Meaning they have already prepared a shot
            int curStrength = strength.compute(player, (p, current) -> {
                final int newStrength = Optional.ofNullable(current).orElse(0) + 1;
                if (newStrength > MAX_STRENGTH) {
                    return 1;
                } else {
                    return newStrength;
                }
            });

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.4f + curStrength * 0.2f);
            return false;
        }
        return super.canUse(player);
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // No implementation - ignore
    }

    @Override
    public void displayTrail(Location location) {
        Particle.REDSTONE.builder().location(location).color(165, 42, 42).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return (double) cooldown - (level - 1);
    }

}
