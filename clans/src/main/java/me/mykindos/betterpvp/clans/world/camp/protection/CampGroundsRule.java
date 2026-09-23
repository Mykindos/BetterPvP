package me.mykindos.betterpvp.clans.world.camp.protection;

import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.world.zone.ZoneActionContext;
import me.mykindos.betterpvp.core.world.zone.ZoneRule;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.OptionalLong;
import java.util.Set;

/**
 * The land of a camp never changes. Nobody breaks or places blocks, except members planting and harvesting inside a
 * farm. Containers open for members, and for allies when the camp allows it. Staff who are administrating are left
 * alone.
 */
public final class CampGroundsRule implements ZoneRule {

    private static final Set<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.MELON_STEM, Material.PUMPKIN_STEM, Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP, Material.MELON, Material.PUMPKIN, Material.SUGAR_CANE);

    private final Camps camps;
    private final CampPermissions permissions;
    private final ClientManager clientManager;
    private final boolean farm;

    public CampGroundsRule(@NotNull Camps camps, @NotNull CampPermissions permissions,
                           @NotNull ClientManager clientManager, boolean farm) {
        this.camps = camps;
        this.permissions = permissions;
        this.clientManager = clientManager;
        this.farm = farm;
    }

    @Override
    public @NotNull Event.Result evaluate(@NotNull ZoneActionContext context) {
        if (clientManager.search().online(context.getPlayer()).isAdministrating()) {
            return Event.Result.DEFAULT;
        }

        final Player player = context.getPlayer();
        return switch (context.getInteraction()) {
            case BREAK, PLACE -> farm && camps.isMember(player, player.getWorld()) && isCrop(context.getBlock())
                    ? Event.Result.ALLOW : Event.Result.DENY;
            case INTERACT -> context.getBlock() != null && context.getBlock().getState() instanceof Container
                    && !mayOpenContainers(player)
                    ? Event.Result.DENY
                    : Event.Result.DEFAULT;
            default -> Event.Result.DEFAULT;
        };
    }

    private boolean mayOpenContainers(@NotNull Player player) {
        final OptionalLong clan = camps.clanOf(player.getWorld());
        return clan.isPresent() && permissions.mayOpenContainers(player, clan.getAsLong());
    }

    static boolean isCrop(@Nullable Block block) {
        return block != null && CROPS.contains(block.getType());
    }
}
