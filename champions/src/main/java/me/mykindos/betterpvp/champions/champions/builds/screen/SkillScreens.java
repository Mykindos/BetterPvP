package me.mykindos.betterpvp.champions.champions.builds.screen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildEdits;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.champions.combat.RoleBowService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.dialog.screen.ActionHandler;
import me.mykindos.betterpvp.core.menu.dialog.screen.ActionResult;
import me.mykindos.betterpvp.core.menu.dialog.screen.GuiScreens;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The skill menus as dialog screens: the class screen ({@code gui/classes.json}), the build editor
 * ({@code gui/build_editor.json}) and the rename prompt ({@code gui/build_rename.json}). Every action of the three
 * screens is bound when the class screen opens, since screens opened on top of it share its bindings.
 */
@Singleton
public class SkillScreens {

    private static final String NAMESPACE = "champions";
    private static final String CLASSES = "champions:classes";
    private static final String EDITOR = "build_editor";
    private static final String RENAME = "build_rename";
    private static final List<SkillType> SLOTS = List.of(SkillType.SWORD, SkillType.AXE, SkillType.BOW,
            SkillType.PASSIVE_A, SkillType.PASSIVE_B, SkillType.GLOBAL);
    private static final int TRAIT_SLOTS = 4;
    private static final int BUILDS = 4;
    /** Skills one slot panel of the editor holds: two rows of five. */
    private static final int SLOT_SKILLS = 10;
    private static final int ROW_SKILLS = 5;
    /** Room for the large class name before the bow and heart buttons. Longer names draw at the menu size. */
    private static final int HEADING_WIDTH = 96;
    private static final String SKILL_ICONS = "menu/gui/classes/skills/";

    private final GuiScreens screens;
    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;
    private final RoleBowService roleBowService;
    private final RoleManager roleManager;
    private final Champions champions;
    /** One pending save per edited build, so a burst of clicks writes the build once, after the last click. */
    private final Map<RoleBuild, BukkitTask> saves = new IdentityHashMap<>();

    @Inject
    public SkillScreens(GuiScreens screens, BuildManager buildManager, ChampionsSkillManager skillManager,
                        RoleBowService roleBowService, RoleManager roleManager, Champions champions) {
        this.screens = screens;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.roleBowService = roleBowService;
        this.roleManager = roleManager;
        this.champions = champions;
    }

    /** Opens the class screen with a tab for every class, starting on the class the player wears or wore last. */
    public void openClasses(Player player) {
        open(player, roleManager.getRole(player).or(() -> roleManager.getLastEquippedRole(player)).orElse(Role.DEFAULT),
                true, null);
    }

    /**
     * Opens the class screen for one class only, without class tabs.
     *
     * @param extra a button to add to every build row, or null for none
     */
    public void openClass(Player player, Role role, @Nullable BuildExtra extra) {
        open(player, role, false, extra);
    }

    private void open(Player player, Role role, boolean tabs, @Nullable BuildExtra extra) {
        buildManager.getObject(player.getUniqueId()).ifPresent(builds -> screens.open(player, CLASSES,
                withExtra(classState(player, builds, role, tabs, activeId(builds, role)), extra, player), bindings(builds, extra)));
    }

    private static Map<String, Object> withExtra(Map<String, Object> state, @Nullable BuildExtra extra, Player player) {
        state.put("extra", extra != null);
        state.put("extraIcon", extra == null ? "" : extra.getIcon());
        state.put("extraTip", extra == null ? Component.empty()
                : Translations.render(Translations.component(extra.getTooltip()), player.locale()));
        return state;
    }

