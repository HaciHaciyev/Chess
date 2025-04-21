package core.project.chess.infrastructure.dal.util.sql;

public class ValuesBuilder {
    private final StringBuilder query;

    ValuesBuilder(StringBuilder query) {
        this.query = query.append(") ");
    }

    public SelectBuilder select() {
        return SelectBuilder.select(query);
    }

    public SelectBuilder selectDistinct() {
        return SelectBuilder.selectDistinct(query);
    }

    public SelectBuilder withAndSelect(String table, String subQuery) {
        return SelectBuilder.with(query, table, subQuery);
    }

    public TailInsertBuilder values(int countOfValues) {
        if (countOfValues == 0) {
            throw new IllegalArgumentException("Values can`t be 0.");
        }

        query.append("VALUES ").append("(");
        for (int i = 0; i < countOfValues; i++) {
            query.append("?");
            if (i < countOfValues - 1) {
                query.append(", ");
            }
        }
        query.append(") ");
        return new TailInsertBuilder(query);
    }
}
