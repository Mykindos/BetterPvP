package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.champions.skills.events.SuccessfulCrowdControlSkillUseEvent;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.ThreateningShoutData;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe.ScytheData;
import me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe.Soul;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Singleton
@BPvPListener
public class ThreateningShout extends Skill implements Listener, PassiveSkill, DebuffSkill, AreaOfEffectSkill, OffensiveSkill {

    private double radius;
    private int vulnerabilityStrength;
    private int tickDelay;
    private double maxDamage;
    private double maxVulnerabilityDuration;
    private double startDistance;
    private int distance;
    private double chargeResetTime;
    private double baseChargeGainedPerCC;
    private double chargeGainedPerCCIncreasePerLevel;

    /**
     * Objects are put into this map when the player successfully uses a CC skill and removed when the player toggles sneak.
     */
    private final Map<Player, ChargeData> playerChargeDataMap = new WeakHashMap<>();

    /**
     * Objects are put into this map when the player toggles sneak and removed when the skill is done.
     */
    private final Map<Player, ThreateningShoutData> shoutDataMap;

    /**
     * Allows us to keep track of the last time a player successfully used a CC skill.
     */
    private final Map<Player, Long> lastChargeWithSucessfulCC= new HashMap<>();


    private final DisplayComponent actionBar = ChargeData.getActionBar(
            gmr -> gmr.isOnline() && playerChargeDataMap.containsKey(gmr.getPlayer()),
            gmr -> playerChargeDataMap.get(gmr.getPlayer())
    );

    private final ClientManager clientManager;

