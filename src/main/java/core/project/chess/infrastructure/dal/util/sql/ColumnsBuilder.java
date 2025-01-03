package core.project.chess.infrastructure.dal.util.sql;

public class ColumnsBuilder {
    private final StringBuilder query;

    ColumnsBuilder(StringBuilder query) {
        this.query = query;
    }

    public ValuesBuilder columns(String... columns) {
        query.append("(").append(String.join(", ", columns)).append(") ");
        return new ValuesBuilder(query);
    }
}
