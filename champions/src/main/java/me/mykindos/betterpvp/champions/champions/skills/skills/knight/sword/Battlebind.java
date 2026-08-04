package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.BattlebindChains;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.BattlebindProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.listeners.effects.RootedListener;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@Getter
public class Battlebind extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill, CrowdControlSkill {

    private final WeakHashMap<Player, BattlebindProjectile> data = new WeakHashMap<>();
    private final WeakHashMap<LivingEntity, BattlebindChains> bound = new WeakHashMap<>();
    private double baseDamage;
    private double damagePerLevel;
    private double baseBindDuration;
    private double bindDurationPerLevel;
    private double airDuration;
    private double hitboxSize;
    private double speed;

    @Inject
    public Battlebind(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Battlebind";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component bindDuration = getValueComponent(this::getBindDuration, level, 1);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines(
                "champions.skill.knight.battlebind.description",
                damage,
                bindDuration,
                cooldown
        );
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + (damagePerLevel * (level - 1));
    }

    public double getBindDuration(int level) {
        return baseBindDuration + (bindDurationPerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public boolean activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 2.0F);

        BattlebindProjectile existing = data.remove(player);
        if (existing != null) {
            existing.remove();
        }

        ItemStack swordItem = player.getInventory().getItemInMainHand();
        Location swordLocation = player.getEyeLocation();
        BattlebindProjectile data = new BattlebindProjectile(
                player,
                getHitboxSize(),
                swordLocation,
                (long) (getAirDuration() * 1000),
                swordItem,
                this,
                level
        );
        data.redirect(player.getLocation().getDirection().multiply(getSpeed()));

        this.data.put(player, data);
        return true;
    }

    /**
     * Wraps whoever the chain caught in what is left of it and pins them there.
     */
    public void bind(Player caster, LivingEntity target, int level) {
        final long duration = (long) (getBindDuration(level) * 1000);

        BattlebindChains existing = bound.remove(target);
        if (existing != null) {
            existing.release();
        }

        championsManager.getEffects().addEffect(target, caster, EffectTypes.ROOTED, getName(), level, duration);
        bound.put(target, new BattlebindChains(target, duration));
    }

    /**
     * Battlebind pins its target down, but it does not make them immovable to everyone else - the
     * point of chaining someone is that their allies and enemies can still knock them around. Runs at
     * {@link EventPriority#MONITOR} purely to get the last word after
     * {@link RootedListener} has cancelled the velocity.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoundVelocity(CustomEntityVelocityEvent event) {
        if (!event.isCancelled() || !RootedListener.CANCEL_REASON.equals(event.getCancelReason())) {
            return;
        }

//        if (event.getVelocityType() == VelocityType.KNOCKBACK) {
//            return;
//        }

        if (event.getSource() == null || event.getSource() == event.getEntity()) {
            return;
        }

        if (!bound.containsKey(event.getEntity())) {
            return;
        }

        event.setCancelled(false);
        event.setCancelReason(null);
    }

    @UpdateEvent
    public void tick() {
        Iterator<Map.Entry<Player, BattlebindProjectile>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, BattlebindProjectile> entry = iterator.next();
            Player player = entry.getKey();
            BattlebindProjectile data = entry.getValue();

            if (data == null) {
                iterator.remove();
                continue; // no data?
            }

            if (player == null || !player.isOnline() || !hasSkill(player) || data.isMarkForRemoval() || data.isExpired()) {
                data.remove();
                iterator.remove();
                continue; // Remove if no player is not online, no skill, or expired
            }

            data.tick();
        }

        Iterator<Map.Entry<LivingEntity, BattlebindChains>> chainIterator = bound.entrySet().iterator();
        while (chainIterator.hasNext()) {
            BattlebindChains chains = chainIterator.next().getValue();
            chains.tick();
            if (chains.isFinished()) {
                chainIterator.remove();
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damagePerLevel = getConfig("damagePerLevel", 0.5, Double.class);
        speed = getConfig("speed", 2.0, Double.class);
        airDuration = getConfig("airDuration", 1.0, Double.class);
        baseBindDuration = getConfig("baseBindDuration", 1.5, Double.class);
        bindDurationPerLevel = getConfig("bindDurationPerLevel", 0.25, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
    }
}
