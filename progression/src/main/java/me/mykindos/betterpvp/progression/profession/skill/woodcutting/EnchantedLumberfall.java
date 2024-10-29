package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerUsesTreeFellerEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Random;

@CustomLog
@Singleton
@BPvPListener
public class EnchantedLumberfall extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private final ItemHandler itemHandler;
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public EnchantedLumberfall(Progression progression, ProfessionProfileManager professionProfileManager,
                               ItemHandler itemHandler, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.itemHandler = itemHandler;
        this.woodcuttingHandler = woodcuttingHandler;
    }

    @Override
    public String getName() {
        return "Enchanted Lumberfall";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Whenever you fell a tree, special items will drop from its leaves",
                "You have a <green>" + specialItemDropChance(level) + "%</green> chance to double your drops!"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller", "Auto Planter", "Tree Compactor"};
        return new ProgressionSkillDependency(dependencies, 1);
    }

    /**
     * @param level the player's skill level
     * @return the computed special drop item chance that triggers whenever a player fells a tree
     */
    public double specialItemDropChance(int level) {
        return 0.1 * Math.max(1, level);
    }

    /**
     * @return the player's skill level
     */
    public int getPlayerSkillLevel(Player player) {
        Optional<ProfessionProfile> profile = professionProfileManager.getObject(player.getUniqueId().toString());

        return profile.map(this::getPlayerSkillLevel).orElse(0);
    }

    /**
     * This function's purpose is to return a boolean that tells you if the player has the skill
     * <b>Enchanted Lumberfall</b>
     */
    public boolean doesPlayerHaveSkill(Player player) {
        return getPlayerSkillLevel(player) > 0;
    }

    @EventHandler
    public void whenPlayerFellsTree(PlayerUsesTreeFellerEvent event) {
        Location locationToActivatePerk = event.getLocationToActivatePerk();
        if (locationToActivatePerk == null) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        Location centerOfBlock = locationToActivatePerk.add(0.5, 0, 0.5);

        final int particleCount = 3;
        final double radius = 0.15;
        final double decreaseInYLvlPerParticle = 0.05;

        UtilServer.runTaskLater(this.getProgression(), () -> {
            world.getBlockAt(locationToActivatePerk).breakNaturally();
            world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1F, 2F);

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = centerOfBlock.getX() + radius * Math.cos(angle);
                double z = centerOfBlock.getZ() + radius * Math.sin(angle);

                double particleY = centerOfBlock.getY() - i*decreaseInYLvlPerParticle;
                Location particleLocation = new Location(world, x, particleY, z);

                UtilServer.runTaskLater(this.getProgression(), () -> {
                    world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 0, 0, -1, 0);
                }, i);
            }

            WoodcuttingHandler.WoodcuttingLootType lootType = woodcuttingHandler.getLootTypes().random();
            Random random = new Random();
            int count = random.ints(lootType.getMinAmount(), lootType.getMaxAmount() + 1)
                    .findFirst()
                    .orElse(lootType.getMinAmount());


            double chance = UtilMath.randDouble(0, 100);
            boolean shouldDoubleDrops = chance < specialItemDropChance(getPlayerSkillLevel(player));
            if (shouldDoubleDrops) count *= 2;

            ItemStack itemStack = new ItemStack(lootType.getMaterial(), count);
            itemStack.editMeta(meta -> meta.setCustomModelData(lootType.getCustomModelData()));
            ItemStack finalItemStack = itemHandler.updateNames(itemStack);
            UtilItem.insert(player, finalItemStack);

            TextComponent messageToPlayer = Component.text("You found ")
                    .append(Component.text(UtilFormat.formatNumber(count)))
                    .append(Component.text(" "))
                    .append(finalItemStack.displayName());

            if (shouldDoubleDrops) {
                messageToPlayer = messageToPlayer.append(Component.text(" and doubled your drops"));
            }

            UtilMessage.message(player, getProgressionTree(), messageToPlayer);

            log.info("{} found {}x {}.", player.getName(), count, lootType.getMaterial().name().toLowerCase())
                    .addClientContext(player).addLocationContext(player.getLocation()).submit();
        }, 20L);
    }
}
