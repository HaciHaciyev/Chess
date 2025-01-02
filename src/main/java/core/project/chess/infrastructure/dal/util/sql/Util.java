package core.project.chess.infrastructure.dal.util.sql;

public class Util {

    private Util() {}

    static void deleteSurplusComa(StringBuilder query) {
        if (query.charAt(query.length() - 1) == ',') {
            query.deleteCharAt(query.length() - 1);
        }
    }
}
