package me.mykindos.betterpvp.clans.clans.components;

import lombok.Value;

@Value
public class ClanMember {

    String uuid;
    MemberRank rank;

    public enum MemberRank {
        RECRUIT,
        MEMBER,
        ADMIN,
        OWNER
    }
}
