package me.mykindos.betterpvp.core.components.clans.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Arrays;

@Data
@AllArgsConstructor
public class ClanMember {

    String uuid;
    MemberRank rank;

    public boolean hasRank(MemberRank memberRank) {
        return this.rank.getPrivilege() >= memberRank.getPrivilege();
    }

    public String getRoleIcon() {
        return "<yellow>" + rank.toString().charAt(0) + ".";
    }

    public String getName() {
        String name = rank.name().toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @Getter
    @AllArgsConstructor
    public enum MemberRank {
        RECRUIT(1),
        MEMBER(2),
        ADMIN(3),
        LEADER(4);

        private final int privilege;

        public static MemberRank getRankByPrivilege(int privilege) {
            return Arrays.stream(values()).filter(memberRank -> memberRank.getPrivilege() == privilege).findFirst().orElse(null);
        }

        public String getName() {
            String name = name().toLowerCase();
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        public boolean hasRank(MemberRank memberRank) {
            return this.privilege >= memberRank.privilege;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ClanMember clanMember))
            return false;

        return this.uuid.equals(clanMember.getUuid());
    }


}
