package discourje.examples.games.impl.chess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    public static String STOCKFISH = "";
    public static int MAX_TURNS = 5899; // https://chess.stackexchange.com/questions/4113
    public static int TURNS_PER_PLAYER = 40;
    public static long TIME_PER_PLAYER = 1 * 60 * 1000;

    private final boolean simple;
    private final Process p;
    private final BufferedReader in;
    private final PrintWriter out;

    private final List<String> moves = new ArrayList<>();
    private String ponderMove;

    private int remainingTurns = TURNS_PER_PLAYER;
    private long remainingTime = TIME_PER_PLAYER;

    public Engine(boolean simple) {
        try {
            this.simple = simple;
            p = Runtime.getRuntime().exec(STOCKFISH);
            out = new PrintWriter(p.getOutputStream());
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void kill() {
        p.destroy();
    }

    public String turn(String opponentMove) {
        if (remainingTurns == 0 || remainingTime < 0 || moves.size() >= MAX_TURNS) {
            return "(none)";
        }

        if (opponentMove != null) {
            moves.add(opponentMove);
        }

        if (ponderMove == null) {
            position(false);
            go(false);
        }
        if (ponderMove != null && ponderMove.equals(opponentMove)) {
            ponderhit();
        }
        if (ponderMove != null && !ponderMove.equals(opponentMove)) {
            stop();
            readLinesUntil("bestmove");
            position(false);
            go(false);
        }

        ponderMove = null;
        String move = null;
        long turnTime = -1;

        var lines = readLinesUntil("bestmove");
        for (int i = lines.size() - 1; i >= 0; i--) {
            var line = lines.get(i);
            var tokens = line.split(" ");
            for (int j = 0; j < tokens.length; j++) {
                if (move == null && tokens[j].equals("bestmove") && j + 1 < tokens.length) {
                    move = tokens[j + 1];
                }
                if (ponderMove == null && tokens[j].equals("ponder") && j + 1 < tokens.length) {
                    ponderMove = tokens[j + 1];
                }
                if (turnTime == -1 && tokens[j].equals("time") && j + 1 < tokens.length) {
                    turnTime = Long.parseLong(tokens[j + 1]);
                }
                if (move != null && ponderMove != null && turnTime > 0) {
                    break;
                }
            }
            if (move != null && ponderMove != null && turnTime > 0) {
                break;
            }
        }

        moves.add(move);
        remainingTime -= turnTime;
        remainingTurns--;

        position(false);
        d();
        for (String line : readLinesUntil("readyok")) {
            if (!line.equals("readyok")) {
                System.out.println(line);
            }
        }

        if (ponderMove != null) {
            position(true);
            go(true);
        }

        return move;
    }

    private List<String> readLinesUntil(String prefix) {
        try {
            List<String> lines = new ArrayList<>();
            String line = "";
            while (!line.startsWith(prefix) && (line = in.readLine()) != null) {
                lines.add(line);
            }
            return lines;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void position(boolean includePonderMove) {
        out.print("position startpos moves");
        for (String s : moves) {
            out.print(" " + s);
        }
        out.print(includePonderMove && ponderMove != null ? " " + ponderMove : "");
        out.println();
        out.flush();
    }

    private void go(boolean ponder) {
        out.print("go");
        if (!simple) {
            out.print(ponder ? " ponder" : "");
            out.print((moves.size() % 2 == 0 ? " wtime " : " btime ") + remainingTime);
            out.print(" movestogo " + remainingTurns);
        }
        out.println();
        out.flush();
    }

    private void stop() {
        out.println("stop");
        out.flush();
    }

    private void ponderhit() {
        out.println("ponderhit");
        out.flush();
    }

    private void d() {
        out.println("d");
        out.println("isready");
        out.flush();
    }
}
