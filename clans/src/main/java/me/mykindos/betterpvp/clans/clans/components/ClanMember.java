package me.mykindos.betterpvp.clans.clans.components;

public record ClanMember(String uuid, MemberRank rank) {

    public enum MemberRank {
        RECRUIT,
        MEMBER,
        ADMIN,
        OWNER
    }
}