    private Map<String, ActionHandler> bindings(GamerBuilds builds, @Nullable BuildExtra extra) {
        final Map<String, ActionHandler> bindings = new HashMap<>();
        bindings.put("extra", context -> {
            final Role role = role(context.getState().get("role"));
            final RoleBuild build = builds.getBuild(role, number(context.arg("build"))).orElse(null);
            if (extra == null || build == null) {
                return ActionResult.none();
            }
            screens.close(context.getPlayer());
            extra.getAction().accept(context.getPlayer(), build);
            return ActionResult.none();
        });
        bindings.put("role", context -> {
            final Role role = role(context.arg("role"));
            final boolean tabs = context.getState().get("tabs", true);
            return ActionResult.update(state -> state.asMap().putAll(
                    classState(context.getPlayer(), builds, role, tabs, activeId(builds, role))));
        });
        bindings.put("equip", context -> {
            final Role role = role(context.getState().get("role"));
            final int selected = number(context.getState().get("selected"));
            if (BuildEdits.equip(context.getPlayer(), builds, role, selected)) {
                SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            }
            final boolean tabs = context.getState().get("tabs", true);
            return ActionResult.update(state -> state.asMap().putAll(classState(context.getPlayer(), builds, role, tabs, selected)));
        });
        bindings.put("edit", context -> {
            final Role role = role(context.getState().get("role"));
            final RoleBuild build = builds.getBuild(role, number(context.getState().get("selected"))).orElse(null);
            if (build == null) {
                return ActionResult.none();
            }
            SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            return ActionResult.open(EDITOR, editorState(context.getPlayer(), build, context.getState().get("tabs", true)));
        });
        bindings.put("back", context -> {
            final Role role = role(context.getState().get("role"));
            screens.open(context.getPlayer(), CLASSES, withExtra(classState(context.getPlayer(), builds, role,
                    context.getState().get("tabs", true), number(context.getState().get("build"))), extra, context.getPlayer()),
                    bindings(builds, extra));
            return ActionResult.none();
        });
        bindings.put("pick", context -> {
            final RoleBuild build = edited(builds, context);
            final SkillType slot = SkillType.valueOf(String.valueOf(context.arg("slot")));
            final Skill skill = skills(build.getRole()).getOrDefault(slot, List.of()).stream()
                    .filter(candidate -> candidate.getName().equals(context.arg("skill")))
                    .findFirst().orElse(null);
            if (skill != null && BuildEdits.choose(context.getPlayer(), build, skill)) {
                saveSoon(build);
                SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            } else {
                SoundEffect.WRONG_ACTION.play(context.getPlayer());
            }
            return editorUpdate(context, build);
        });
        bindings.put("reset", context -> {
            final RoleBuild build = edited(builds, context);
            BuildEdits.reset(context.getPlayer(), build);
            saveSoon(build);
            SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            return editorUpdate(context, build);
        });
        bindings.put("randomize", context -> {
            final RoleBuild build = edited(builds, context);
            BuildEdits.randomize(context.getPlayer(), build, skills(build.getRole()));
            saveSoon(build);
            SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            return editorUpdate(context, build);
        });
        bindings.put("rename", context -> {
            final RoleBuild build = edited(builds, context);
            final Map<String, Object> state = new HashMap<>(context.getState().asMap());
            state.put("name", build.getName() == null ? "" : build.getName());
            state.put("error", null);
            return ActionResult.open(RENAME, state);
        });
        bindings.put("cancel", context -> ActionResult.back());
        bindings.put("save", context -> {
            final RoleBuild build = edited(builds, context);
            final String name = String.valueOf(context.getState().get("name", "")).trim();
            if (!BuildEdits.isValidName(name)) {
                return ActionResult.error("champions.menu.rename.error");
            }
            build.setName(name);
            saveSoon(build);
            SoundEffect.HIGH_PITCH_PLING.play(context.getPlayer());
            screens.open(context.getPlayer(), NAMESPACE + ":" + EDITOR,
                    editorState(context.getPlayer(), build, context.getState().get("tabs", true)), bindings(builds, extra));
            return ActionResult.none();
        });
        return bindings;
    }

    /** Saves a build a moment after its last change, replacing any save still waiting for it. */
    private void saveSoon(RoleBuild build) {
        final BukkitTask waiting = saves.remove(build);
        if (waiting != null) {
            waiting.cancel();
        }
        saves.put(build, UtilServer.runTaskLater(champions, () -> {
            saves.remove(build);
            buildManager.getBuildRepository().update(build);
        }, 40L));
    }

    private ActionResult editorUpdate(ActionHandler.Context context, RoleBuild build) {
        final Map<String, Object> fresh = editorState(context.getPlayer(), build, context.getState().get("tabs", true));
        return ActionResult.update(state -> state.asMap().putAll(fresh));
    }

    private RoleBuild edited(GamerBuilds builds, ActionHandler.Context context) {
        return builds.getBuild(role(context.getState().get("role")), number(context.getState().get("build"))).orElseThrow();
    }

    private static int activeId(GamerBuilds builds, Role role) {
        final RoleBuild active = builds.getActiveBuilds().get(role.getName());
        return active == null ? 1 : active.getId();
    }

