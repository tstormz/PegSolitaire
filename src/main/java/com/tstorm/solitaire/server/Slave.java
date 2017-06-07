package com.tstorm.solitaire.server;

import com.tstorm.solitaire.App;
import com.tstorm.solitaire.moves.Move;
import com.tstorm.solitaire.moves.SlaveMoveEvaluator;
import com.tstorm.solitaire.pieces.Board;
import com.tstorm.solitaire.pieces.Peg;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Slave {
    private Socket socket;
    private DataOutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    
    public Slave(InetAddress address) throws IOException {
        this.socket = new Socket(address, App.PORT);
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(App.PORT);
        System.out.printf("waiting...");
        Socket master = serverSocket.accept();
        InetAddress masterAddress = master.getInetAddress();
        System.out.println("connected!");
        InputStream stream = master.getInputStream();
        DataInputStream inputStream = new DataInputStream(stream);
        ObjectInputStream objectInputStream = new ObjectInputStream(stream);
        List<SlaveMoveEvaluator> pendingEvaluations = new ArrayList<>();
        while (!inputStream.readBoolean()) {
            boolean hasAllBoards = false;
            while (!hasAllBoards) {
                Move m = receiveMove(inputStream);
                System.out.println(m.toString());
                Board b = createBoard(objectInputStream);
                System.out.println("received board");
                b.jump(m.start(), m.jumped(), m.end());
                b.print();
                SlaveMoveEvaluator evaluation = new SlaveMoveEvaluator(b, Optional.of(m));
                pendingEvaluations.add(evaluation);
                evaluation.fork();
                if (!inputStream.readBoolean()) {
                    hasAllBoards = true;
                }
            }
            for (int i = 0; i < pendingEvaluations.size(); i++) {
                boolean result = pendingEvaluations.get(i).join();
                try (Socket response = new Socket(masterAddress, App.PORT)) {
                    DataOutputStream out = new DataOutputStream(response.getOutputStream());
                    Optional<Move> m = pendingEvaluations.get(i).getEvaluationMove();
                    if (m.isPresent()) {
                        System.out.println("sending move " + m.toString());
                        out.writeInt(m.get().start());
                        out.writeInt(m.get().jumped());
                        out.writeInt(m.get().end());
                        out.writeBoolean(result);
                    } else {
                        out.write(-1);
                    }
                }
            }
        }
        objectInputStream.close();
        inputStream.close();
        master.close();
    }
    
    public void shutdown(boolean shutdown) {
        try {
            outputStream.writeBoolean(shutdown);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendMove(Move move) {
        System.out.println("sending move " + move.toString());
        try {
            outputStream.writeInt(move.start());
            outputStream.writeInt(move.jumped());
            outputStream.writeInt(move.end());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Move receiveMove(DataInputStream inputStream) {
        int start, jumped, end;
        try {
            System.out.print("receiving move: ");
            start = inputStream.readInt();
            jumped = inputStream.readInt();
            end = inputStream.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            start = jumped = end = 0;
        }
        return new Move(start, jumped, end);
    }
    
    private static Board createBoard(ObjectInputStream inputStream) throws IOException {
        Peg[][] pegs = new Peg[Board.ROWS][Board.COLUMNS];
        int i = 0, j = 0;
        while (i < Board.ROWS) {
            try {
                pegs[i][j++] = (Peg) inputStream.readObject();
                if (j == Board.COLUMNS) {
                    j = 0;
                    i += 1;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return new Board(pegs);
    }
    
    public void sendBoard(Board board, boolean isMoreWork) {
        try {
            for (Peg[] row : board.array()) {
                for (Peg p : row) {
                    objectOutputStream.writeObject(p);
                }
            }
            outputStream.writeBoolean(isMoreWork);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}