package com.tstorm.solitaire.server;

import com.tstorm.solitaire.App;
import com.tstorm.solitaire.moves.MasterMoveEvaluator;
import com.tstorm.solitaire.moves.Move;
import com.tstorm.solitaire.moves.MoveEvaluator;
import com.tstorm.solitaire.pieces.Board;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Master {
    private static final int THREE_MINUTES = 3 * 60 * 1000;
    
    private final List<Slave> cluster = new ArrayList<>();
    private final List<MoveResult> results = new ArrayList<>();
    private MasterMoveEvaluator moveEvaluator;
    private volatile boolean listening = true;
    
    public Master startListening() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(App.PORT)) {
                while (listening) {
                    addMove(serverSocket.accept());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return this;
    }
    private void addMove(Socket s) {
        if (moveEvaluator != null) {
            MoveResult moveResult = new MoveResult(s, moveEvaluator);
            synchronized (results) {
                results.add(moveResult);
            }
            moveResult.start();
        }
    }
    
    public void addSlave(Slave s) {
        cluster.add(s);
    }
    
    public void shutdown() {
        for (Slave s : cluster) {
            s.shutdown(true);
        }
    }
    
    public String evaluate(Board b) {
        try {
            moveEvaluator = new MasterMoveEvaluator(b, Optional.empty());
            for (Slave s : cluster) {
                s.shutdown(false);
            }
            JSONArray suggestions = moveEvaluator.evaluate(distributeWork(b));
            final int remainingMoves = remainingMoveCount();
            while (!hasAllResults(remainingMoves)) {
                synchronized (moveEvaluator) {
                    moveEvaluator.wait(THREE_MINUTES);
                }
            }
            // we should be finished writing to results at this point
            for (MoveResult move : results) {
                if (move.result()) {
                    suggestions.put(moveEvaluator.buildSuggestion(move.move()));
                }
            }
            JSONObject object = new JSONObject();
            object.put("suggestions", suggestions);
            return object.toString();
        } catch (JSONException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private List<MoveEvaluator> distributeWork(Board board) {
        List<MoveEvaluator> pendingEvaluations = new ArrayList<>();
        List<Move> moves = board.getMoves();
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            Board copy = new Board(board);
            copy.jump(m.start(), m.jumped(), m.end());
            int pos = i % (cluster.size() + 1); // plus me
            if (pos == 0) {
                // I'll do it
                MoveEvaluator evaluation = new MasterMoveEvaluator(copy, Optional.of(moves.get(i)));
                pendingEvaluations.add(evaluation);
                evaluation.fork();
            } else {
                // Delegate to a worker node
                boolean hasMoreWork = moves.size() - i > cluster.size() + 1;
                cluster.get(pos - 1).sendMove(m);
                cluster.get(pos - 1).sendBoard(board, hasMoreWork);
            }
        }
        // TODO if moves < cluster size
        return pendingEvaluations;
    }
    
    private boolean hasAllResults(int remainingMoves) {
        int i = 0;
        synchronized (results) {
            for (MoveResult result : results) {
                if (!result.isWaitingForResult()) {
                    i += 1;
                }
            }
        }
        return remainingMoves - i == 0 || cluster.size() == 0;
    }
    
    private int remainingMoveCount() {
        if (moveEvaluator != null) {
            int totalMoves = moveEvaluator.getBoard().getMoves().size();
            int totalClusterSize = cluster.size() + 1;
            int remainingMoves = totalMoves / totalClusterSize;
            remainingMoves += (totalMoves % totalClusterSize > 0) ? 1 : 0;
            return remainingMoves;
        } else {
            throw new RuntimeException();
        }
    }
}
