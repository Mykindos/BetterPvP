package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The settler numbers as the shipped settlers.yml gives them. */
final class ShippedSettlers {

    private ShippedSettlers() {
    }

    static SettlerConfig config() {
        return config(yaml -> {
        });
    }

    /** The shipped numbers with {@code change} made to them. */
    static SettlerConfig config(Consumer<ExtendedYamlConfiguration> change) {
        final ExtendedYamlConfiguration yaml = new ExtendedYamlConfiguration();
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                ShippedSettlers.class.getResourceAsStream("/configs/settlers.yml")), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        change.accept(yaml);
        final Clans clans = mock(Clans.class);
        when(clans.getConfig("settlers")).thenReturn(yaml);
        when(clans.getReloadables()).thenReturn(new ArrayList<>());
        return new SettlerConfig(clans);
    }
}
