package me.mykindos.betterpvp.core.chat.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import org.jooq.exception.DataAccessException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static me.mykindos.betterpvp.core.database.jooq.Tables.FILTERED_WORDS;

@CustomLog
@Singleton
public class ShadowChatFilterManager extends Manager<String, String> {

    private final Database database;
    private final Set<String> filteredWords = ConcurrentHashMap.newKeySet();

    @Inject
    public ShadowChatFilterManager(Database database) {
        this.database = database;
        loadFilteredWords();
    }

    /**
     * Loads all filtered words from the database into memory
     */
    public void loadFilteredWords() {
        log.info("Loading filtered words...").submit();
        filteredWords.clear();

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            try {
                ctx.select(FILTERED_WORDS.WORD).from(FILTERED_WORDS).fetch().forEach(filterRecord -> {
                    filteredWords.add(filterRecord.get(FILTERED_WORDS.WORD).toLowerCase());
                });
            } catch (DataAccessException e) {
                log.error("Error loading filtered words", e).submit();
            }
        });

    }

    /**
     * Adds a word to the filter list
     *
     * @param word    The word to add
     * @param addedBy Client who added the word
     * @return CompletableFuture that completes when the word is added
     */
    public CompletableFuture<Boolean> addFilteredWord(String word, Client addedBy) {
        if (word == null || word.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String lowercaseWord = word.toLowerCase();

        // Check if word already exists in the filter
        if (filteredWords.contains(lowercaseWord)) {
            return CompletableFuture.completedFuture(false);
        }

        return database.getAsyncDslContext().executeAsync(ctx -> {
            try {
                ctx.insertInto(FILTERED_WORDS)
                        .set(FILTERED_WORDS.WORD, lowercaseWord)
                        .set(FILTERED_WORDS.CREATED_BY, addedBy.getId())
                        .execute();

                return true;
            } catch (DataAccessException ex) {
                log.error("Failed to save filtered word: {}", word, ex).submit();
            }

            return false;
        });
    }

    /**
     * Removes a word from the filter list
     *
     * @param word The word to remove
     * @return CompletableFuture that completes when the word is removed
     */
    public CompletableFuture<Boolean> removeFilteredWord(String word) {
        if (word == null || word.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String lowercaseWord = word.toLowerCase();

        // Check if word exists in the filter
        if (!filteredWords.contains(lowercaseWord)) {
            return CompletableFuture.completedFuture(false);
        }

        // Remove from database
        return database.getAsyncDslContext().executeAsync(ctx -> {

            try {
                int deleted = ctx.deleteFrom(FILTERED_WORDS).where(FILTERED_WORDS.WORD.eq(lowercaseWord)).execute();
                log.info("Deleted {} filtered words matching: {}", deleted, lowercaseWord).submit();
                return true;
            } catch (DataAccessException ex) {
                log.error("Failed to delete filtered words matching: {}", lowercaseWord, ex).submit();
            }

            return false;
        });

    }

    /**
     * Gets all filtered words
     *
     * @return Set of filtered words
     */
    public Set<String> getFilteredWords() {
        return new HashSet<>(filteredWords);
    }

    /**
     * Checks if a message contains any filtered words
     *
     * @param message The message to check
     * @return true if the message contains filtered words, false otherwise
     */
    public boolean containsFilteredWord(String message) {
        if (message == null || message.isEmpty() || filteredWords.isEmpty()) {
            return false;
        }

        String lowercaseMessage = message.toLowerCase();

        // Check for partial matches (word is contained within the message)
        for (String word : filteredWords) {
            if (lowercaseMessage.contains(word)) {
                return true;
            }
        }

        return false;
    }

}
