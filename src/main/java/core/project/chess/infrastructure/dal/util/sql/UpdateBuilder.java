package core.project.chess.infrastructure.dal.util.sql;

public class UpdateBuilder {
    final StringBuilder qb;

    UpdateBuilder() {
        this.qb = new StringBuilder("UPDATE ");
    }
}
