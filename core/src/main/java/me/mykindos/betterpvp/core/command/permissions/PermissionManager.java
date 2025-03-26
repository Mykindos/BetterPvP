package me.mykindos.betterpvp.core.command.permissions;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
@CustomLog
public class PermissionManager extends Manager<PermissionAttachment> {

    private final Map<Rank, Set<String>> permissions = new EnumMap<>(Rank.class);

    public void loadPermissions(ExtendedYamlConfiguration config) {
        permissions.clear();
        final Set<String> permissionSet = new HashSet<>();
        for (Rank rank : Rank.values()) {
            permissionSet.addAll(config.getStringList(rank.name()));
            permissions.put(rank, Set.copyOf(permissionSet));
        }
        JavaPlugin.getPlugin(Core.class).saveConfig();
    }

    public void onJoin(Client client, Player player) {
        final Set<String> playerPermissions = permissions.get(client.getRank());
        //first, unset all permissions the player should not have

        if (!player.isOp()) {
            player.getEffectivePermissions().forEach(permissionAttachmentInfo -> {
                if (!playerPermissions.contains(permissionAttachmentInfo.getPermission())) {
                    final PermissionAttachment attachment = permissionAttachmentInfo.getAttachment();
                    if (attachment != null) {
                        attachment.unsetPermission(permissionAttachmentInfo.getPermission());
                        log.info("Removing Permission: {}", permissionAttachmentInfo.getPermission()).submit();
                    }
                }
            });
        }

        //set the permissions they should have
        final PermissionAttachment attachment = player.addAttachment(JavaPlugin.getPlugin(Core.class));
        playerPermissions.forEach(permission -> {
            attachment.setPermission(permission, true);
        });
        addObject(player.getUniqueId(), attachment);
        player.recalculatePermissions();
        player.updateCommands();
    }

    public void onQuit(Player player) {
        final PermissionAttachment attachment = getObject(player.getUniqueId()).orElseThrow();
        player.removeAttachment(attachment);
        removeObject(player.getUniqueId().toString());
    }
}
