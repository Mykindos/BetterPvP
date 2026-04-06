package me.mykindos.betterpvp.orchestration.service;

import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.QueueTargetType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManagedTargetResolver {

    private final List<ManagedTargetRule> rules;

    public ManagedTargetResolver(List<ManagedTargetRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<QueueTarget> resolve(String serverName) {
        for (ManagedTargetRule rule : rules) {
            final Matcher matcher = rule.pattern().matcher(serverName);
            if (matcher.matches()) {
                final String resolvedServerName = rule.resolveServerName(serverName, matcher);
                return Optional.of(new QueueTarget(
                        rule.targetType().name().toLowerCase(Locale.ROOT) + ":" + resolvedServerName.toLowerCase(Locale.ROOT),
                        rule.targetType(),
                        resolvedServerName
                ));
            }
        }

        return Optional.empty();
    }

    public record ManagedTargetRule(Pattern pattern, QueueTargetType targetType, String serverNameTemplate) {

        public String resolveServerName(String serverName, Matcher matcher) {
            if (serverNameTemplate == null || serverNameTemplate.isBlank()) {
                return serverName;
            }

            try {
                return matcher.replaceFirst(serverNameTemplate);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid managed target server_name_template '" + serverNameTemplate
                        + "' for pattern '" + pattern.pattern() + "'", ex);
            }
        }
    }
}
