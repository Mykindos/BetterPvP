package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class TriShot extends PrepareArrowSkill implements OffensiveSkill {

    private int baseNumTridents;
    private int numTridentsIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double tridentDelay;
    private final Map<UUID, TriShotData> dataMap = new HashMap<>();
    private final WeakHashMap<Trident, Player> tridents = new WeakHashMap<>();

    @Inject
    private CooldownManager cooldownManager;

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        if (player == null || !dataMap.containsKey(player.getUniqueId()) || !isHolding(player)) {
            return null;
        }

        TriShotData data = dataMap.get(player.getUniqueId());
        int level = getLevel(player);
        final int maxCharges = getNumTridents(level);
        final int newCharges = (maxCharges - data.getTridentsShot());

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxCharges - newCharges))).color(NamedTextColor.RED));
    });



    @Inject
    public TriShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tri-Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next " + getValueString(this::getNumTridents, level) + " arrows will be converted into tridents",
                "",
                "Left click to instantly shoot them, dealing " + getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds",
        };
    }

    public int getNumTridents(int level) {
        return baseNumTridents + ((level - 1) * numTridentsIncreasePerLevel);
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
        dataMap.put(playerId, new TriShotData(0, System.currentTimeMillis(), 0L));
        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        cooldownManager.removeCooldown(player, getName(), true);

        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(50, actionBarComponent);

    }

    @Override
    public boolean canUse(Player player) {
        int level = getLevel(player);
        if (dataMap.containsKey(player.getUniqueId())){
            doTridentShoot(player, level);
            return false;
        }
        return true;
    }

    @EventHandler
    public void onBowSwing(CustomDamageEvent event){
        if(!(event.getDamager() instanceof Player player)) return;
        if(!dataMap.containsKey(player.getUniqueId())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if(!isHolding(player)) return;

        TriShotData data = dataMap.get(player.getUniqueId());
        int level = getLevel(player);

        if(data.getTridentsShot() < getNumTridents(level)){
            doTridentShoot(player, level);
        }
    }

    public void doTridentShoot(Player player, int level) {
        UUID playerId = player.getUniqueId();
        TriShotData data = dataMap.get(playerId);

        if (data == null) {
            return;
        }

        if (System.currentTimeMillis() - data.getLastShotTime() < tridentDelay * 1000L){
            return;
        }

        if (!UtilInventory.contains(player, Material.ARROW, 1)) {
            UtilMessage.message(player, getName(), "You need at least <alt2>1 Arrow</alt2> to use this skill.");
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            UtilInventory.remove(player, Material.ARROW, 1);
        }

        data.incrementTridentsShot();
        Trident newTrident = player.launchProjectile(Trident.class);
        newTrident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        tridents.put(newTrident, player);
        newTrident.setVelocity(player.getLocation().getDirection().multiply(4));
        data.setLastShotTime(System.currentTimeMillis());
        newTrident.setShooter(player);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 2.0F, 2.0F);

        if (data.getTridentsShot() >= getNumTridents(level)) {
            dataMap.remove(playerId);
            championsManager.getCooldowns().use(Bukkit.getPlayer(playerId),
                    getName(),
                    getCooldown(level),
                    true,
                    true,
                    isCancellable(),
                    this::shouldDisplayActionBar);
            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            gamer.getActionBar().remove(actionBarComponent);
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // Do nothing
    }

    @UpdateEvent
    public void updateTridentTrail() {
        if (tridents.isEmpty()) return;

        Iterator<Map.Entry<Trident, Player>> iterator = tridents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Trident, Player> entry = iterator.next();
            Trident trident = entry.getKey();

            if (trident.isDead()) {
                iterator.remove();
            } else if (!trident.isInBlock()) {
                displayTrail(trident.getLocation());
            }
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
    public void removeTridents(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Trident trident) {
            UtilServer.runTaskLater(champions, () -> {
                trident.remove();
                tridents.remove(trident);
            }, 20L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        dataMap.remove(playerId);
        tridents.values().removeIf(player -> player.getUniqueId().equals(playerId));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        dataMap.remove(playerId);
        tridents.values().removeIf(player -> player.getUniqueId().equals(playerId));
    }

    @EventHandler
    public void onTridentHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Trident trident)) return;
        if (!tridents.containsKey(trident)) return;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
        event.getDamager().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);

        trident.remove();

        event.setDamage(getDamage(getLevel(player)));
        event.addReason(getName());
        event.setDamageDelay(0);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseNumTridents = getConfig("baseNumTridents", 3, Integer.class);
        numTridentsIncreasePerLevel = getConfig("numTridentsIncreasePerLevel", 0, Integer.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.75, Double.class);
        tridentDelay = getConfig("tridentDelay", 0.2, Double.class);
    }

    @Getter
    private static class TriShotData {
        private int tridentsShot;
        private final long skillStartTime;
        @Setter
        private long lastShotTime;

        public TriShotData(int tridentsShot, long skillStartTime, long lastShotTime) {
            this.tridentsShot = tridentsShot;
            this.skillStartTime = skillStartTime;
            this.lastShotTime = lastShotTime;
        }

        public void incrementTridentsShot() {
            this.tridentsShot++;
        }

    }
}