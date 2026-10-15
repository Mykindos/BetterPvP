package me.mykindos.betterpvp.core.resourcepack;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Reads a channel's release straight from the pack server's public manifest, for servers whose database the pack
 * server does not write to.
 */
@Singleton
public class PackServerReleaseStore implements PackReleaseStore {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Gson gson = new Gson();

    @Inject
    @Config(path = "core.resourcepack.server", defaultValue = "https://packs.brauw.dev")
    private String server;

    @Override
    public Optional<PackRelease> latest(String channel) {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(server + "/manifest/" + channel + ".json"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not reach the pack server at " + server, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading the pack server", ex);
        }
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Pack server answered " + response.statusCode() + " for channel " + channel);
        }
        return Optional.ofNullable(gson.fromJson(response.body(), PackRelease.class));
    }

}
