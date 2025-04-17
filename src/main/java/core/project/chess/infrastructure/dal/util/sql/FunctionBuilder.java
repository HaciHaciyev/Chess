package core.project.chess.infrastructure.dal.util.sql;

import java.util.Objects;

import static core.project.chess.infrastructure.dal.util.sql.Util.deleteSurplusComa;

public class FunctionBuilder {
    private final StringBuilder query;

    FunctionBuilder(StringBuilder query) {
        this.query = query;
    }

    public FunctionBuilder count(String column) {
        query.append("COUNT");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder sum(String column) {
        query.append("SUM");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder avg(String column) {
        query.append("AVG");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder min(String column) {
        query.append("MIN");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder max(String column) {
        query.append("MAX");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder upper(String column) {
        query.append("UPPER");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder lower(String column) {
        query.append("LOWER");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder concat(String... columns) {
        if (columns.length == 0) {
            throw new IllegalArgumentException("At least one column must be provided.");
        }

        query.append("CONCAT(");

        if (columns.length == 1) {
            query.append(columns[0]);
        } else {
            query.append(String.join(", ", columns));
        }

        deleteSurplusComa(query);

        query.append(") ");
        return this;
    }

    public FunctionBuilder length(String column) {
        query.append("LENGTH");
        appendColumn(column);
        return this;
    }

    public FunctionBuilder trim(String column) {
        query.append("TRIM");
        appendColumn(column);
        return this;
    }

    private void appendColumn(String column) {
        Objects.requireNonNull(column, "Column can`t be null.");
        if (column.isBlank()) {
            throw new IllegalArgumentException("Column can`t be blank.");
        }

        query.append("(").append(column).append(") ");
    }

    public InitialWhereBuilder from(String table) {
        query.append("FROM ").append(table).append(" ");
        return new InitialWhereBuilder(query);
    }

    public JoinBuilder fromAs(String table, String alias) {
        query.append(table).append(" AS ").append(alias).append(" ");
        return new JoinBuilder(query);
    }

    public ChainedFromBuilder as(String alias) {
        query.append("AS ").append(alias).append(" ");
        return new ChainedFromBuilder(query);
    }
}
