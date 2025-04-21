package core.project.chess.infrastructure.dal.util.sql;

import static core.project.chess.infrastructure.dal.util.sql.Util.deleteSurplusComa;

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
        return this;
    }

    public TailInsertBuilder values(int countOfValues) {
        if (countOfValues == 0) throw new IllegalArgumentException("Values can`t be 0.");
        deleteSurplusComa(query);

        query.append(") ").append("VALUES ").append("(");
        for (int i = 0; i < countOfValues; i++) {
            query.append("?");
            if (i < countOfValues - 1) query.append(", ");
        }
        query.append(") ");
        return new TailInsertBuilder(query);
    }
}
