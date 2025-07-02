package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.champions.RoleStat;
import me.mykindos.betterpvp.core.client.stats.listeners.TimedStatListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class RoleStatListener extends TimedStatListener {

    private final RoleManager roleManager;

    @Inject
    public RoleStatListener(ClientManager clientManager, RoleManager roleManager) {
        super(clientManager);
        this.roleManager = roleManager;
    }

    //role is changed during Low, they still have the previous role at this point
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRoleChange(RoleChangeEvent event) {
        doUpdate(event.getPlayer());
        //increment equip of new role, this event cannot be cancelled
        final RoleStat roleStat = RoleStat.builder()
                .action(RoleStat.Action.EQUIP)
                .role(event.getRole())
                .build();
        clientManager.incrementStat(event.getPlayer(), roleStat, 1);
    }

    @Override
    public void onUpdate(Client client, long deltaTime) {
        final Role role = roleManager.getObject(client.getUniqueId()).orElse(null);
        final RoleStat roleStat = RoleStat.builder()
                .action(RoleStat.Action.TIME_PLAYED)
                .role(role)
                .build();
        client.getStatContainer().incrementStat(roleStat, deltaTime);
    }
}
