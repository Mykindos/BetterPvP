package me.mykindos.betterpvp.core.quest;

import lombok.Value;

/** Identifies the participant that owns a quest instance: a type + an id. */
@Value
public class ScopeKey {
    String type;
    String id;
}
