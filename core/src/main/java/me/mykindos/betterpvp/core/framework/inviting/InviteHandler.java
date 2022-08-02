package me.mykindos.betterpvp.core.framework.inviting;

import com.google.inject.Singleton;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class InviteHandler {

    @Getter
    private final List<Invite> invites;

    public InviteHandler(){
        invites = new ArrayList<>();
    }

    /**
     * Create a valid invite for x minutes
     *
     * @param inviter The inviter
     * @param invitee The invitee
     * @param expiry  Time in minutes until invite is no longer valid
     * @return true if invite does not already exist
     */
    public boolean createInvite(Invitable inviter, Invitable invitee, String type, int expiry) {

        Invite invite = new Invite(inviter, invitee, type, expiry);
        if (!invites.contains(invite)) {
            invites.add(invite);
            return true;
        }

        return false;
    }


    /**
     * @param inviter
     * @param invitee
     * @return Returns true if the invite was found and valid
     */
    public boolean removeInvite(Invitable inviter, Invitable invitee, String type) {
        return invites.removeIf(i -> i.getInviter().equals(inviter) && i.getInvitee().equals(invitee) && i.getType().equals(type));
    }

    /**
     * Check if there is a valid invite between two Invitable
     *
     * @param invitee The invitee
     * @param inviter The inviter
     * @return Returns true if a valid invite exists
     */
    public boolean isInvited(Invitable invitee, Invitable inviter, String type) {
        if(invitee == null || inviter == null) return false;
        return invites.stream().anyMatch(i -> i.getInvitee().equals(invitee) && i.getInviter().equals(inviter) && i.getType().equals(type));
    }

}
