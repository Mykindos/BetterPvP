package me.mykindos.betterpvp.progression.profession.skill.woodcutting.enchantedlumberfall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.impl.interaction.event.TreeFellerEvent;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@CustomLog
@Singleton
@SkillId("enchanted_lumberfall")
public class EnchantedLumberfall extends ProfessionSkill {
    private final ProfessionProfileManager profileManager;
    private final ItemFactory itemFactory;
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public EnchantedLumberfall(ProfessionProfileManager profileManager,
                               ItemFactory itemFactory, WoodcuttingHandler woodcuttingHandler) {
        super("Enchanted Lumberfall");
        this.profileManager = profileManager;
        this.itemFactory = itemFactory;
        this.woodcuttingHandler = woodcuttingHandler;
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

    /**
     * @param level the player's skill level
     * @return the computed special drop item chance that triggers whenever a player fells a tree
     */
    public double specialItemDropChance(int level) {
        return 0.1 * Math.max(1, level);
    }

    public void whenPlayerFellsTree(TreeFellerEvent event) {
        Location locationToActivatePerk = event.getLeafActivationLocation();
        if (locationToActivatePerk == null) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        Location centerOfBlock = locationToActivatePerk.add(0.5, 0, 0.5);

        final int particleCount = 3;
        final double radius = 0.15;
        final double decreaseInYLvlPerParticle = 0.05;

        UtilServer.runTaskLater(this.getProgression(), () -> {
            world.getBlockAt(locationToActivatePerk).breakNaturally();
            Location loc = player.getLocation();
            world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1F, 2F);

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

            final Location location = event.getPlayer().getLocation();
            LootBundle bundle = woodcuttingHandler.getRandomLoot(event.getPlayer(), location);
            for (Loot<?, ?> loot : bundle) {
                final Object award = loot.award(bundle.getContext());
                if (award instanceof Item item) {
                    processItemStack(player, locationToActivatePerk, item.getItemStack());
                } else if (award instanceof ItemStack itemStack) {
                    processItemStack(player, locationToActivatePerk, itemStack);
                }
            }
        }, 20L);
    }

    private void processItemStack(Player player, Location location, ItemStack itemStack) {
        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getSkillLevel(profile);
            if (skillLevel <= 0) return;
            int count = itemStack.getAmount();

            double chance = UtilMath.randDouble(0, 100);
            boolean shouldDoubleDrops = chance < specialItemDropChance(skillLevel);
            if (shouldDoubleDrops) {
                count *= 2;
                itemStack.setAmount(count);
            }
            ItemStack finalItemStack = itemFactory.convertItemStack(itemStack).orElse(itemStack);
            final Component name;
            if (itemStack.getItemMeta().hasDisplayName()) {
                name = Objects.requireNonNull(itemStack.getItemMeta().displayName());
            } else {
                name = Objects.requireNonNullElse(itemStack.getData(DataComponentTypes.ITEM_NAME),
                        Component.translatable(itemStack.getType().translationKey()));
            }


            UtilItem.insert(player, finalItemStack);

            TextComponent messageToPlayer = Component.text("You found ")
                    .append(Component.text(UtilFormat.formatNumber(count)))
                    .append(Component.text(" "))
                    .append(name);

            if (shouldDoubleDrops) {
                messageToPlayer = messageToPlayer.append(Component.text(" and doubled your drops"));
            }

            UtilMessage.message(player, getProgressionTree(), messageToPlayer);

            log.info("{} found {}x {}.", player.getName(), count, itemStack.getType().name().toLowerCase())
                    .addClientContext(player).addLocationContext(location).submit();
        });
    }
}
