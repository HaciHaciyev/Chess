package core.project.chess.infrastructure.dal.util.sql;

public class OrderByBuilder {
    private final StringBuilder query;

    public OrderByBuilder(StringBuilder query) {
        this.query = query;
    }

    public String limitAndOffset(int limit, int offset) {
        query.append("LIMIT ").append(limit).append(" ").append("OFFSET ").append(offset).append(" ");
        return this.query.toString();
    }

    public String build() {
        query.append(";");
        return this.query.toString();
    }
}
