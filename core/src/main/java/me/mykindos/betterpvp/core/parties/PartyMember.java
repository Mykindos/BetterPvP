package me.mykindos.betterpvp.core.parties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
@Setter
public class PartyMember {

    private final UUID uuid;
    private PartyMemberReadyStatus status = PartyMemberReadyStatus.UNKNOWN;

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PartyMember partyMember))
            return false;

        return partyMember.getUuid().equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

}
