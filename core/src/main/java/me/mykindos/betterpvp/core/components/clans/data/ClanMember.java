package me.mykindos.betterpvp.core.components.clans.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.bukkit.ChatColor;

@Value
public class ClanMember {

    String uuid;
    MemberRank rank;

    public boolean hasRank(MemberRank memberRank){
        return this.rank.getPrivilege() >= memberRank.getPrivilege();
    }

    @Getter
    @AllArgsConstructor
    public enum MemberRank {
        RECRUIT(0),
        MEMBER(1),
        ADMIN(2),
        LEADER(3);

        private final int privilege;
    }

    public String getRoleIcon() {
        return ChatColor.YELLOW + rank.toString().substring(0, 1) + ".";
    }
}
