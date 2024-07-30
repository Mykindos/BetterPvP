package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class StormArrows extends PrepareArrowSkill implements OffensiveSkill {

    private int baseNumArrows;
    private int numArrowsIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private final Map<UUID, Integer> playerArrowsShot = new HashMap<>();
    private final Map<UUID, Long> playerSkillStartTime = new HashMap<>();
    private final WeakHashMap<Arrow, Player> arrows = new WeakHashMap<>();


    @Inject
    public StormArrows(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Storm Arrows";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Left click to shoot up to " + getValueString(this::getNumArrows, level) + " arrows instantly",
                "",
                "Each storm arrow deals " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public int getNumArrows(int level) {
        return baseNumArrows + ((level - 1) * numArrowsIncreasePerLevel);
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
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
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        UUID playerId = player.getUniqueId();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        playerArrowsShot.put(playerId, 0);
        playerSkillStartTime.put(playerId, System.currentTimeMillis());
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        if (playerArrowsShot.containsKey(player.getUniqueId())){
            doStormArrows(player, level);
            return false;
        }
        return true;
    }

    @EventHandler
    public void onBowSwing(CustomDamageEvent event){
        if(!(event.getDamager() instanceof Player player)) return;
        if(!(playerArrowsShot.containsKey(player.getUniqueId()))) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if(!isHolding(player)) return;

        int arrowsShot = playerArrowsShot.get(player.getUniqueId());
        int level = getLevel(player);
        if(arrowsShot < getNumArrows(level)){
            doStormArrows(player, level);
        }
    }

    public void doStormArrows(Player player, int level) {

        UUID playerId = player.getUniqueId();

        if (!playerArrowsShot.containsKey(playerId) || !playerSkillStartTime.containsKey(playerId)) {
            return;
        }

        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            UtilMessage.message(player, getName(), "You need at least <alt2>1 Arrow</alt2> to use this skill.");
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            UtilInventory.remove(player, Material.ARROW, 1);
        }

        int arrowsShot = playerArrowsShot.get(playerId);
        playerArrowsShot.put(playerId, arrowsShot + 1);

        System.out.println("arrowsShot: " +playerArrowsShot.get(playerId));
        System.out.println("getNumArrows: " +playerArrowsShot.get(playerId));

        Arrow newArrow = player.launchProjectile(Arrow.class);
        arrows.put(newArrow, player);
        newArrow.setVelocity(player.getLocation().getDirection().multiply(4)); // Adjust multiplier as needed for arrow speed

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 2.0F, 2.0F);
        player.getWorld().playEffect(player.getLocation(), Effect.BOW_FIRE, 0);

        if (arrowsShot >= getNumArrows(level) - 1) {
            System.out.println("got in here");
            playerArrowsShot.remove(playerId);
            playerSkillStartTime.remove(playerId);
            championsManager.getCooldowns().use(Bukkit.getPlayer(playerId),
                    getName(),
                    getCooldown(level),
                    true,
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // Do nothing
    }

    @UpdateEvent
    public void updateArrowTrail() {
        if(arrows.isEmpty()) return;
        System.out.println("test1");
        for (Arrow arrow : arrows.keySet()) {
            System.out.println("test2");
            if(arrow.isDead()){
                System.out.println("test3");
                arrows.remove(arrow);
            }
            displayTrail(arrow.getLocation());
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.FALLING_WATER)
                .location(location)
                .count(4)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!playerArrowsShot.containsKey(player.getUniqueId())) return;

        event.setDamage(getDamage(getLevel(player)));
        event.addReason(getName());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseNumArrows = getConfig("baseNumArrows", 3, Integer.class);
        numArrowsIncreasePerLevel = getConfig("numArrowsIncreasePerLevel", 0, Integer.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.75, Double.class);
    }
}
