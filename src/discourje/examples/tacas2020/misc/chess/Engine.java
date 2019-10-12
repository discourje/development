package discourje.examples.tacas2020.misc.chess;

import discourje.java.ClojureCoreAsync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    public static int MAX_MOVES = 5899; // https://chess.stackexchange.com/questions/4113

    public static String STOCKFISH = ""; // "/Users/sung/Desktop/stockfish-10-64";
    public static long TIME = -1; //4 * 60 * 1000;
    public static int MOVES_TO_GO = -1; //= 40;

    private final boolean simple;
    private final Process p;
    private final BufferedReader in;
    private final PrintWriter out;

    private final List<String> moves = new ArrayList<>();
    private String ponderMove;

    private long time = TIME;
    private int movesToGo = MOVES_TO_GO;

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

    public static void main(String[] args) {
        var c1 = ClojureCoreAsync.chan(1);
        var c2 = ClojureCoreAsync.chan(1);

        var tWhite = new Thread(() -> {
            var e = new Engine(false);
            c1.send(e.turn(null));
            while (true) {
                var move = (String) c2.recv();
                if (move.equals("(none)")) {
                    break;
                }
                move = e.turn(move);
                c1.send(move);
                if (move.equals("(none)")) {
                    break;
                }
            }
        });

        var tBlack = new Thread(() -> {
            var e = new Engine(true);
            while (true) {
                var move = (String) c1.recv();
                if (move.equals("(none)")) {
                    break;
                }
                move = e.turn(move);
                c2.send(move);
                if (move.equals("(none)")) {
                    break;
                }
            }
        });

        tWhite.start();
        tBlack.start();
    }

    public void kill() {
        p.destroy();
    }

    public String turn(String opponentMove) {
        if (moves.size() >= MAX_MOVES) {
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

        time -= turnTime;
        movesToGo--;
        if (movesToGo == 0) {
            time = TIME;
            movesToGo = MOVES_TO_GO;
        }

        moves.add(move);

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
            if (TIME >= 0) {
                out.print((moves.size() % 2 == 0 ? " wtime " : " btime ") + time);
            }
            if (MOVES_TO_GO >= 0) {
                out.print(" movestogo " + movesToGo);
            }
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
