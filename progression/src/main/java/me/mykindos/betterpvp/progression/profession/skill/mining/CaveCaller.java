package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import me.mykindos.betterpvp.progression.utility.ProgressionNamespacedKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Singleton
@BPvPListener
public class CaveCaller extends MiningProgressionSkill implements Listener {
    ProfessionProfileManager professionProfileManager;
    MiningHandler miningHandler;

    private double caveMonsterHealth;
    private double caveMonsterLifespanInSeconds;
    private HashMap<UUID, ArrayList<CaveMonsterData>> playerToAliveCaveMonsters = new HashMap<>();

    private final Role[] championsRoles = {
            Role.WARLOCK, Role.MAGE,
            Role.ASSASSIN, Role.RANGER,
            Role.KNIGHT, Role.BRUTE
    };
    private enum EquipmentPiece { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    @Getter
    static class CaveMonsterData {
        private final Husk monsterEntity;

        @Setter
        private double remainingLifespanInSeconds;

        @Setter
        private ItemStack armamentToDropOnDeath;

        public CaveMonsterData(Husk monsterEntity, double startingLifespanInSeconds) {
            this.monsterEntity = monsterEntity;
            this.remainingLifespanInSeconds = startingLifespanInSeconds;
            this.armamentToDropOnDeath = new ItemStack(Material.AIR);
        }
    }

    @Inject
    public CaveCaller(Progression progression, ProfessionProfileManager professionProfileManager, MiningHandler miningHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.miningHandler = miningHandler;
    }

    @Override
    public String getName() {
        return "Cave Caller";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Mine any block in your territory for a <green>20%</green> chance",
                "to summon a cave monster. These monsters drop",
                "various armaments.",
                "",
                "Toggle this perk on/off with <green>/cavecaller</green>"
        };
    }

    @Override
    public Material getIcon() {
        return Material.IRON_HELMET;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();

        caveMonsterHealth = getConfig("caveMonsterHealth", 20.0, Double.class);
        caveMonsterLifespanInSeconds = getConfig("caveMonsterLifespanInSeconds", 15.0, Double.class);
    }