    @Inject
    public ThreateningShout(Champions champions, ChampionsManager championsManager, ClientManager clientManager) {
        super(champions, championsManager);
        shoutDataMap = new WeakHashMap<>();
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "Threatening Shout";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Successfully using Crowd Control skills",
                "charges this ability by <val>" + getValueString(this::getChargeGainedPerCC, level, 1, "%", 0) + "</val>",
                "",
                "Sneak to release a roar, inflicting all",
                "enemies hit with <effect>Vulnerability " + UtilFormat.getRomanNumeral(vulnerabilityStrength) + "</effect>",
                "and dealing more damage with higher charge",
                "",
                EffectTypes.VULNERABILITY.getDescription(vulnerabilityStrength)
        };
    }

    public double getChargeGainedPerCC(int level) {
        return baseChargeGainedPerCC + ((level - 1) * chargeGainedPerCCIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @UpdateEvent
    public void resetChargeAndUpdateData() {
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<Player, ChargeData>> iterator = playerChargeDataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, ChargeData> entry = iterator.next();
            Player player = entry.getKey();
            ChargeData chargeData = entry.getValue();

            Long lastChargeTime = lastChargeWithSucessfulCC.get(player);
            if (lastChargeTime == null || (currentTime - lastChargeTime > chargeResetTime*1000)) {
                chargeData.setCharge(0);
                iterator.remove();
                lastChargeWithSucessfulCC.remove(player);
                Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                gamer.getActionBar().remove(actionBar);
            }
        }
    }

    @EventHandler
    public void onPlayerSuccessfullyUseCrowdControl(SuccessfulCrowdControlSkillUseEvent event) {
        Player player = event.getPlayer();
        int level = getLevel(player);
        if (level < 0) return;

        ChargeData chargeData;

        if (!playerChargeDataMap.containsKey(player)) {
            chargeData = new ChargeData(0);
            playerChargeDataMap.put(player, chargeData);
            clientManager.search().online(player).getGamer().getActionBar().add(1000, actionBar);
        } else {
            chargeData = playerChargeDataMap.get(player);
        }

        // This value is between 0d and 100d
        double chargeGained = getChargeGainedPerCC(level);

        // This value is between 0.0f and 1.0f
        float chargeGainedAsFloat = (float) (chargeGained * (1 / 100d));
        float previousCharge = chargeData.getCharge();
        float currentCharge = previousCharge + chargeGainedAsFloat;

        // ProgressBar.java does not let the charge exceed 1.0f
        if (currentCharge > 1.0f) {
            currentCharge = 1.0f;
        }

        chargeData.setCharge(currentCharge);
        lastChargeWithSucessfulCC.put(player, System.currentTimeMillis());
    }

    /**
     * <b>Primary Use</b>: To activate the skill when the player (toggle) sneaks.
     * <p>
     * <b>Edge Case</b>: this method can fire twice; first when the player starts sneaking and second when the player
     * stops sneaking.
     */
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {

        // no f-ing clue if this can ever be cancelled since this is client-sided
        if (event.isCancelled()) return;

        // CHECK 4 SAFE-ZONES

        Player player = event.getPlayer();
        int level = getLevel(player);
        if (level < 0) return;

        ChargeData chargeData = playerChargeDataMap.getOrDefault(player, new ChargeData(0));
        if (chargeData.getCharge() <= 0) {
            return;
        }

        final float finalCharge = chargeData.getCharge();

        // Reset charge and action bar
        chargeData.setCharge(0);
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBar);

        double damageToDeal = maxDamage * finalCharge;
        double vulnerabilityDurationToApply = maxVulnerabilityDuration * finalCharge;

        player.sendMessage("Your final damage and duration is " + damageToDeal + " and " + vulnerabilityDurationToApply);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 2.0F);

        Location start = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(startDistance));
        List<Location> points = new ArrayList<>();

        for (int i = 0; i < distance; i++) {
            Location point = start.clone().add(start.getDirection().normalize().multiply(i * 1.0));
            Block targetBlock = point.getBlock();
            if (!UtilBlock.airFoliage(targetBlock)) {
                break;
            }
            points.add(point);
        }

        ThreateningShoutData data = new ThreateningShoutData(points, 0, new HashSet<>(), new HashSet<>(),
                damageToDeal, vulnerabilityDurationToApply);
        shoutDataMap.put(player, data);
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<Player, ThreateningShoutData>> iterator = shoutDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, ThreateningShoutData> entry = iterator.next();
            Player player = entry.getKey();
            ThreateningShoutData data = entry.getValue();

            List<Location> points = data.getPoints();
            int currentPointIndex = data.getPointIndex();
            Set<LivingEntity> damagedEntities = data.getDamagedEntities();

            if (points.isEmpty() || currentPointIndex >= points.size()) {
                iterator.remove();
                continue;
            }

            for (int i = 0; i < 2 && currentPointIndex < points.size(); i++) {
                Location point = points.get(currentPointIndex);

                point.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 0, 0, 0, 0, 0);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (LivingEntity target : UtilEntity.getNearbyEnemies(player, point, radius)) {
                            if (!damagedEntities.contains(target)) {
                                UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null,
                                        EntityDamageEvent.DamageCause.CUSTOM, data.getDamageToDeal(),
                                        false, getName()));

                                double duration = data.getVulnerabilityDurationToApply();

                                championsManager.getEffects().addEffect(target, EffectTypes.VULNERABILITY, vulnerabilityStrength, (long) (duration * 1000L));
                                UtilMessage.simpleMessage(player, getName(), "You hit <yellow>%s</yellow> with <green>Threatening Shout</green>", target.getName());
                                damagedEntities.add(target);
                            }
                        }
                    }
                }.runTaskLater(champions, tickDelay);

                currentPointIndex++;
            }

            data.setPointIndex(currentPointIndex);
        }
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 1.5, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 2, Integer.class);
        tickDelay = getConfig("tickDelay", 12, Integer.class);
        maxDamage = getConfig("maxDamage", 12.0, Double.class);
        maxVulnerabilityDuration = getConfig("maxVulnerabilityDuration", 3.0, Double.class);
        startDistance = getConfig("startDistance", 1.0, Double.class);
        distance = getConfig("distance", 15, Integer.class);
        chargeResetTime = getConfig("chargeResetTime", 10.0, Double.class);
        baseChargeGainedPerCC = getConfig("baseChargeGainedPerCC", 10d, Double.class);
        chargeGainedPerCCIncreasePerLevel = getConfig("chargeGainedPerCCIncreasePerLevel ", 5d, Double.class);
    }
}
