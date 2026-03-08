import java.io.File

object SqlMigrationParser {
    fun extractTableNames(migrationsDir: File): Set<String> {
        val tables = mutableSetOf<String>()

        if (!migrationsDir.exists()) return tables

        migrationsDir.walk()
            .filter { it.isFile && it.extension == "sql" }
            .forEach { sqlFile ->
                val content = sqlFile.readText()

                // Extract CREATE TABLE statements
                val createTableRegex = """CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?["'`]?(\w+)["'`]?\s*\(""".toRegex(RegexOption.IGNORE_CASE)
                createTableRegex.findAll(content).forEach { match ->
                    tables.add(match.groupValues[1])
                }

                // Extract CREATE PROCEDURE statements (stored procedures)
                val createProcRegex = """CREATE\s+(?:OR\s+REPLACE\s+)?PROCEDURE\s+["'`]?(\w+)["'`]?""".toRegex(RegexOption.IGNORE_CASE)
                createProcRegex.findAll(content).forEach { match ->
                    tables.add(match.groupValues[1])
                }

                // Extract CREATE FUNCTION statements
                val createFuncRegex = """CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+["'`]?(\w+)["'`]?""".toRegex(RegexOption.IGNORE_CASE)
                createFuncRegex.findAll(content).forEach { match ->
                    tables.add(match.groupValues[1])
                }
            }

        return tables
    }

    fun generateIncludes(tableNames: Set<String>): String {
        if (tableNames.isEmpty()) return ".*"  // Fallback to all

        return tableNames.joinToString("|") { "^${it}$" }
    }

    fun generateExcludes(): String {
        return ".*_\\d+|.*_\\d+_\\d+|flyway_schema_history"
    }
}