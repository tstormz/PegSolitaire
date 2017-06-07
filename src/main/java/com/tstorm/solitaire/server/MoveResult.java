package com.tstorm.solitaire.server;

import com.tstorm.solitaire.moves.MasterMoveEvaluator;
import com.tstorm.solitaire.moves.Move;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class MoveResult extends Thread {
    private static int debug_id = 0;
    private final int id;
    private final Socket socket;
    private final MasterMoveEvaluator masterRef;
    private boolean result, waitingForResult; // possible race condition
    private Move move;
    
    public MoveResult(Socket s, MasterMoveEvaluator moveEvaluator) {
        socket = s;
        masterRef = moveEvaluator;
        result = false;
        waitingForResult = true;
        id = debug_id++;
    }
    
    public void run() {
        System.out.println("Move result #" + debug_id + " waiting for result...");
        while (waitingForResult) {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                move = new Move(in.readInt(), in.readInt(), in.readInt());
                result = in.readBoolean();
                waitingForResult = false;
                System.out.println(String.format("Move result #" + id + " received %s move %s",
                        move.toString(), result ? "successful" : "unsuccessful"));
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (masterRef) {
            masterRef.notify();
        }
    }
    
    public boolean isWaitingForResult() {
        return waitingForResult;
    }
    
    public boolean result() {
        return result;
    }
    
    public Move move() {
        return move;
    }
}

