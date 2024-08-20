package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingLootType;
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
                               ItemHandler itemHandler,
                               WoodcuttingHandler woodcuttingHandler) {
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
                "You have a <green>" + specialItemDropChance(level) + "% chance to double your drops!"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller"};
        return new ProgressionSkillDependency(dependencies, 20);
    }

    /**
     * @param level the player's skill level
     * @return the computed special drop item chance that triggers whenever a player fells a tree
     */
    public double specialItemDropChance(int level) {
        return level*2;
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
     * <b>No More Leaves</b>
     */
    public boolean doesPlayerHaveSkill(Player player) {
        return getPlayerSkillLevel(player) > 0;
    }

    public void whenSkillTriggers(Player player, Location locationToDropItem) {

        World world = player.getWorld();
        Location centerOfBlock = locationToDropItem.add(0.5, 0, 0.5);
        final int particleCount = 3;
        final double radius = 0.15;
        final double decreaseInYLvlPerParticle = 0.05;

        UtilServer.runTaskLater(this.getProgression(), () -> {
            world.getBlockAt(locationToDropItem).breakNaturally();
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

            WoodcuttingLootType lootType = woodcuttingHandler.getLootTypes().random();
            Random random = new Random();
            final int count = random.ints(lootType.getMinAmount(), lootType.getMaxAmount() + 1)
                    .findFirst()
                    .orElse(lootType.getMinAmount());

            ItemStack itemStack = new ItemStack(lootType.getMaterial(), count);
            itemStack.editMeta(meta -> meta.setCustomModelData(lootType.getCustomModelData()));
            UtilItem.insert(player, itemStack);

            UtilMessage.message(player, getProgressionTree(), "You found %s <alt>%s</alt>",
                    UtilFormat.formatNumber(count), lootType.getMaterial());


            log.info("{} found {}x {}.", player.getName(), count, lootType.getMaterial().name().toLowerCase())
                    .addClientContext(player).addLocationContext(player.getLocation()).submit();
        }, 20L);
    }
}