    private Map<String, Object> classState(Player player, GamerBuilds builds, Role role, boolean tabs, int selected) {
        final Locale locale = player.locale();
        final Map<String, Object> state = new HashMap<>();
        state.put("tabs", tabs);
        state.put("role", role.name().toLowerCase());
        // The name keeps its own colour and weight under the screen's text style, which colours only the root.
        final TextColor colour = role.getColor();
        // A shadow in a dark shade of the class colour, the way the vanilla font shades text, keeps light colours legible.
        final Component name = render(role.getDisplayName(), locale).color(colour).decorate(TextDecoration.BOLD)
                .shadowColor(ShadowColor.shadowColor(colour.red() / 4, colour.green() / 4, colour.blue() / 4, 255));
        state.put("roleName", Component.text().append(name).build());
        state.put("nameLarge", UtilFont.componentWidth(name.font(Resources.Font.UI_LARGE)) <= HEADING_WIDTH);
        state.put("summary", render(role.getSummaryComponent(), locale));
        state.put("selected", selected);
        state.put("bow", role.isUsesBow());
        state.put("bowTip", role.isUsesBow() ? bowTip(role, locale) : Component.empty());
        state.put("healthTip", render(Translations.component("champions.menu.class.health").color(NamedTextColor.GRAY)
                .appendSpace().append(Component.text(UtilFormat.formatNumber(role.getHealth()), NamedTextColor.RED)), locale));
        state.put("stats", Arrays.stream(ClassStat.values()).map(stat -> Map.<String, Object>of(
                "name", render(stat.getDisplayName(), locale),
                "tip", render(stat.getDescription().color(NamedTextColor.GRAY), locale),
                "level", stat.getSteps(role))).toList());

        final List<Trait> traits = skillManager.getTraitsForRole(role);
        state.put("traits", IntStream.range(0, TRAIT_SLOTS).mapToObj(index -> {
            final Map<String, Object> slot = new HashMap<>();
            slot.put("icon", index < traits.size() ? icon(traits.get(index), SkillType.PASSIVE_A) : null);
            slot.put("tip", index < traits.size() ? traitTip(traits.get(index), locale) : Component.empty());
            return slot;
        }).toList());

        final RoleBuild active = builds.getActiveBuilds().get(role.getName());
        state.put("builds", IntStream.rangeClosed(1, BUILDS).mapToObj(id -> builds.getBuild(role, id)).flatMap(Optional::stream)
                .map(build -> Map.<String, Object>of(
                        "id", build.getId(),
                        "name", buildName(build, locale),
                        "active", active != null && active.getId() == build.getId(),
                        "skills", SLOTS.stream().map(type -> slotted(build.getBuildSkill(type), locale)).toList()))
                .toList());
        return state;
    }

    private Map<String, Object> slotted(@Nullable BuildSkill buildSkill, Locale locale) {
        final Map<String, Object> entry = new HashMap<>();
        entry.put("icon", buildSkill == null ? null : icon(buildSkill.getSkill(), buildSkill.getSkill().getType()));
        entry.put("tip", buildSkill == null ? Component.empty() : skillTip(buildSkill.getSkill(), locale));
        return entry;
    }

    private Map<String, Object> editorState(Player player, RoleBuild build, boolean tabs) {
        final Locale locale = player.locale();
        final Map<SkillType, List<Skill>> skills = skills(build.getRole());
        final Map<String, Object> state = new HashMap<>();
        state.put("tabs", tabs);
        state.put("role", build.getRole().name().toLowerCase());
        state.put("build", build.getId());
        state.put("name", buildName(build, locale));
        state.put("slots", SLOTS.stream().map(type -> {
            final BuildSkill slotted = build.getBuildSkill(type);
            final List<Skill> options = skills.getOrDefault(type, List.of());
            final Map<String, Object> entry = new HashMap<>();
            entry.put("type", type.name());
            entry.put("label", render(Translations.component("champions.menu.editor.slot." + type.name().toLowerCase()), locale));
            entry.put("enabled", !options.isEmpty());
            // The folder's icons are declared at 16 and 32 px, so the same name draws the large equipped icon.
            entry.put("icon", slotted == null ? null : icon(slotted.getSkill(), type));
            entry.put("tip", slotted == null ? Component.empty() : skillTip(slotted.getSkill(), locale));
            entry.put("equipped", slotted == null
                    ? render(Translations.component("champions.menu.editor.slot.none"), locale)
                    : render(slotted.getSkill().getDisplayName(), locale));
            final List<Map<String, Object>> tiles = options.stream().limit(SLOT_SKILLS).map(skill -> Map.<String, Object>of(
                    "id", skill.getName(),
                    "icon", icon(skill, type),
                    "tip", skillTip(skill, locale),
                    "equipped", slotted != null && slotted.getSkill().equals(skill))).toList();
            // Up to five tiles fit a row. More split into two rows as even as possible.
            final int rows = tiles.size() > ROW_SKILLS ? 2 : 1;
            final int first = (tiles.size() + rows - 1) / rows;
            entry.put("rows", rows);
            entry.put("row1", tiles.subList(0, first));
            entry.put("row1Count", first);
            entry.put("row2", tiles.subList(first, tiles.size()));
            entry.put("row2Count", tiles.size() - first);
            return entry;
        }).toList());
        return state;
    }

