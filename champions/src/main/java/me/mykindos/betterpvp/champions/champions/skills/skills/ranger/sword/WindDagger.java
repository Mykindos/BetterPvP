package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@Getter
public class WindDagger extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, DaggerProjectile> daggerDataMap = new WeakHashMap<>();
    private double baseDamage;
    private double damagePerLevel;
    private double baseCooldownReduction;
    private double cooldownReductionPerLevel;
    private double duration;
    private double hitboxSize;
    private double speed;

    @Inject
    public WindDagger(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wind Dagger";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Throw a dagger that will fly for " + getValueString(this::getDuration, level) + " seconds",
                "and deal " + getValueString(this::getDamage, level) + " damage to enemies it hits.",
                "",
                "Every hit will reduce the cooldown by " + getValueString(this::getCooldownReduction, level) + " seconds.",
                "",
                "The dagger inherits all melee properties.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getDuration(int level) {
        return duration;
    }

    public double getDamage(int level) {
        return baseDamage + (damagePerLevel * (level - 1));
    }

    public double getCooldownReduction(int level) {
        return baseCooldownReduction + (cooldownReductionPerLevel * (level - 1));
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
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
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 2.0F);

        DaggerProjectile existingData = daggerDataMap.remove(player);
        if (existingData != null) {
            existingData.remove();
        }

        ItemStack swordItem = player.getInventory().getItemInMainHand();
        Location swordLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.0));
        DaggerProjectile data = new DaggerProjectile(
                player,
                getHitboxSize(),
                swordLocation,
                (long) (getDuration() * 1000),
                swordItem,
                getDamage(level),
                this
        );
        data.redirect(player.getLocation().getDirection().multiply(getSpeed()));

        daggerDataMap.put(player, data);
    }

    @UpdateEvent
    public void tick() {
        Iterator<Map.Entry<Player, DaggerProjectile>> iterator = daggerDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, DaggerProjectile> entry = iterator.next();
            Player player = entry.getKey();
            DaggerProjectile data = entry.getValue();

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
    }

    @EventHandler
    public void reduceCooldown(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!hasSkill(player)) return;

        int level = getLevel(player);
        this.championsManager.getCooldowns().reduceCooldown(player, getName(), getCooldownReduction(level));
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damagePerLevel = getConfig("damagePerLevel", 1.0, Double.class);
        speed = getConfig("speed", 2.0, Double.class);
        duration = getConfig("duration", 1.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
        baseCooldownReduction = getConfig("baseCooldownReduction", 1.5, Double.class);
        cooldownReductionPerLevel = getConfig("cooldownReductionPerLevel", 0.5, Double.class);
    }
}