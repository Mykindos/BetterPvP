package me.mykindos.betterpvp.core.framework.inviting;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@BPvPListener
public class InviteListener implements Listener {

    private final InviteHandler inviteHandler;

    @Inject
    public InviteListener(InviteHandler inviteHandler) {
        this.inviteHandler = inviteHandler;
    }

    @UpdateEvent(delay = 5000)
    public void clearExpiredInvites(){
        inviteHandler.getInvites().removeIf(invite -> invite.getExpiry() <= System.currentTimeMillis());
    }
}
