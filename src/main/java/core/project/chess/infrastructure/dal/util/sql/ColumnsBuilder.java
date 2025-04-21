package core.project.chess.infrastructure.dal.util.sql;

public class ColumnsBuilder {
    private final StringBuilder query;

    ColumnsBuilder(StringBuilder query) {
        this.query = query.append("(");
    }

    public ValuesBuilder columns(String... columns) {
        query.append(String.join(", ", columns));
        return new ValuesBuilder(query);
    }

    public ColumnsBuilder column(String column) {
        query.append(column).append(", ");
        return new ColumnsBuilder(query);
    }
}