    /** The skills a role can slot, by slot, sorted by name. */
    private Map<SkillType, List<Skill>> skills(Role role) {
        final Map<SkillType, List<Skill>> bySlot = new EnumMap<>(SkillType.class);
        skillManager.getSkillsForRole(role).stream()
                .filter(skill -> skill.getType() != null && skill.isEnabled())
                .sorted(Comparator.comparing(Skill::getName))
                .forEach(skill -> bySlot.computeIfAbsent(skill.getType(), type -> new ArrayList<>()).add(skill));
        return bySlot;
    }

    /** The icon a skill draws with, or its slot's placeholder when the pack has no icon for it yet. */
    private String icon(Skill skill, SkillType slot) {
        final String own = skill.getIcon().value();
        if (screens.hasSprite(NAMESPACE, own, 16, 16)) {
            return own;
        }
        return SKILL_ICONS + switch (slot) {
            case SWORD -> "sword_placeholder";
            case AXE -> "axe_placeholder";
            case BOW -> "bow_placeholder";
            case PASSIVE_B -> "passive_b_placeholder";
            case GLOBAL -> "global_passive_placeholder";
            default -> "passive_a_placeholder";
        };
    }

    private Component skillTip(Skill skill, Locale locale) {
        final List<Component> lines = new ArrayList<>();
        lines.add(skill.getDisplayName().color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        if (skill.getTags() != null) {
            lines.add(skill.getTags());
        }
        lines.add(Component.empty());
        Arrays.stream(skill.getDescription(1)).map(line -> line.colorIfAbsent(NamedTextColor.GRAY)).forEach(lines::add);
        return render(Component.join(JoinConfiguration.newlines(), lines), locale);
    }

    private Component traitTip(Trait trait, Locale locale) {
        final List<Component> lines = new ArrayList<>();
        lines.add(trait.getDisplayName().color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        lines.add(Translations.component("champions.menu.trait.innate").color(NamedTextColor.GRAY));
        lines.add(Component.empty());
        Arrays.stream(trait.getDescription(trait.getTraitLevel())).map(line -> line.colorIfAbsent(NamedTextColor.GRAY)).forEach(lines::add);
        return render(Component.join(JoinConfiguration.newlines(), lines), locale);
    }

    private Component bowTip(Role role, Locale locale) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Translations.component("champions.menu.class.bow").color(NamedTextColor.YELLOW));
        lines.add(Translations.component("champions.menu.class.bow.damage").color(NamedTextColor.GRAY).appendSpace()
                .append(Component.text(UtilFormat.formatNumber(roleBowService.getArrowDamage(role)), NamedTextColor.RED)));
        if (roleBowService.isOnlyWhilePrepared(role)) {
            lines.add(Translations.component("champions.menu.class.bow.skills-only").color(NamedTextColor.GRAY));
        }
        return render(Component.join(JoinConfiguration.newlines(), lines), locale);
    }

    private Component buildName(RoleBuild build, Locale locale) {
        return build.getName() == null
                ? render(Translations.component("champions.menu.build.button.name", Component.text(build.getId())), locale)
                : Component.text(build.getName());
    }

    private static Component render(Component component, Locale locale) {
        return Translations.render(component, locale);
    }

    private static Role role(Object id) {
        return Role.valueOf(String.valueOf(id).toUpperCase());
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : (int) Double.parseDouble(String.valueOf(value));
    }
}
