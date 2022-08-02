package me.mykindos.betterpvp.core.framework.inviting;

import lombok.Getter;

@Getter
public class Invite {

    private Invitable inviter;
    private Invitable invitee;
    private String type;
    private long requestTime;
    private long expiry;

    public Invite(Invitable inviter, Invitable invitee, String type, int expiry) {
        this.inviter = inviter;
        this.invitee = invitee;
        this.type = type;
        this.requestTime = System.currentTimeMillis();
        this.expiry = this.requestTime + (expiry * 60000L);
    }

    @Override
    public boolean equals(Object i) {
        if (!(i instanceof Invite invite)) return false;

        return invite.inviter.equals(inviter) && invite.invitee.equals(invitee)
                && invite.expiry == expiry && type.equals(invite.type);
    }

}