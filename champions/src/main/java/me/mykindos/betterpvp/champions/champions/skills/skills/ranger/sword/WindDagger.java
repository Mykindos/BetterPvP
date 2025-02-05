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
    private double damage;
    private double cooldownReduction;
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
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Throw a dagger that will fly for <val>" + UtilFormat.formatNumber(getDuration()) + "</val> seconds",
                "and deal <val>" + UtilFormat.formatNumber(getDamage()) + "</val> damage to enemies it hits.",
                "",
                "Every hit will reduce the cooldown by <val>" + UtilFormat.formatNumber(getCooldownReduction()) + "</val> seconds.",
                "",
                "The dagger inherits all melee properties.",
                "",
                "Cooldown: <val>" + getCooldown()
        };
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
    public void activate(Player player) {
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
                getDamage(),
                this
        );
        data.redirect(player.getLocation().getDirection());
        data.setSpeed(getSpeed());

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
        Player player = (Player) event.getDamager();
        this.championsManager.getCooldowns().reduceCooldown(player, getName(), getCooldownReduction());
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 4.0, Double.class);
        speed = getConfig("speed", 2.0, Double.class);
        duration = getConfig("duration", 1.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
        cooldownReduction = getConfig("cooldownReduction", 1.5, Double.class);
    }
}