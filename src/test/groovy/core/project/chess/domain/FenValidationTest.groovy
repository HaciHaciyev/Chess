package core.project.chess.domain

import core.project.chess.domain.chess.util.ChessNotationsValidator
import core.project.chess.domain.chess.value_objects.FromFEN
import core.project.chess.domain.commons.containers.StatusPair
import spock.lang.Specification
import spock.lang.Unroll

class FenValidationTest extends Specification{

    void "success: valid starting position"() {
        given: "a FEN string with standard position"
        def fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "success: valid complex position"() {
        given: "a FEN string with complex position"
        def fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 6"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should reject empty string"() {
        given: "an empty FEN string"
        def fen = ""

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject null input"() {
        given: "null"
        def fen = null

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid piece coordinate"() {
        given: "a FEN string with invalid piece coordinate"
        def fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid active color"() {
        given: "a FEN string with non existent color"
        def fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPP/RNBQKBNR x KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid castling rights"() {
        given: "a FEN string with invalid castling rights"
        def fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqz - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid en passant square"() {
        given: "a FEN string with invalid en passant square"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq h9 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid halfmove clock"() {
        given: "a FEN string with invalid halfmove clock"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - x 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid piece symbol"() {
        given: "a FEN string with invalid piece symbol"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid row length"() {
        given: "a FEN string with invalid row length"
        String fen = "rnbqkbnr/ppppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid number of rows"() {
        given: "a FEN string with invalid number of rows"
        String fen = "rnbqkbnr/pppppppp/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject special positions"() {
        given: "empty board and only kings positions"
        String emptyBoard = "8/8/8/8/8/8/8/8 w - - 0 1"
        String onlyKings = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

        when: "validating the FEN strings"
        StatusPair<FromFEN> result1 = ChessNotationsValidator.validateFEN(emptyBoard)
        StatusPair<FromFEN> result2 = ChessNotationsValidator.validateFEN(onlyKings)

        then: "both validations should fail"
        !result1.status()
        !result2.status()
    }

    @Unroll
    void "should accept valid castling rights: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w K - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w k - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w kq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w qk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Qq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w qQ - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w kK - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"
        ]
    }

    @Unroll
    void "should accept valid en passant positions: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        ]
    }

    @Unroll
    void "should reject malformed FEN input: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 extra",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                "rnbqkbnr/pppppppp/8/8/8//PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1",
                "/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        ]
    }

    void "should reject extreme board positions"() {
        given: "extreme board positions"
        String allWhitePieces = "RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP w - - 0 1"
        String justKings = "k7/8/8/8/8/8/8/K7 w - - 0 1"

        when: "validating the FEN strings"
        StatusPair<FromFEN> result1 = ChessNotationsValidator.validateFEN(allWhitePieces)
        StatusPair<FromFEN> result2 = ChessNotationsValidator.validateFEN(justKings)

        then: "both validations should fail"
        !result1.status()
        !result2.status()
    }

    @Unroll
    void "should accept boundary values: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 999 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 9999"
        ]
    }

    @Unroll
    void "should reject special characters: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\n",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\t",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 "
        ]
    }

    void "should reject invalid empty board"() {
        given: "an empty board FEN"
        String fen = "8/8/8/8/8/8/8/8 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid kings only"() {
        given: "a FEN with only kings"
        String fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should accept valid Sicilian Defense position"() {
        given: "a valid Sicilian Defense FEN"
        String fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should accept valid Ruy Lopez position"() {
        given: "a valid Ruy Lopez FEN"
        String fen = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should accept valid Queens Gambit position"() {
        given: "a valid Queens Gambit FEN"
        String fen = "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should accept valid Kings Indian Defense position"() {
        given: "a valid Kings Indian Defense FEN"
        String fen = "rnbqkb1r/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 1 3"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should accept valid pawn endgame position"() {
        given: "a valid pawn endgame FEN"
        String fen = "8/3k4/8/2KP4/8/8/8/8 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should accept valid rook endgame position"() {
        given: "a valid rook endgame FEN"
        String fen = "8/8/8/3k4/8/8/3K4/4R3 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should reject invalid material"() {
        given: "a FEN with invalid material"
        String fen = "8/8/3k4/8/2B5/8/3K4/8 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should accept valid queen endgame position"() {
        given: "a valid queen endgame FEN"
        String fen = "8/8/3k4/8/2Q5/8/3K4/8 w - - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }

    void "should reject invalid too many white pawns"() {
        given: "a FEN with too many white pawns"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/PPPPPPPP w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too many black pawns"() {
        given: "a FEN with too many black pawns"
        String fen = "pppppppp/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too many white kings"() {
        given: "a FEN with too many white kings"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBKKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too many black kings"() {
        given: "a FEN with too many black kings"
        String fen = "rnbkkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too many white queens"() {
        given: "a FEN with too many white queens"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQQBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid no white king"() {
        given: "a FEN with no white king"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid no black king"() {
        given: "a FEN with no black king"
        String fen = "rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid pawns on first rank"() {
        given: "a FEN with pawns on first rank"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/8/PPPPPPPP w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid pawns on last rank"() {
        given: "a FEN with pawns on last rank"
        String fen = "pppppppp/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    @Unroll
    void "should reject invalid en passant square format: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq i4 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq 4e 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq ee 0 1"
        ]
    }

    @Unroll
    void "should reject invalid row separators: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()

        where:
        fen << [
                "rnbqkbnr\\pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr|pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr,pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        ]
    }

    @Unroll
    void "should reject invalid piece numbers: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/0/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/18/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        ]
    }

    @Unroll
    void "should accept valid various castling rights: #fen"() {
        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()

        where:
        fen << [
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w K - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w k - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Qq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"
        ]
    }

    void "should reject invalid missing spaces"() {
        given: "a FEN with missing spaces"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR wKQkq-01"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid extra spaces"() {
        given: "a FEN with extra spaces"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR  w  KQkq  -  0  1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid wrong section count"() {
        given: "a FEN with wrong section count"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid characters in board"() {
        given: "a FEN with invalid characters in board"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQXBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too many squares in row"() {
        given: "a FEN with too many squares in row"
        String fen = "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid too few squares in row"() {
        given: "a FEN with too few squares in row"
        String fen = "rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid castling rights with invalid characters"() {
        given: "a FEN with invalid castling rights"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqA - 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should reject invalid en passant square with invalid rank"() {
        given: "a FEN with invalid en passant square"
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq a9 0 1"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should fail"
        !result.status()
    }

    void "should accept valid FEN position"() {
        given: "a valid FEN position"
        String fen = "rnbqkbnr/ppp2ppp/4p3/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 0 3"

        when: "validating the FEN"
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen)

        then: "validation should pass"
        result.status()
    }
}
