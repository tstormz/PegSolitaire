package com.tstorm.solitaire.pieces;

import com.tstorm.solitaire.App;
import com.tstorm.solitaire.moves.Move;

import java.util.*;

public class Board implements Comparable<Board> {
    private enum Direction {
        LEFT (0),
        UP (1),
        RIGHT (2),
        DOWN (3);
        
        private final int index;
        
        Direction(int index) {
            this.index = index;
        }
        
        public int index() {
            return index;
        }
    }
    
    public static final int ROWS = 7;
    public static final int COLUMNS = 7;

    private final Peg[][] board = new Peg[ROWS][COLUMNS];
    private final Map<Integer, Peg> emptyHoles = new HashMap<>(ROWS * COLUMNS);
    
    public Board(Board b) {
        this();
        for (int id : b.emptyHoles().keySet()) {
            Peg peg = getPegById(id);
            peg.discard();
            this.emptyHoles.put(peg.getId(), peg);
        }
    }
    
    public Board(Peg[][] pegs) {
        this();
        for (Peg[] row : pegs) {
            for (Peg peg : row) {
                if (peg.isDiscarded()) {
                    Peg p = getPegById(peg.getId());
                    p.discard();
                    emptyHoles.put(p.getId(), p);
                }
            }
        }
    }
    
    public Board(boolean cheat) {
        this();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                board[i][j].discard();
                emptyHoles.put(board[i][j].getId(), board[i][j]);
            }
        }
        int[] pegs = {9, 16, 18, 22, 24, 25, 30, 38};
        for (int id : pegs) {
            Peg p = getPegById(id);
            p.putBack();
            emptyHoles.remove(id, p);
        }
    }
    
    public Board() {
        initializePegs();
        assignNeighbors();
        cutOutCorners();
    }

    private void initializePegs() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                board[i][j] = new Peg((i * 7) + j);
            }
        }
    }

    private void assignNeighbors() {
        for (int row = 0; row < ROWS ; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int left = col - 1;
                Peg leftPeg = left >= 0 ? board[row][left] : null;
                int top = row - 1;
                Peg topPeg = top >= 0 ? board[top][col] : null;
                int right = col + 1;
                Peg rightPeg = right < COLUMNS ? board[row][right] : null;
                int bottom = row + 1;
                Peg bottomPeg = bottom < ROWS ? board[bottom][col] : null;
                board[row][col].setNeighbors(leftPeg, topPeg, rightPeg, bottomPeg);
            }
        }
    }

    private void cutOutCorners() {
        int[] rowCorners = {0, 1, ROWS - 2, ROWS - 1};
        int[] colCorners = {0, 1, COLUMNS - 2, COLUMNS - 1};
        for (int i : rowCorners) {
            for (int j : colCorners) {
                board[i][j].setEnabled(false);
            }
        }
        if (App.EUROPEAN) {
            board[1][1].setEnabled(true);
            board[1][COLUMNS-2].setEnabled(true);
            board[ROWS-2][1].setEnabled(true);
            board[ROWS-2][COLUMNS-2].setEnabled(true);
        }
    }

    public Peg get(int row, int col) {
        return board[row][col];
    }
    
    public Peg getPegById(int id) {
        return board[id / 7][id % 7];
    }

    public Peg[][] array() {
        return board;
    }
    
    public boolean pegIsEmpty(int id) {
        return emptyHoles.containsKey(id);
    }
    
    public Map<Integer, Peg> emptyHoles() {
        return emptyHoles;
    }
    
    public int remainingPegs() {
        final int TOTAL_PEGS = App.EUROPEAN ? 37 : 33;
        return TOTAL_PEGS - emptyHoles.keySet().size();
    }
    
    public static final int FALSE = 0, TRUE = 1;
    
    @Override
    public int compareTo(Board b) {
        if (emptyHoles.size() != b.emptyHoles().size()) {
            return FALSE;
        }
        for (int id : emptyHoles.keySet()) {
            if (!b.pegIsEmpty(id)) {
                return FALSE;
            }
        }
        return TRUE;
    }
    
    public void jump(int firstSelection, int jumpedPeg, int secondSelection) {
        // move the selected peg to the position of the second selection
        // and remove the peg in the middle
        Peg first = getPegById(firstSelection);
        first.discard();
        emptyHoles.put(firstSelection, first);
        Peg jumped = getPegById(jumpedPeg);
        jumped.discard();
        emptyHoles.put(jumpedPeg, jumped);
        Peg second = getPegById(secondSelection);
        second.putBack();
        emptyHoles.remove(secondSelection);
    }
    
    public List<Move> getMoves() {
        List<Move> moves = new ArrayList<>();
        Map<Integer, Peg> empties = emptyHoles;
        for (int pegId : empties.keySet()) {
            Peg p = empties.get(pegId);
            for (Direction d : Direction.values()) {
                Move m;
                if ((m = canMove(p, d)) != null) {
                    moves.add(m);
                }
            }
        }
        return moves;
    }
    
    private Move canMove(Peg peg, Direction direction) {
        Optional<Peg> firstNeighbor = peg.getNeighbors().get(direction.index());
        if (isValidPeg(firstNeighbor)) {
            Optional<Peg> secondNeighbor = firstNeighbor.get().getNeighbors().get(direction.index());
            if (isValidPeg(secondNeighbor)) {
                return new Move(secondNeighbor.get(), firstNeighbor.get(), peg);
            }
        }
        return null;
    }
    
    private boolean isValidPeg(Optional<Peg> peg) {
        if (peg.isPresent()) {
            Peg p = peg.get();
            return !p.isDiscarded() && p.isEnabled();
        } else {
            return false;
        }
    }
    
    public void print() {
        for (Peg[] row : board) {
            for (int i = 0; i < 2; i++) {
                for (Peg p : row) {
                    if (p.isEnabled()) {
                        System.out.print(String.format("%s", p.isDiscarded() ? "   " : "XXX"));
                    } else {
                        System.out.print("---");
                    }
                    System.out.print(" ");
                }
                if (i == 0)
                    System.out.println();
            }
            System.out.println();
        }
    }
}
