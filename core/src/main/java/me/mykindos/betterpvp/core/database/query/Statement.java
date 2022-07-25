package me.mykindos.betterpvp.core.database.query;

public class Statement {

    private String query;
    private StatementValue[] values;

    public Statement(String query, StatementValue... values){
        this.query = query;
        this.values = values;
    }

    public String getQuery() {
        return query;
    }

    public StatementValue[] getValues() {
        return values;
    }
}
