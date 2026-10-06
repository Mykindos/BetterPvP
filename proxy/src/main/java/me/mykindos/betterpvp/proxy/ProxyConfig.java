package me.mykindos.betterpvp.proxy;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ProxyConfig {

    private final String orchestrationBaseUrl;
    private final long orchestrationRequestTimeoutMs;
    private final boolean packsEnabled;
    private final String packChannel;
    private final String packDatabaseUrl;
    private final String packDatabaseUser;
    private final String packDatabasePassword;

    private ProxyConfig(
            String orchestrationBaseUrl,
            long orchestrationRequestTimeoutMs,
            boolean packsEnabled,
            String packChannel,
            String packDatabaseUrl,
            String packDatabaseUser,
            String packDatabasePassword
    ) {
        this.orchestrationBaseUrl = orchestrationBaseUrl;
        this.orchestrationRequestTimeoutMs = orchestrationRequestTimeoutMs;
        this.packsEnabled = packsEnabled;
        this.packChannel = packChannel;
        this.packDatabaseUrl = packDatabaseUrl;
        this.packDatabaseUser = packDatabaseUser;
        this.packDatabasePassword = packDatabasePassword;
    }

    public static ProxyConfig load(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        final Path path = dataDirectory.resolve("config.toml");
        if (Files.notExists(path)) {
            copyBundledConfig(path);
        }

        final TomlParseResult result = Toml.parse(path);
        if (result.hasErrors()) {
            throw new IOException("Invalid TOML in proxy config: " + result.errors());
        }

        return new ProxyConfig(
                result.getString("orchestration_base_url", () -> "http://127.0.0.1:8085/"),
                result.getLong("orchestration_request_timeout_ms", () -> 3000L),
                result.getBoolean("resourcepack.enabled", () -> false),
                result.getString("resourcepack.channel", () -> "production"),
                result.getString("resourcepack.database_url", () -> "jdbc:postgresql://127.0.0.1:5432/betterpvp"),
                result.getString("resourcepack.database_user", () -> ""),
                result.getString("resourcepack.database_password", () -> "")
        );
    }

    public String orchestrationBaseUrl() {
        return orchestrationBaseUrl;
    }

    public long orchestrationRequestTimeoutMs() {
        return orchestrationRequestTimeoutMs;
    }

    public boolean packsEnabled() {
        return packsEnabled;
    }

    public String packChannel() {
        return packChannel;
    }

    public String packDatabaseUrl() {
        return packDatabaseUrl;
    }

    public String packDatabaseUser() {
        return packDatabaseUser;
    }

    public String packDatabasePassword() {
        return packDatabasePassword;
    }

    private static void copyBundledConfig(Path path) throws IOException {
        try (InputStream inputStream = ProxyConfig.class.getResourceAsStream("/config.toml")) {
            if (inputStream == null) {
                throw new IOException("Missing bundled proxy config resource: /config.toml");
            }

            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
