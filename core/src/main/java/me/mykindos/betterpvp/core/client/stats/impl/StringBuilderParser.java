package me.mykindos.betterpvp.core.client.stats.impl;

import joptsimple.internal.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;


public class StringBuilderParser<T> {

    public static final String DEFAULT_INTRA_SEQUENCE_DELIMITER = "=";
    public static final String DEFAULT_SEQUENCE_DELIMITER = "==";

    private final String intraSequenceDelimiter;
    private final String sequenceDelimiter;

    final List<List<BiFunction<T, String, T>>> sequenceParsers;

    @SafeVarargs
    public StringBuilderParser(List<BiFunction<T, String, T>>... sequenceParsers) {
        this(DEFAULT_INTRA_SEQUENCE_DELIMITER, DEFAULT_SEQUENCE_DELIMITER, sequenceParsers);
    }

    @SafeVarargs
    public StringBuilderParser(String intraSequenceDelimiter, String sequenceDelimiter, List<BiFunction<T, String, T>>... sequenceParsers) {
        this.intraSequenceDelimiter = intraSequenceDelimiter;
        this.sequenceDelimiter = sequenceDelimiter;
        this.sequenceParsers = Arrays.stream(sequenceParsers).toList();
    }

    /**
     * Parses a string using the sequence parsers provided
     * @param builder
     * @param string
     * @return
     * @throws IllegalArgumentException if the given string is not buildable by the builder
     */
    public T parse(@NotNull T builder, @NotNull String string) {
        final String[] sequenceStrings = string.split(sequenceDelimiter);
        for (int i = 0; i < sequenceStrings.length; i++) {
            final String sequenceString = sequenceStrings[i];
            final List<BiFunction<T, String, T>> parsers = sequenceParsers.get(i);
            final String[] elementStrings = sequenceString.split(intraSequenceDelimiter);
            for (int j = 0; j < elementStrings.length; j++) {
                final String elementString = elementStrings[j];
                final BiFunction<T, String, T> parser = parsers.get(j);
                builder = parser.apply(builder, elementString);
            }
        }
        return builder;
    }

    /**
     * Given an array of sequences (made up of list of strings), create a unified string
     * Elements that are empty or null stop the parsing of that sequence and continues to the next one
     * @param sequences
     * @return
     * @throws NoSuchElementException if any sequence has no elements
     */
    @SafeVarargs
    public final String asString(final List<String>... sequences) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sequences.length; i++) {
            final List<String> sequence = sequences[i];
            final String firstElement = sequence.getFirst();
            builder.append(firstElement);
            for (int j = 1; j < sequence.size(); j++) {
                final String element = sequence.get(j);
                if (Strings.isNullOrEmpty(element)) break;
                builder.append(intraSequenceDelimiter);
                builder.append(element);
            }

            if (i + 1 < sequences.length) {
                builder.append(sequenceDelimiter);
            }
        }
        return builder.toString();
    }
}
