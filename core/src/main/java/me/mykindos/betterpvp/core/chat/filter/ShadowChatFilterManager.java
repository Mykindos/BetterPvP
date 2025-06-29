package me.mykindos.betterpvp.core.chat.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
@Singleton
public class ShadowChatFilterManager extends Manager<String> {

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
        log.info("Loading filtered words...");
        filteredWords.clear();

        Statement statement = new Statement("SELECT word FROM filtered_words");
        database.executeQuery(statement, TargetDatabase.GLOBAL).thenAccept(result -> {
            try {
                while (result.next()) {
                    String word = result.getString("word");
                    filteredWords.add(word.toLowerCase());
                }
                log.info("Loaded {} filtered words", filteredWords.size());
            } catch (SQLException e) {
                log.error("Error loading filtered words", e);
            }
        });
    }

    /**
     * Adds a word to the filter list
     *
     * @param word    The word to add
     * @param addedBy UUID of the staff member who added the word
     * @return CompletableFuture that completes when the word is added
     */
    public CompletableFuture<Boolean> addFilteredWord(String word, UUID addedBy) {
        if (word == null || word.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        String lowercaseWord = word.toLowerCase();

        // Check if word already exists in the filter
        if (filteredWords.contains(lowercaseWord)) {
            return CompletableFuture.completedFuture(false);
        }

        // Add to database
        Statement statement = new Statement("INSERT INTO filtered_words (word, created_by) VALUES (?, ?)",
                new StringStatementValue(lowercaseWord),
                addedBy != null ? new UuidStatementValue(addedBy) : new StringStatementValue(null));

        return database.executeUpdate(statement, TargetDatabase.GLOBAL).thenApply(v -> {
            // Add to in-memory cache
            filteredWords.add(lowercaseWord);
            log.info("Added filtered word: {} by {}", lowercaseWord, addedBy);
            return true;
        }).exceptionally(ex -> {
            log.error("Error adding filtered word: {}", lowercaseWord, ex);
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
        Statement statement = new Statement("DELETE FROM filtered_words WHERE word = ?",
                new StringStatementValue(lowercaseWord));

        return database.executeUpdate(statement, TargetDatabase.GLOBAL).thenApply(v -> {
            // Remove from in-memory cache
            filteredWords.remove(lowercaseWord);
            log.info("Removed filtered word: {}", lowercaseWord);
            return true;
        }).exceptionally(ex -> {
            log.error("Error removing filtered word: {}", lowercaseWord, ex);
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
