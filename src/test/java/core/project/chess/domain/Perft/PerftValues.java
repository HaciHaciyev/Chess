package core.project.chess.domain.Perft;

public class PerftValues {
    long nodes;
    long captures;
    long capturesOnPassage;
    long castles;
    long promotions;
    long checks;
    long checkMates;

    private PerftValues(long nodes, long captures, long capturesOnPassage,
            long castles, long promotions, long checks, long checkMates) {
        this.nodes = nodes;
        this.captures = captures;
        this.capturesOnPassage = capturesOnPassage;
        this.castles = castles;
        this.promotions = promotions;
        this.checks = checks;
        this.checkMates = checkMates;
    }

    public static PerftValues newInstance() {
        return new PerftValues(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PerftValues that))
            return false;

        return nodes == that.nodes &&
                captures == that.captures &&
                capturesOnPassage == that.capturesOnPassage &&
                castles == that.castles &&
                promotions == that.promotions &&
                checks == that.checks &&
                checkMates == that.checkMates;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(nodes);
        result = 31 * result + Long.hashCode(captures);
        result = 31 * result + Long.hashCode(capturesOnPassage);
        result = 31 * result + Long.hashCode(castles);
        result = 31 * result + Long.hashCode(promotions);
        result = 31 * result + Long.hashCode(checks);
        result = 31 * result + Long.hashCode(checkMates);
        return result;
    }

    public boolean verify(PerftValues secondPerftValues) {
        if (secondPerftValues == null)
            return false;

        if (nodes != secondPerftValues.nodes)
            return false;
        if (captures != secondPerftValues.captures)
            return false;
        if (capturesOnPassage != secondPerftValues.capturesOnPassage)
            return false;
        if (castles != secondPerftValues.castles)
            return false;
        if (promotions != secondPerftValues.promotions)
            return false;
        if (checks != secondPerftValues.checks)
            return false;
        return checkMates == secondPerftValues.checkMates;
    }
}
