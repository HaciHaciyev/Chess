package core.project.chess.infrastructure.dal.util.sql;

import java.util.Objects;

import static core.project.chess.infrastructure.dal.util.sql.Util.deleteSurplusComa;

public class ColumnBuilder {
    private final StringBuilder query;

    public ColumnBuilder(StringBuilder query) {
        this.query = query;
    }

    public ColumnBuilder column(String column) {
        query.append(column).append(" ");
        return new ColumnBuilder(query);
    }

    public ColumnBuilder as(String column) {
        query.append("AS ").append(column).append(", ");
        return new ColumnBuilder(query);
    }

    public JoinBuilder from(String table) {
        deleteSurplusComa(query);

        query.append("FROM ").append(table).append(" ");
        return new JoinBuilder(query);
    }

    public FunctionBuilder count(String column) {
        query.append(", COUNT");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder sum(String column) {
        query.append(", SUM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder avg(String column) {
        query.append(", AVG");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder min(String column) {
        query.append(", MIN");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder max(String column) {
        query.append(", MAX");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder upper(String column) {
        query.append(", UPPER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder lower(String column) {
        query.append(", LOWER");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder concat(String... columns) {
        if (columns.length == 0) {
            throw new IllegalArgumentException("Columns, at least one, required.");
        }

        query.append(", CONCAT(");

        if (columns.length == 1) {
            query.append(columns[0]);
        } else {
            query.append(String.join(", ", columns));
        }
        deleteSurplusComa(query);

        query.append(") ");
        return new FunctionBuilder(query);
    }

    public FunctionBuilder length(String column) {
        query.append(", LENGTH");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    public FunctionBuilder trim(String column) {
        query.append(", TRIM");
        appendColumn(column);
        return new FunctionBuilder(query);
    }

    private void appendColumn(String column) {
        Objects.requireNonNull(column, "Column can`t be null.");
        if (column.isBlank()) {
            throw new IllegalArgumentException("Column can`t be blank.");
        }

        query.append("(").append(column).append(") ");
    }
}
