package me.mykindos.betterpvp.core.database.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Statement {

    private String query;

    protected List<StatementValue<?>> values = new ArrayList<>();
    private boolean hasWhere;
    private boolean hasOrderBy;
    private boolean hasGroupBy;


    public Statement(String query, StatementValue<?>... values) {
        this.query = query;
        if (values.length > 0) {
            this.values = Arrays.asList(values);
        }
    }

    public Statement(String query, List<StatementValue<?>> values) {
        this.query = query;
        this.values = values;
    }

    public static class StatementBuilder {

        public StatementBuilder queryBase(String base) {
            this.query = base;

            if(values == null) {
                values = new ArrayList<>();
            }
            return this;
        }

        public StatementBuilder where(String column, String operator, StatementValue<?> value) {


            if (!this.hasWhere) {
                this.query += " WHERE " + column + " " + operator + " ?";
                this.hasWhere = true;
            } else {
                this.query += " AND " + column + " " + operator + " ?";
            }

            values.add(value);

            return this;
        }

        public StatementBuilder orderBy(String column, boolean ascending) {
            if (!this.hasOrderBy) {
                this.query += " ORDER BY " + column + (ascending ? " ASC" : " DESC");
                this.hasOrderBy = true;
            } else {
                this.query += ", " + column + (ascending ? " ASC" : " DESC");
            }

            return this;
        }

        public StatementBuilder groupBy(String column) {
            if (!this.hasGroupBy) {
                this.query += " GROUP BY " + column;
                this.hasGroupBy = true;
            } else {
                this.query += ", " + column;
            }

            return this;
        }

        public StatementBuilder limit(int limit) {
            this.query += " LIMIT ?";
            values.add(IntegerStatementValue.of(limit));
            return this;
        }

        public StatementBuilder offset(int offset) {
            this.query += " OFFSET ?";
            values.add(IntegerStatementValue.of(offset));
            return this;
        }

    }

}
