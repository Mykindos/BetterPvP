package me.mykindos.betterpvp.progression.profession.skill.mining.demolitioncharge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.blastdamage.BlastDamageAttribute;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.blastradius.BlastRadiusAttribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@NodeId("demolition_charge")
public class DemolitionCharge extends ProfessionSkill {

    @Inject
    private BlastRadiusAttribute blastRadius;

    @Inject
    private BlastDamageAttribute blastDamage;

    @Inject
    private CooldownManager cooldownManager;

    private double baseRadius;
    private double baseDamage;
    private double cooldown;
    private double markDurationSeconds;

    final Map<Block, ItemDisplay> markedBlocks = new ConcurrentHashMap<>();

    @Inject
    public DemolitionCharge() {
        super("Demolition Charge");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right-click any block while holding <yellow>Coal</yellow>",
                "to plant a demolition charge.",
                "",
                "Breaking the marked block triggers an explosion:",
                "  Radius: <green>" + UtilMath.round(baseRadius, 1) + " <reset>blocks",
                "  Damage: <green>" + UtilMath.round(baseDamage, 1),
                "  Cooldown: <green>" + UtilMath.round(cooldown, 1) + "s",
                "",
                "Charges expire after <yellow>" + UtilMath.round(markDurationSeconds, 0) + "s</yellow>."
        };
    }

    @Override
    public Material getIcon() {
        return Material.COAL;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.COAL) return;

        Block block = event.getClickedBlock();
        if (block == null || !block.getType().isSolid()) return;

        event.setCancelled(true);

        // Don't place a second charge on an already-marked block
        if (markedBlocks.containsKey(block)) return;

        // Consume 1 coal
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.subtract(1);
        }

        Location center = block.getLocation().toCenterLocation();
        ItemDisplay display = block.getWorld().spawn(center, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.COAL));
            d.setPersistent(false);
            d.setInvulnerable(true);
        });

        markedBlocks.put(block, display);

        long expiryTicks = (long) (markDurationSeconds * 20L);
        UtilServer.runTaskLater(getProgression(), () -> {
            ItemDisplay existing = markedBlocks.remove(block);
            if (existing != null) {
                existing.remove();
            }
        }, expiryTicks);
    }

    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!markedBlocks.containsKey(block)) return;

        ItemDisplay display = markedBlocks.remove(block);
        if (display != null) {
            display.remove();
        }

        Player player = event.getPlayer();
        if (!cooldownManager.use(player, "Demolition Charge", cooldown, true)) return;

        double radius = baseRadius + blastRadius.getBonusRadius(player);
        double damage = baseDamage + blastDamage.getBonusDamage(player);
        Location loc = block.getLocation().toCenterLocation();

        block.getWorld().createExplosion(loc, 0f, false, true, player);

        for (Entity entity : block.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity == player) continue;
            if (entity instanceof LivingEntity living) {
                living.damage(damage, player);
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getSkillConfig("baseRadius", 3.0, Double.class);
        baseDamage = getSkillConfig("baseDamage", 4.0, Double.class);
        cooldown = getSkillConfig("cooldown", 10.0, Double.class);
        markDurationSeconds = getSkillConfig("markDurationSeconds", 30.0, Double.class);
    }
}
