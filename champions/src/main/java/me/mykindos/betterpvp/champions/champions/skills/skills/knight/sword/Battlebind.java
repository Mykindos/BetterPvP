package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.BattlebindProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@Getter
public class Battlebind extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, BattlebindProjectile> data = new WeakHashMap<>();
    private double baseDamage;
    private double damagePerLevel;
    private double pullSpeed;
    private double pullDuration;
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
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Throw your sword forward and deal <val>" + UtilFormat.formatNumber(getDamage(level)) + "</val> damage",
                "to the first enemy hit. The enemy hit will be",
                "pulled towards you",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + (damagePerLevel * (level - 1));
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
    public void activate(Player player, int level) {
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
                (long) (getPullDuration() * 1000),
                pullSpeed,
                swordItem,
                getDamage(level),
                this
        );
        data.redirect(player.getLocation().getDirection().multiply(getSpeed()));

        this.data.put(player, data);
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
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damagePerLevel = getConfig("damagePerLevel", 0.5, Double.class);
        speed = getConfig("speed", 2.0, Double.class);
        airDuration = getConfig("airDuration", 1.0, Double.class);
        pullDuration = getConfig("pullDuration", 1.5, Double.class);
        pullSpeed = getConfig("pullSpeed", 1.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
    }
}