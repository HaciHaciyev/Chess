package core.project.chess.infrastructure.dal.util.sql;

public class InsertBuilder {
    final StringBuilder query;

    InsertBuilder() {
        this.query = new StringBuilder();
    }

    private InsertBuilder(StringBuilder query) {
        this.query = query;
    }

    static InsertBuilder with(String table, String subQuery) {
        return new InsertBuilder(new StringBuilder("WITH ").append(table).append(" AS ").append("(").append(subQuery).append(") "));
    }

    public ValuesBuilder into(String table, String... columns) {
        query.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns));
        return new ValuesBuilder(query);
    }

    public ColumnsBuilder into(String table) {
        return new ColumnsBuilder(query.append("INSERT INTO ").append(table).append(" "));
    }

    public TailInsertBuilder defaultValues(String table) {
        query.append("INSERT INTO ").append(table).append(" DEFAULT VALUES ");
        return new TailInsertBuilder(query);
    }

    public TailInsertBuilder defaultValues(String table, String... columns) {
        query.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns)).append(") DEFAULT VALUES ");
        return new TailInsertBuilder(query);
    }
}
