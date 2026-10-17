package me.mykindos.betterpvp.clans.commands.arguments;

import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Clan argument errors")
class ClanArgumentExceptionTest {

    @Test
    @DisplayName("a taken clan name is separated from the rest of the message")
    void nameAlreadyExistsHasSpace() {
        assertEquals("Foo is already in use by another Clan",
                ClanArgumentException.NAME_ALREADY_EXISTS.create("Foo").getRawMessage().getString());
    }
}