    @EventHandler
    public void onBreakStoneBlock(BlockBreakEvent event) {
        // 1.5 seconds until they're spawned
        long DELAY_UNTIL_MONSTER_SPAWNED = 30L;

        if (event.isCancelled()) return;

        // Return if invalid block
        long experience = miningHandler.getExperienceFor(event.getBlock().getType());
        if (experience <= 0) return;

        // Move this to clans b/c u need to check if block is in your territory

        Player player = event.getPlayer();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            // Return if the player doesn't have the perk
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;

            Location locationToSpawn = event.getBlock().getLocation();

            Particle.OMINOUS_SPAWNING.builder()
                    .location(locationToSpawn)
                    .count(25)
                    .offset(0, 1, 0)
                    .receivers(player)
                    .spawn();

            player.getWorld().playSound(locationToSpawn, Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, 2.0F, 1.0F);

            UtilServer.runTaskLater(getProgression(), () -> {

                Husk caveMonster = (Husk) player.getWorld().spawnEntity(locationToSpawn, EntityType.HUSK);
                caveMonster.setAI(true);
                caveMonster.setHealth(caveMonsterHealth);
                caveMonster.customName(this.getSecondsAsComponent(caveMonsterLifespanInSeconds));
                caveMonster.setCustomNameVisible(true);

                // (this) Should fix bug where entities persist have restart
                caveMonster.setPersistent(false);

                PersistentDataContainer pdc = caveMonster.getPersistentDataContainer();
                pdc.set(ProgressionNamespacedKeys.CAVE_CALLER_MONSTER, PersistentDataType.BOOLEAN, true);

                CaveMonsterData caveMonsterData = new CaveMonsterData(caveMonster, caveMonsterLifespanInSeconds);

                UUID playerUUID = player.getUniqueId();

                if (!playerToAliveCaveMonsters.containsKey(playerUUID)) {
                    ArrayList<CaveMonsterData> newCaveMonsterDataList = new ArrayList<>(List.of(caveMonsterData));
                    playerToAliveCaveMonsters.put(playerUUID, newCaveMonsterDataList);
                } else {
                    // I believe `add` should mutate the List, so you won't have to call `put` on the `HashMap`
                    playerToAliveCaveMonsters.get(playerUUID).add(caveMonsterData);
                }

                EntityEquipment caveMonsterEquipment = caveMonster.getEquipment();
                caveMonsterEquipment.clear();

                final Random RANDOM = new Random();

                int randomRoleIndex = RANDOM.nextInt(championsRoles.length);
                Role randomRole = championsRoles[randomRoleIndex];

                caveMonsterEquipment.setHelmet(new ItemStack(randomRole.getHelmet()));
                caveMonsterEquipment.setChestplate(new ItemStack(randomRole.getChestplate()));
                caveMonsterEquipment.setLeggings(new ItemStack(randomRole.getLeggings()));
                caveMonsterEquipment.setBoots(new ItemStack(randomRole.getBoots()));
                caveMonsterEquipment.setItemInMainHand(new ItemStack(Material.IRON_SWORD));

                if (randomRole.equals(Role.ASSASSIN)) {
                    caveMonster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));
                }

                // Only 4 equipment pieces + the item in main hand
                int randomIndexForMaterialToDrop = RANDOM.nextInt(5);

                Material materialToDrop = switch (randomIndexForMaterialToDrop) {
                    case 0 -> randomRole.getBoots();
                    case 1 -> randomRole.getLeggings();
                    case 2 -> randomRole.getChestplate();
                    case 3 -> randomRole.getHelmet();
                    case 4 -> Material.IRON_SWORD;
                    default -> Material.AIR;  // unreachable
                };

                ItemStack equipmentAsItemStack = new ItemStack(materialToDrop);
                caveMonsterData.setArmamentToDropOnDeath(equipmentAsItemStack);

                CaveMonsterPathfinder caveMonsterPathfinder = new CaveMonsterPathfinder(getProgression(), caveMonster, player);
                Bukkit.getMobGoals().addGoal(caveMonster, 0, caveMonsterPathfinder);

            }, DELAY_UNTIL_MONSTER_SPAWNED);
        });
    }

    /**
     * Every second, update the cave monster's timer above their head.
     * If the cave monster time is up, then kill them
     */
    @UpdateEvent(delay=1000)
    public void updateCaveMonsterState() {
        for (UUID playerUUID : playerToAliveCaveMonsters.keySet()) {
            List<CaveMonsterData> caveMonstersForPlayer = playerToAliveCaveMonsters.get(playerUUID);

            ArrayList<CaveMonsterData> currentlyAliveCaveMonsters = new ArrayList<>(caveMonstersForPlayer);

            for (CaveMonsterData caveMonsterData : caveMonstersForPlayer) {

                // If the monster's lifespan is up, kill em'
                if (caveMonsterData.getRemainingLifespanInSeconds() <= 0) {

                    // It's important that the pdc is altered BEFORE you kill the monster
                    PersistentDataContainer pdc = caveMonsterData.getMonsterEntity().getPersistentDataContainer();
                    pdc.set(ProgressionNamespacedKeys.CAVE_CALLER_DEATH_BY_LIFESPAN, PersistentDataType.BOOLEAN, true);
                    caveMonsterData.getMonsterEntity().setHealth(0);
                }

                if (caveMonsterData.monsterEntity.isDead()) {
                    currentlyAliveCaveMonsters.remove(caveMonsterData);
                    continue;
                }

                // Decrement the current lifespan
                caveMonsterData.setRemainingLifespanInSeconds(caveMonsterData.getRemainingLifespanInSeconds() - 1);

                Component secondsAsComponent = getSecondsAsComponent(caveMonsterData.getRemainingLifespanInSeconds());
                caveMonsterData.monsterEntity.customName(secondsAsComponent);
            }

            playerToAliveCaveMonsters.put(playerUUID, currentlyAliveCaveMonsters);
        }
    }

    @EventHandler
    public void onPlayerKillCaveMonster(EntityDeathEvent event) {
        if (event.isCancelled()) return;

        // If it's not a cave caller monster, then ignore
        if (!event.getEntity().getPersistentDataContainer().has(ProgressionNamespacedKeys.CAVE_CALLER_MONSTER)) return;

        // If it died of natural causes, then ignore
        if (event.getEntity().getPersistentDataContainer().has(ProgressionNamespacedKeys.CAVE_CALLER_DEATH_BY_LIFESPAN)) return;

        for (UUID playerUUID : playerToAliveCaveMonsters.keySet()) {
            List<CaveMonsterData> caveMonstersForPlayer = playerToAliveCaveMonsters.get(playerUUID);

            // Go through every monster the player has
            for (CaveMonsterData caveMonsterData : caveMonstersForPlayer) {

                // Once you find the one that the player killed, drop its armament
                if (caveMonsterData.monsterEntity.equals(event.getEntity())) {
                    event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(),
                            caveMonsterData.armamentToDropOnDeath);
                }
            }
        }

    }

    private Component getSecondsAsComponent(double remainingTime) {
        TextColor color = (remainingTime > 5) ?
                TextColor.color(0, 200, 255) :
                TextColor.color(200, 70,  0);

        return Component.text(remainingTime).color(color)
                .append(Component.text("s").color(color));
    }

    static class CaveMonsterPathfinder implements Goal<Mob> {
        protected final Progression progression;
        private final GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey("progression", "cave_monster"));
        private final Mob mob;

        @Setter
        @Getter
        private LivingEntity target;

        public CaveMonsterPathfinder(Progression progression, Mob mob, LivingEntity target) {
            this.progression = progression;
            this.mob = mob;
            this.target = target;
        }

        @Override
        public boolean shouldActivate() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) return;
            mob.setTarget(target);

            if (mob.getLocation().distanceSquared(target.getLocation()) < 3) return;

            mob.getPathfinder().moveTo(target);
        }

        @Override
        public @NotNull GoalKey<Mob> getKey() {
            return key;
        }

        @Override
        public @NotNull EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.TARGET);
        }

    }
}
