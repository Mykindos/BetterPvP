package me.mykindos.betterpvp.core.world.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Rolls new settlers. The template fixes rarity, profession and source, and the rest comes from the table: a name, a
 * history line for the source, a specialty of the profession, and as many traits as the rarity allows.
 */
@Singleton
public class SettlerGenerator {

    private final ProfessionRegistry professions;
    private final TraitRegistry traits;

    @Inject
    public SettlerGenerator(@NotNull ProfessionRegistry professions, @NotNull TraitRegistry traits) {
        this.professions = professions;
        this.traits = traits;
    }

    public @NotNull Settler roll(@NotNull SettlerTemplate template, @NotNull SettlerTable table, @NotNull Random random) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(template.getRarity());
        settler.setName(name(table, random, settler.getId()));

        final List<String> histories = table.getHistories().getOrDefault(template.getSource(), List.of());
        if (!histories.isEmpty()) {
            settler.setHistory(pick(histories, random));
            settler.setHistoryArgs(new ArrayList<>(template.getHistoryArgs()));
        }

        if (template.getProfession() != null) {
            final Profession profession = professions.find(template.getProfession())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown profession " + template.getProfession()));
            settler.setProfession(profession.getId());
            if (!profession.getSpecialties().isEmpty()) {
                settler.setSpecialty(pick(profession.getSpecialties(), random));
            }
        }

        settler.setTraits(rollTraits(template, table.rarity(template.getRarity()), random));
        return settler;
    }

    private @NotNull List<String> rollTraits(@NotNull SettlerTemplate template, @NotNull RarityNumbers numbers,
                                             @NotNull Random random) {
        final List<String> rolled = new ArrayList<>();
        for (int i = 0; i < numbers.getTraits(); i++) {
            final List<Trait> open = traits.all().stream()
                    .filter(trait -> trait.canRoll(template.getRarity(), template.getProfession()))
                    .filter(trait -> !rolled.contains(trait.getId()))
                    .toList();
            final List<Trait> tradeOffs = open.stream().filter(Trait::isTradeOff).toList();
            final List<Trait> plain = open.stream().filter(trait -> !trait.isTradeOff()).toList();

            final boolean wantsTradeOff = random.nextDouble() < numbers.getTradeOffChance();
            final List<Trait> pool = wantsTradeOff && !tradeOffs.isEmpty() || plain.isEmpty() ? tradeOffs : plain;
            if (pool.isEmpty()) {
                break;
            }
            rolled.add(pick(pool, random).getId());
        }
        return rolled;
    }

    private static @NotNull String name(@NotNull SettlerTable table, @NotNull Random random, @NotNull UUID id) {
        if (table.getFirstNames().isEmpty()) {
            return id.toString().substring(0, 8);
        }
        final String first = pick(table.getFirstNames(), random);
        return table.getBynames().isEmpty() ? first : first + " " + pick(table.getBynames(), random);
    }

    private static <T> @NotNull T pick(@NotNull List<T> options, @NotNull Random random) {
        return options.get(random.nextInt(options.size()));
    }
}
