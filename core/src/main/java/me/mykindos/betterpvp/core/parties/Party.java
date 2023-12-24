package me.mykindos.betterpvp.core.parties;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
public class Party {

    protected UUID partyLeader;
    protected Set<PartyMember> members;

    public Party(UUID leader) {
        this.partyLeader = leader;
        this.members = new HashSet<>();

        members.add(new PartyMember(leader));
    }
}
