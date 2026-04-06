package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.model.QueueTargetType;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record OrchestrationServiceConfig(
        String host,
        int port,
        long reservationTtlSeconds,
        long boostIntervalSeconds,
        int boostAmount,
        String defaultRank,
        Map<String, RankPolicy> rankPolicies,
        List<ManagedTargetResolver.ManagedTargetRule> managedTargets
) {

    public static OrchestrationServiceConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            copyBundledConfig(path);
        }

        final TomlParseResult result = Toml.parse(path);
        if (result.hasErrors()) {
            throw new IOException("Invalid TOML in orchestration service config: " + result.errors());
        }

        final String defaultRank = normalizeRank(result.getString("default_rank", () -> "PLAYER"));
        final Map<String, RankPolicy> rankPolicies = new LinkedHashMap<>();
        final TomlTable ranksTable = result.getTable("ranks");
        if (ranksTable != null) {
            for (String rankName : ranksTable.keySet()) {
                final TomlTable rankTable = ranksTable.getTable(rankName);
                if (rankTable == null) {
                    continue;
                }

                rankPolicies.put(normalizeRank(rankName), new RankPolicy(
                        (int) rankTable.getLong("priority", () -> 0L),
                        rankTable.getBoolean("bypass", () -> false)
                ));
            }
        }

        rankPolicies.putIfAbsent(defaultRank, new RankPolicy(0, false));
        final List<ManagedTargetResolver.ManagedTargetRule> managedTargets = parseManagedTargets(result.getArray("managed_targets"));

        return new OrchestrationServiceConfig(
                result.getString("host", () -> "0.0.0.0"),
                Math.toIntExact(result.getLong("port", () -> 8085L)),
                result.getLong("reservation_ttl_seconds", () -> 10L),
                result.getLong("boost_interval_seconds", () -> 30L),
                Math.toIntExact(result.getLong("boost_amount", () -> 5L)),
                defaultRank,
                Map.copyOf(rankPolicies),
                List.copyOf(managedTargets)
        );
    }

    public RankPolicy policyForRank(String rankName) {
        if (rankName == null || rankName.isBlank()) {
            return rankPolicies.get(defaultRank);
        }

        return rankPolicies.getOrDefault(normalizeRank(rankName), rankPolicies.get(defaultRank));
    }

    public String normalizeRankName(String rankName) {
        if (rankName == null || rankName.isBlank()) {
            return defaultRank;
        }
        return normalizeRank(rankName);
    }

    private static String normalizeRank(String rankName) {
        return rankName.trim().toUpperCase(Locale.ROOT);
    }

    private static void copyBundledConfig(Path path) throws IOException {
        try (InputStream inputStream = OrchestrationServiceConfig.class.getResourceAsStream("/config.toml")) {
            if (inputStream == null) {
                throw new IOException("Missing bundled orchestration service config resource: /config.toml");
            }

            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<ManagedTargetResolver.ManagedTargetRule> parseManagedTargets(TomlArray array) throws IOException {
        final List<ManagedTargetResolver.ManagedTargetRule> targets = new ArrayList<>();
        if (array == null) {
            return targets;
        }

        for (int i = 0; i < array.size(); i++) {
            final TomlTable table = array.getTable(i);
            if (table == null) {
                continue;
            }

            final String pattern = table.getString("pattern");
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            final String targetType = table.getString("target_type");
            if (targetType == null || targetType.isBlank()) {
                throw new IOException("Missing required target_type for managed target pattern '" + pattern + "'");
            }
            final String serverNameTemplate = table.getString("server_name_template");
            try {
                targets.add(new ManagedTargetResolver.ManagedTargetRule(
                        Pattern.compile(pattern.trim()),
                        QueueTargetType.valueOf(targetType.trim().toUpperCase(Locale.ROOT)),
                        serverNameTemplate == null || serverNameTemplate.isBlank() ? null : serverNameTemplate
                ));
            } catch (PatternSyntaxException ex) {
                throw new IOException("Invalid managed target regex '" + pattern + "'", ex);
            }
        }

        return targets;
    }

    public record RankPolicy(int priority, boolean bypass) {
    }
}
