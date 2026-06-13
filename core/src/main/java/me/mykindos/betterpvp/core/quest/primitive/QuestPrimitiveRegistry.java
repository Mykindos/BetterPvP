package me.mykindos.betterpvp.core.quest.primitive;

import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * The built-in catalogue of quest primitives. This is the authoritative list:
 * it is exported to {@code quest_primitives} on boot and is what the admin
 * console offers in its inspector dropdowns. The ids here are the contract that
 * {@code QuestConditions} / {@code QuestActions} / the trigger listener switch on.
 */
@Singleton
public class QuestPrimitiveRegistry {

    private final List<QuestPrimitive> primitives = build();

    public List<QuestPrimitive> all() {
        return primitives;
    }

    private static Map<String, Object> p(String type) {
        return Map.of("type", type);
    }

    private static Map<String, Object> req(String type) {
        return Map.of("type", type, "required", true);
    }

    private static Map<String, Object> intDefault(int def) {
        return Map.of("type", "int", "default", def);
    }

    private static Map<String, Object> contentRef(String contentType) {
        return Map.of("type", "content_ref", "contentType", contentType, "required", true);
    }

    private static List<QuestPrimitive> build() {
        return List.of(
                // ── Triggers ──
                new QuestPrimitive("trigger.zone_enter", "trigger", "Enter zone", Map.of("zone", req("zone_ref"))),
                new QuestPrimitive("trigger.zone_exit", "trigger", "Exit zone", Map.of("zone", req("zone_ref"))),
                new QuestPrimitive("trigger.kill", "trigger", "Kill entity", Map.of("entityType", p("string"), "count", intDefault(1))),
                new QuestPrimitive("trigger.harvest", "trigger", "Harvest resource", Map.of("profession", p("profession_ref"), "count", intDefault(1))),
                new QuestPrimitive("trigger.fish_caught", "trigger", "Catch fish", Map.of("count", intDefault(1))),
                new QuestPrimitive("trigger.profession_xp", "trigger", "Gain profession XP", Map.of("profession", p("profession_ref"), "amount", intDefault(1))),
                new QuestPrimitive("trigger.npc_interact", "trigger", "Talk to NPC", Map.of("npc", req("npc_ref"))),
                new QuestPrimitive("trigger.reach_location", "trigger", "Reach location", Map.of("zone", p("zone_ref"))),

                // ── Conditions / Requirements ──
                new QuestPrimitive("condition.clan_level", "condition", "Clan level ≥", Map.of("level", intDefault(1))),
                new QuestPrimitive("condition.profession_level", "condition", "Profession level ≥", Map.of("profession", p("profession_ref"), "level", intDefault(1))),
                new QuestPrimitive("condition.has_item", "condition", "Has item", Map.of("item", req("item_ref"), "count", intDefault(1))),
                new QuestPrimitive("condition.in_zone", "condition", "In zone", Map.of("zone", req("zone_ref"))),
                new QuestPrimitive("condition.quest_completed", "condition", "Quest completed", Map.of("quest", contentRef("quest"))),
                new QuestPrimitive("condition.expression", "condition", "Expression (JEXL)", Map.of("expression", req("jexl"))),
                new QuestPrimitive("requirement.clan_level", "requirement", "Requires clan level ≥", Map.of("level", intDefault(1))),
                new QuestPrimitive("requirement.profession_level", "requirement", "Requires profession level ≥", Map.of("profession", p("profession_ref"), "level", intDefault(1))),
                new QuestPrimitive("requirement.quest_completed", "requirement", "Requires quest completed", Map.of("quest", contentRef("quest"))),

                // ── Actions ──
                new QuestPrimitive("action.give_item", "action", "Give item", Map.of("item", req("item_ref"), "amount", intDefault(1))),
                new QuestPrimitive("action.give_xp", "action", "Give XP", Map.of("profession", p("profession_ref"), "amount", intDefault(100))),
                new QuestPrimitive("action.give_clan_energy", "action", "Give clan energy", Map.of("amount", intDefault(1))),
                new QuestPrimitive("action.play_sound", "action", "Play sound", Map.of("key", p("string"), "volume", Map.of("type", "float", "default", 1), "pitch", Map.of("type", "float", "default", 1))),
                new QuestPrimitive("action.send_message", "action", "Send message", Map.of("message", p("text"))),
                new QuestPrimitive("action.start_conversation", "action", "Start conversation", Map.of("conversation", contentRef("conversation"))),
                new QuestPrimitive("action.start_cinematic", "action", "Start cinematic", Map.of("cinematic", contentRef("cinematic"))),
                new QuestPrimitive("action.fire_event", "action", "Fire event", Map.of("key", req("string"))),
                new QuestPrimitive("action.teleport", "action", "Teleport", Map.of("zone", req("zone_ref"))),

                // ── Rewards ──
                new QuestPrimitive("reward.loot_table", "reward", "Roll loot table", Map.of("lootTable", contentRef("loot_table"))),
                new QuestPrimitive("reward.item", "reward", "Item", Map.of("item", req("item_ref"), "amount", intDefault(1))),
                new QuestPrimitive("reward.xp", "reward", "XP", Map.of("profession", p("profession_ref"), "amount", intDefault(100))),
                new QuestPrimitive("reward.unlock_quest", "reward", "Unlock quest", Map.of("quest", contentRef("quest")))
        );
    }
}
