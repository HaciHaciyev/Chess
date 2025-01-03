package core.project.chess.infrastructure.dal.util.sql;

public class InitialWhereBuilder {
    private final StringBuilder query;

    InitialWhereBuilder(StringBuilder query) {
        this.query = query;
    }

    public ChainedWhereBuilder where(String condition) {
        query.append("WHERE ").append(condition).append(" ");
        return new ChainedWhereBuilder(query);
    }

    public ChainedWhereBuilder whereNot(String condition) {
        query.append("WHERE NOT ").append(condition).append(" ");
        return new ChainedWhereBuilder(query);
    }

    public ChainedWhereBuilder whereIn(String condition, int countOfValues) {
        if (countOfValues == 0) {
            return where(condition);
        }

        query.append("WHERE ").append(condition).append(" ");

        query.append("IN(");
        for (int i = 0; i < countOfValues - 1; i++) {
            query.append("?");
            if (i < countOfValues - 1) {
                query.append(", ");
            }
        }
        query.append(") ");

        return new ChainedWhereBuilder(query);
    }

    public GroupByBuilder groupBy(String... columns) {
        query.append("GROUP BY ").append(String.join(", ", columns)).append(" ");
        return new GroupByBuilder(query);
    }

    public GroupByBuilder groupByf(String condition) {
        query.append("GROUP BY ").append(condition).append(" ");
        return new GroupByBuilder(query);
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
