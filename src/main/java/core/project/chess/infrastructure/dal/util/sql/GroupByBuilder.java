package core.project.chess.infrastructure.dal.util.sql;

public class GroupByBuilder {
    private final StringBuilder query;

    public GroupByBuilder(StringBuilder query) {
        this.query = query;
    }

    public HavingBuilder having(String condition) {
        query.append("HAVING ").append(condition).append(" ");
        return new HavingBuilder(query);
    }

    public OrderByBuilder orderBy(String column, Order order) {
        query.append("ORDER BY ").append(column).append(" ").append(order).append(" ");
        return new OrderByBuilder(query);
    }

    public OrderByBuilder orderBy(String customOrder) {
        query.append("ORDER BY ").append(customOrder).append(" ");
        return new OrderByBuilder(query);
    }

    public String limitAndOffset() {
        query.append("LIMIT ").append("? ").append("OFFSET ").append("? ");
        return this.query.toString();
    }

    public String limitAndOffset(int limit, int offset) {
        query.append("LIMIT ").append(limit).append(" ").append("OFFSET ").append(offset).append(" ");
        return this.query.toString();
    }

    public String build() {
        return this.query.toString();
    }
}
