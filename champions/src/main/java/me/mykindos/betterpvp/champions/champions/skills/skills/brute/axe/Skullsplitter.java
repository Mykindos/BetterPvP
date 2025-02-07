package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.SkullsplitterProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
@Getter
public class Skullsplitter extends Skill implements InteractSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final WeakHashMap<Player, SkullsplitterProjectile> data = new WeakHashMap<>();
    private final EffectManager effectManager;
    private double bleedSeconds;
    private double duration;
    private double hitboxSize;
    private double speed;

    @Inject
    public Skullsplitter(Champions champions, ChampionsManager championsManager, EffectManager effectManager) {
        super(champions, championsManager);
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "Skullsplitter";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Throw a mace forward and follow its path",
                "once it lands. If it hits an enemy, they will",
                "bleed for <val>" + UtilFormat.formatNumber(getBleedSeconds()) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown()
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void activate(Player player) {
        if (!isHolding(player)) return;

        SkullsplitterProjectile existing = data.remove(player);
        if (existing != null) {
            existing.remove();
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.6F, 1.6F);

        Vector perpendicularAxis = player.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize();
        Location rightHandPosition = player.getLocation().add(0, 1, 0).add(perpendicularAxis.multiply(0.3));

        Vector direction = player.getLocation().getDirection().normalize();
        direction.add(new Vector(0, Math.sin(Math.toRadians(8)), 0));
        direction.normalize().multiply(speed);
        SkullsplitterProjectile projectile = new SkullsplitterProjectile(
                player,
                getHitboxSize(),
                rightHandPosition,
                (long) (getDuration() * 1000),
                new ItemStack(Material.MACE),
                getBleedSeconds(),
                getSpeed(),
                this,
                effectManager
        );
        projectile.redirect(direction);

        data.put(player, projectile);
    }

    @UpdateEvent
    public void tick() {
        Iterator<Map.Entry<Player, SkullsplitterProjectile>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, SkullsplitterProjectile> entry = iterator.next();
            Player player = entry.getKey();
            SkullsplitterProjectile data = entry.getValue();

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
        bleedSeconds = getConfig("bleedSeconds", 4.0, Double.class);
        speed = getConfig("speed", 5.0, Double.class);
        duration = getConfig("duration", 10.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.6, Double.class);
    }
}