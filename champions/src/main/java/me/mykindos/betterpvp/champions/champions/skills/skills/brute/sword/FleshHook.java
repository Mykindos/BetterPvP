package me.mykindos.betterpvp.champions.champions.skills.skills.brute.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@CustomLog
public class FleshHook extends ChannelSkill implements InteractSkill, CooldownSkill, ThrowableListener, DamageSkill, CrowdControlSkill {

    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, Hook> hooks = new WeakHashMap<>();
    private final DisplayComponent actionBarComponent = ChargeData.getActionBar(this, charging);

    private double damage;
    private double damageIncreasePerLevel;

    @Inject
    public FleshHook(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Flesh Hook";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Hold right click with a Sword to channel",
                "",
                "Charge a hook that latches onto",
                "enemies pulling them towards you" ,
                "and dealing " + getValueString(this::getDamage, level) + " damage.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
    }

    public double getDamage(int level){
        return (damage + (damageIncreasePerLevel * (level-1)));
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return (cooldown - (cooldownDecreasePerLevel * (level - 1)));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void activate(Player player, int level) {
        charging.put(player, new ChargeData((float) (0.1 + (level - 1) * 0.05) * 5));
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !charging.containsKey(gamer.getPlayer()) && isHolding(gamer.getPlayer());
    }

    @UpdateEvent
    public void updateFleshHook() {
        final Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final ChargeData data = charging.get(player);
            if (player == null) {
                iterator.remove();
                continue;
            }

            // Remove if they no longer have the skill
            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            // Check if they still are blocking and charge
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (isHolding(player) && gamer.isHoldingRightClick()) {
                data.tick();
                data.tickSound(player);
                continue;
            }

            shoot(player, data, level);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <alt>" + getName() + " " + level + "</alt>.");
            new SoundEffect(Sound.ENTITY_SPLASH_POTION_THROW, 2F, 0.8F).play(player.getLocation());
            iterator.remove();
        }

        // Hook particles
        final Iterator<Player> hookIterator = hooks.keySet().iterator();
        while (hookIterator.hasNext()) {
            final Player player = hookIterator.next();
            final Hook data = hooks.get(player);
            if (player == null) {
                hookIterator.remove();
                continue;
            }

            final ThrowableItem hook = data.getThrowable();
            final int level = getLevel(player);
            if (hook == null || hook.getItem() == null || !hook.getItem().isValid() || level <= 0) {
                hookIterator.remove();
                continue;
            }

            final Location location = hook.getItem().getLocation();
            Particle.CRIT.builder()
                    .count(3)
                    .extra(0)
                    .offset(0.2, 0.2, 0.2)
                    .location(location)
                    .receivers(60)
                    .spawn();

            new SoundEffect(Sound.ITEM_FLINTANDSTEEL_USE, 1.4F, 0.8F).play(location);
        }
    }

    private void shoot(Player player, ChargeData data, int level) {
        final Item item = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.TRIPWIRE_HOOK));
        final ThrowableItem throwable = new ThrowableItem(this, item, player, getName(), 10_000L, true);
        throwable.setCollideGround(true);
        throwable.setCanHitFriendlies(true);
        championsManager.getThrowables().addThrowable(throwable);

        VelocityData velocityData = new VelocityData(player.getLocation().getDirection(), 1 + data.getCharge(), false, 0, 0.2, 20, false);
        UtilVelocity.velocity(item, player, velocityData);

        hooks.put(player, new Hook(throwable, data, level));
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        final Player player = (Player) thrower;
        if (!hooks.containsKey(player)) {
            return;
        }

        final Hook hookData = hooks.get(player);
        final int level = hookData.getLevel();

        final PlayerCanUseSkillEvent useEvent = new PlayerCanUseSkillEvent(player, this);
        UtilServer.callEvent(useEvent);
        if (useEvent.isCancelled()) {
            return;
        }

        // Velocity
        final Vector direction = player.getLocation().toVector().subtract(hit.getLocation().toVector()).normalize();
        final double strength = hookData.getThrowable().getItem().getVelocity().length();
        VelocityData velocityData = new VelocityData(direction, strength, false, 0, 0.7, 1.2, true);
        UtilVelocity.velocity(hit, thrower, velocityData, VelocityType.CUSTOM);
        hit.setFallDistance(0); // Reset their fall distance

        // Damage
        final double damage = getDamage(level) * hookData.getData().getCharge();
        CustomDamageEvent ev = new CustomDamageEvent(hit, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName());
        UtilDamage.doCustomDamage(ev);

        // Cues
        UtilMessage.simpleMessage(hit, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> pulled you with <alt>" + getName() + " " + level + "</alt>.");
        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + hit.getName() + "</alt2> with <alt>" + getName() + " " + level + "</alt>.");
        new SoundEffect(Sound.ENTITY_ARROW_HIT_PLAYER, 2f, 2f).play(player);

        throwableItem.getItem().remove();
        charging.remove(player);
        hooks.remove(player);
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }


    @Value
    private static class Hook {
        ThrowableItem throwable;
        ChargeData data;
        int level;
    }
}
