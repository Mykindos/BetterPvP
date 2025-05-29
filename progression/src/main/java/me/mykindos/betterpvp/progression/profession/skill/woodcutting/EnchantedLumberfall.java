package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeDependency;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerUsesTreeFellerEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CustomLog
@Singleton
@BPvPListener
public class EnchantedLumberfall extends ProfessionSkillNode implements Listener {

    @Inject
    private ItemHandler itemHandler;

    @Inject
    private WoodcuttingHandler woodcuttingHandler;

    @Inject
    public EnchantedLumberfall(String name) {
        super("Enchanted Lumberfall");
    }

    @Override
    public String getName() {
        return "Enchanted Lumberfall";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Whenever you fell a tree, special items will drop from its leaves",
                "You have a <green>" + specialItemDropChance(level) + "%</green> chance to double your drops!"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    @Override
    public ProfessionNodeDependency getDependencies() {
        List<String> deps = List.of("Tree Feller");
        return new ProfessionNodeDependency(deps, 1);
    }

    /**
     * @param level the player's skill level
     * @return the computed special drop item chance that triggers whenever a player fells a tree
     */
    public double specialItemDropChance(int level) {
        return 0.1 * Math.max(1, level);
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

        Optional<ProfessionProfile> professionProfileOptional = professionProfileManager.getObject(player.getUniqueId().toString());
        if (professionProfileOptional.isEmpty()) {
            return;
        }

        ProfessionProfile profile = professionProfileOptional.get();

        UtilServer.runTaskLater(this.getProgression(), () -> {
            world.getBlockAt(locationToActivatePerk).breakNaturally();
            Location loc = player.getLocation();
            world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1F, 2F);

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = centerOfBlock.getX() + radius * Math.cos(angle);
                double z = centerOfBlock.getZ() + radius * Math.sin(angle);

                double particleY = centerOfBlock.getY() - i * decreaseInYLvlPerParticle;
                Location particleLocation = new Location(world, x, particleY, z);

                UtilServer.runTaskLater(this.getProgression(), () -> {
                    world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 0, 0, -1, 0);
                }, i);
            }

            ItemStack itemStack = woodcuttingHandler.getRandomLoot();
            if (itemStack == null) return;

            int count = itemStack.getAmount();

            double chance = UtilMath.randDouble(0, 100);
            boolean shouldDoubleDrops = chance < specialItemDropChance(getPlayerNodeLevel(profile));
            if (shouldDoubleDrops) {
                count *= 2;
                itemStack.setAmount(count);
            }
            ItemStack finalItemStack = itemHandler.updateNames(itemStack);

            UtilItem.insert(player, finalItemStack);

            TextComponent messageToPlayer = Component.text("You found ")
                    .append(Component.text(UtilFormat.formatNumber(count)))
                    .append(Component.text(" "))
                    .append(finalItemStack.getItemMeta() != null && finalItemStack.getItemMeta().hasDisplayName()
                            ? Objects.requireNonNull(finalItemStack.getItemMeta().displayName()) : finalItemStack.displayName());

            if (shouldDoubleDrops) {
                messageToPlayer = messageToPlayer.append(Component.text(" and doubled your drops"));
            }

            UtilMessage.message(player, getProgressionTree(), messageToPlayer);

            log.info("{} found {}x {}.", player.getName(), count, itemStack.getType().name().toLowerCase())
                    .addClientContext(player).addLocationContext(loc).submit();

            try {
                ItemEntity entity = new ItemEntity(((CraftWorld) player.getWorld()).getHandle(), loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(finalItemStack));
                org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) entity.getBukkitEntity();
                UtilServer.callEvent(new SpecialItemDropEvent(itemEntity, "Woodcutting"));
            } catch (Exception ex) {
                log.error("Failed to create special item drop event for player " + player.getName(), ex).submit();
            }
        }, 20L);
    }
}
