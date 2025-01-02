package core.project.chess.infrastructure.dal.util.sql;

public class JoinBuilder {
    private final StringBuilder query;

    JoinBuilder(StringBuilder query) {
        this.query = query;
    }

    public JoinBuilder join(String table, String condition) {
        query.append("JOIN ").append(table).append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder innerJoin(String table, String condition) {
        query.append("INNER JOIN ").append(table).append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder leftJoin(String table, String condition) {
        query.append("LEFT JOIN ").append(table).append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder rightJoin(String table, String condition) {
        query.append("RIGHT JOIN ").append(table).append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder fullJoin(String table, String condition) {
        query.append("FULL JOIN ").append(table).append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder joinAs(String table, String as, String condition) {
        query.append("JOIN ").append(table).append("AS ").append(as).append(" ").append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder innerJoinAs(String table, String as, String condition) {
        query.append("INNER JOIN ").append(table).append("AS ").append(as).append(" ").append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder leftJoinAs(String table, String as, String condition) {
        query.append("LEFT JOIN ").append(table).append("AS ").append(as).append(" ").append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder rightJoinAs(String table, String as, String condition) {
        query.append("RIGHT JOIN ").append(table).append("AS ").append(as).append(" ").append(" ON ").append(condition).append(" ");
        return this;
    }

    public JoinBuilder fullJoinAs(String table, String as, String condition) {
        query.append("FULL JOIN ").append(table).append("AS ").append(as).append(" ").append(" ON ").append(condition).append(" ");
        return this;
    }

    public ChainedWhereBuilder where(String condition) {
        query.append("WHERE ").append(condition).append(" ");
        return new ChainedWhereBuilder(query);
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
        return this.toString();
    }
}
