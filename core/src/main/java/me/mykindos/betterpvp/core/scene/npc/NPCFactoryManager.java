package me.mykindos.betterpvp.core.scene.npc;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

/**
 * Stores all {@link NPCFactory} instances registered by loaded modules, keyed by factory name.
 * Used by the {@code /npc spawn} command to enumerate available NPC types.
 */
@Singleton
public final class NPCFactoryManager extends Manager<String, NPCFactory> {
}
