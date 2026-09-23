package me.mykindos.betterpvp.core.world.settler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkplaceBonusTest {

    private static Settler resident(Roster roster, String workplace, SettlerState state, int morale) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setAssignment(workplace);
        settler.setState(state);
        settler.setMorale(morale);
        roster.getSettlers().add(settler);
        return settler;
    }

    @Test
    void onlyThoseAtWorkThereCount() {
        final Roster roster = new Roster();
        final Settler working = resident(roster, "farm", SettlerState.WORKING, 0);
        resident(roster, "farm", SettlerState.STRIKING, 0);
        resident(roster, "mill", SettlerState.WORKING, 0);

        assertEquals(List.of(working), WorkplaceBonus.residents(roster, "farm"));
    }

    @Test
    void moraleScalesEachShareAndTheTotalIsCapped() {
        final Roster roster = new Roster();
        final Settler happy = resident(roster, "farm", SettlerState.WORKING, 100);
        final Settler sad = resident(roster, "farm", SettlerState.WORKING, -100);

        assertEquals(0.15 + 0.05, WorkplaceBonus.total(List.of(happy, sad), settler -> 0.1, 1), 1e-9);
        assertEquals(0.12, WorkplaceBonus.total(List.of(happy, sad), settler -> 0.1, 0.12), 1e-9);
    }
}
