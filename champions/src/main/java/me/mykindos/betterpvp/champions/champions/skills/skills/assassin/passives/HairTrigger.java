package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.HairTriggerData;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HairTrigger extends Skill implements PassiveSkill, Listener {
    private Map<Player, Map<LivingEntity, HairTriggerData>> data = new WeakHashMap<>();
    private List<Arrow> shotArrows = new ArrayList<>();

    /**
     * In seconds.
     */
    private double tagResetTime;
    private double tagResetTimeIncreasePerLevel;
    private int requiredTagsOnEnemy;
    private double taggedDamageIncreaseAmount;

    /**
     * In seconds.
     */
    private double shootArrowWindUpTime;

    @Inject
    public HairTrigger(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {

        // Note: this ability does work in water, but I didn't add that to the desc to keep it simpler.
        return new String[]{
                "Your melee attacks tag enemies for ",
                getValueString(this::getTagResetTime, level) + " seconds.",
                "",
                "Tagging an enemy " + getValueString(this::getRequiredTagsOnEnemy, level) + " times causes",
                "your bow to shoot.",
                "",
                "Tagged enemies take " + getValueString(this::getTaggedDamageIncreaseAmount, level) + " more",
                "damage from your shot arrows."
        };
    }

    private double getTagResetTime(int level) {
        return tagResetTime + (level - 1) * tagResetTimeIncreasePerLevel;
    }

    private int getRequiredTagsOnEnemy(int level) {
        return requiredTagsOnEnemy;
    }

    private double getTaggedDamageIncreaseAmount(int level) {
        return taggedDamageIncreaseAmount;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitEnemy(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level <= 0) return;

        final @Nullable LivingEntity taggedEnemy = event.getLivingDamagee();
        if (taggedEnemy == null) return;  // Should not happen since we check isDamageeLiving, but just in case

        final @NotNull HairTriggerData abilityData = getAbilityData(damager, taggedEnemy);
        if (abilityData.isWindingUp()) return;  // Don't touch tags while winding up

        final long currentTime = System.currentTimeMillis();
        abilityData.setLastTagTime(currentTime);

        final int requiredTags = getRequiredTagsOnEnemy(level);

        final int tagCount = abilityData.getTagCount();
        if (tagCount < requiredTags) {  // DO NOT turn this into if-else w/ stmt below!!!
            abilityData.setTagCount(tagCount + 1);
        }

        if (abilityData.getTagCount() >= requiredTags) {
            abilityData.setWindingUp(true);
            abilityData.setWindUpStartTime(currentTime);
        }
    }

    // Priority set to high so it doesn't conflict with the assassin no-bow dmg role-effect*
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!(projectile instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        final int level = getLevel(damager);
        if (level <= 0) return;

        final @Nullable LivingEntity taggedEnemy = event.getLivingDamagee();
        if (taggedEnemy == null) return;

        final @NotNull HairTriggerData abilityData = getAbilityData(damager, taggedEnemy);
        if (abilityData.getTagCount() == 0) return;

        // Feedback particles for tagged enemy getting hit by the arrow
        Particle.DUST.builder()
                .color(Color.AQUA)
                .location(taggedEnemy.getLocation())
                .offset(1.0, taggedEnemy.getHeight() / 2, 1.0)
                .count(30)
                .receivers(32)
                .extra(2f)
                .spawn();

        final double damageIncreaseAmount = getTaggedDamageIncreaseAmount(level);
        event.addModifier(new SkillDamageModifier.Flat(this, damageIncreaseAmount));
    }

    private @NotNull HairTriggerData getAbilityData(@NotNull Player player, @NotNull LivingEntity enemy) {

        // I know there's a more concise way to do this but I like how this looks
        data.putIfAbsent(player, new WeakHashMap<>());
        final Map<LivingEntity, HairTriggerData> taggedEnemiesMap = data.get(player);

        taggedEnemiesMap.putIfAbsent(enemy, new HairTriggerData());
        return taggedEnemiesMap.get(enemy);
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<Player> allPlayersIterator = data.keySet().iterator();
        while (allPlayersIterator.hasNext()) {
            final @Nullable Player player = allPlayersIterator.next();
            final Map<LivingEntity, HairTriggerData> tagEnemiesMap = data.get(player);

            if (player == null || !player.isOnline() || player.isDead() || !player.isValid()) {
                allPlayersIterator.remove();
                continue;
            }

            final int level = getLevel(player);
            if (level <= 0) {
                allPlayersIterator.remove();
                continue;
            }

            final Iterator<LivingEntity> taggedEnemiesIterator = tagEnemiesMap.keySet().iterator();
            while (taggedEnemiesIterator.hasNext()) {
                final LivingEntity taggedEnemy = taggedEnemiesIterator.next();
                final @NotNull HairTriggerData abilityData = tagEnemiesMap.get(taggedEnemy);

                // Spawn camp edge case; probably not worth checking
                if (taggedEnemy.isDead() || !taggedEnemy.isValid()) {
                    taggedEnemiesIterator.remove();
                    continue;
                }

                // Play sound for wind up
                final long windUpTimeMillis = (long) (shootArrowWindUpTime * 1000L);
                if (abilityData.isWindingUp()) {
                    playWindUpSound(player, abilityData, windUpTimeMillis);
                }

                if (abilityData.isWindingUp() && UtilTime.elapsed(abilityData.getWindUpStartTime(), windUpTimeMillis)) {
                    doArrowShootWhenWindUpComplete(player, abilityData);
                }

                // If the enemy has been tagged for long enough, remove the tag and play a sound to notify the player
                final long tagResetTimeMillis = (long) (getTagResetTime(level) * 1000L);
                if (UtilTime.elapsed(abilityData.getLastTagTime(), tagResetTimeMillis)) {
                    taggedEnemiesIterator.remove();

                    if (abilityData.getTagCount() > 0) {
                        player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    }
                }
            }
        }

        // Clean up arrows that have been on the ground for a while or are otherwise invalid
        shotArrows.removeIf(arrow -> arrow.isOnGround() || !arrow.isValid() || arrow.isInsideVehicle());
    }

    // Calcualte how much time till full wind up and player louder ptich sound the closer they are to full wind up
    private void playWindUpSound(Player player, HairTriggerData abilityData, long windUpTimeMillis) {
        final long currentTime = System.currentTimeMillis();
        final long timeSinceWindUpStart = currentTime - abilityData.getWindUpStartTime();
        final long timeUntilShot = Math.max(windUpTimeMillis - timeSinceWindUpStart, 0);

        // If windUpTime = 0.5s then this pitch goes rom 1.25f -> 1.75f
        float pitch = 1.25f + 0.5f * (1.0f - ((float) timeUntilShot / windUpTimeMillis));

        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
    }

    private void doArrowShootWhenWindUpComplete(@NotNull Player player, @NotNull HairTriggerData abilityData) {

        final Arrow hairTriggeredArrow = player.launchProjectile(Arrow.class);
        hairTriggeredArrow.setPickupStatus(Arrow.PickupStatus.CREATIVE_ONLY);
        final EntityShootBowEvent shootBowEvent = new EntityShootBowEvent(player, new ItemStack(Material.BOW),
                null, hairTriggeredArrow, EquipmentSlot.HAND, 1f, true);
        UtilServer.callEvent(shootBowEvent);

        shotArrows.add(hairTriggeredArrow);
        abilityData.setTagCount(0);
        abilityData.setWindingUp(false);

        player.playSound(player, Sound.ENTITY_ARROW_SHOOT, 1f, 1.5f);
    }

    @Override
    public String getName() {
        return "Hair Trigger";
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        tagResetTime = getConfig("tagResetTime", 3.0, Double.class);
        tagResetTimeIncreasePerLevel = getConfig("tagResetTimeIncreasePerLevel", 1.5, Double.class);
        requiredTagsOnEnemy = getConfig("requiredTagsOnEnemy", 3, Integer.class);
        taggedDamageIncreaseAmount = getConfig("taggedDamageIncreaseAmount", 2.0, Double.class);
        shootArrowWindUpTime = getConfig("shootArrowWindUpTime", 0.5, Double.class);
    }
}
