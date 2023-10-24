package me.mykindos.betterpvp.core.combat.combatlog;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerClickCombatLogEvent;
import me.mykindos.betterpvp.core.combat.nms.CombatSheep;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
public class CombatLog {

    private final UUID owner;
    private final List<ItemStack> items;
    private final long expiryTime;
    private final LivingEntity combatLogSheep;

    public CombatLog(Player player, long expiryTime) {
        this.owner = player.getUniqueId();
        this.expiryTime = expiryTime;

        items = new ArrayList<>();
        for(ItemStack itemStack : player.getInventory().getContents()) {
            if(itemStack == null || itemStack.getType() == Material.AIR) continue;

            items.add(itemStack);
        }

        CombatSheep combatSheep = new CombatSheep(player.getLocation());
        combatLogSheep = (LivingEntity) combatSheep.spawn();

        setupSheep(player);
    }

    private void setupSheep(Player player) {
        combatLogSheep.customName(Component.text("Right Click Me! ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .append(Component.text(player.getName(), NamedTextColor.GRAY)));
        combatLogSheep.setCustomNameVisible(true);

    }

    public void onClicked(Player player) {

        if(Bukkit.getPlayer(owner) != null) {
            return; // Safety check, shouldn't ever be true but just in case
        }

        combatLogSheep.remove();
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            combatLogSheep.getLocation().getWorld().dropItemNaturally(combatLogSheep.getLocation(), stack);
        }

        UtilServer.callEvent(new PlayerClickCombatLogEvent(player, this));

        File file = new File("world/playerdata", owner.toString() + ".dat");
        if (file.exists()) {
            if(!file.delete()) {
                log.error("Failed to delete dat file for player {}", owner);
            }
        }
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}
