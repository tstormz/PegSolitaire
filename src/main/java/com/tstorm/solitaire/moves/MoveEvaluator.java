package com.tstorm.solitaire.moves;

import com.tstorm.solitaire.App;
import com.tstorm.solitaire.pieces.Board;
import com.tstorm.solitaire.pieces.Peg;
import com.tstorm.solitaire.server.Slave;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class MoveEvaluator extends RecursiveTask<Boolean> {
    private enum Translation {
        NINETY_DEGREES,
        ONE_EIGHTY_DEGREES,
        TWO_SEVENTY_DEGREES,
        HORIZONTAL_REFLECTION,
        VERTICAL_REFLECTION
    }
    
    @FunctionalInterface
    private interface Translator {
        int translate(Peg p, int row, int col);
    }
    
    public abstract Board getBoard();
    public abstract String getRole();
    
    private static final int TOTAL_PEGS = App.EUROPEAN ? 37 : 33;
    private static final int MINIMUM_REMAINING_MOVES = 3;
    
    private static final ReadWriteLock rotateBoard = new ReentrantReadWriteLock();
    private static final List<Board> existingBoards = new ArrayList<>();
    
    protected Optional<Move> evaluationMove;
    
    public MoveEvaluator(Optional<Move> evaluationMove) {
        this.evaluationMove = evaluationMove;
    }
    
    @Override
    public Boolean compute() {
        List<Move> remainingMoves = getBoard().getMoves();
        if (belowThreshold(remainingMoves.size())) {
            LinkedList<Board> boards = new LinkedList<>();
            boards.add(getBoard());
            return canWin(boards);
        } else {
            List<MoveEvaluator> subMoves = new ArrayList<>();
            for (Move m : remainingMoves) {
                Board copy = new Board(getBoard());
                copy.jump(m.start, m.jumped, m.end);
                if (!existingBoard(copy)) {
                    MoveEvaluator subMoveEvaluator;
                    if (getRole().equals("Master")) {
                        subMoveEvaluator = new MasterMoveEvaluator(copy, Optional.empty());
                    } else if (getRole().equals("Slave")) {
                        subMoveEvaluator = new SlaveMoveEvaluator(copy, Optional.empty());
                    } else {
                        throw new RuntimeException("Unsupported MoveEvaluator: " + getRole());
                    }
                    subMoves.add(subMoveEvaluator);
                    subMoveEvaluator.fork();
                    addToExistingBoards(copy);
                }
            }
            for (MoveEvaluator child : subMoves) {
                if (child.join()) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private boolean belowThreshold(int remainingMoves) {
        int remainingPegs = getBoard().remainingPegs();
        return remainingMoves <= MINIMUM_REMAINING_MOVES && (remainingPegs >> 1) <= TOTAL_PEGS;
    }
    
    private boolean canWin(LinkedList<Board> boards) {
        while (!boards.isEmpty()) {
            Board board = boards.remove();
            List<Move> remainingMoves = board.getMoves();
            if (remainingMoves.size() != 0) {
                for (Move m : remainingMoves) {
                    Board b = new Board(board);
                    b.jump(m.start, m.jumped, m.end);
                    boards.push(b);
                }
            } else {
                if (TOTAL_PEGS - board.emptyHoles().keySet().size() == 1) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean existingBoard(Board board) {
        boolean exists = false;
        rotateBoard.readLock().lock();
        try {
            for (Board b : existingBoards) {
                if (b.compareTo(board) == Board.TRUE) {
                    exists = true;
                    // because we are holding a read lock we should get out as soon as possible
                    break; // for any waiting writers
                }
            }
        } finally {
            rotateBoard.readLock().unlock();
        }
        return exists;
    }
    
    private void addToExistingBoards(Board board) {
        Board[] boards = new Board[Translation.values().length + 1];
        boards[0] = board;
        int i = 1;
        for (Translation t : Translation.values()) {
            boards[i++] = rotate(boards[0], t);
        }
        rotateBoard.writeLock().lock();
        try {
            for (Board b : boards) {
                existingBoards.add(b);
            }
        } finally {
            rotateBoard.writeLock().unlock();
        }
    }
    
    private Board rotate(Board b, Translation translation) {
        switch (translation) {
            case NINETY_DEGREES:
                return rotation(b, (peg, row, col) -> peg.getId() + (42 - (row * 6)) - (col * 8));
            case ONE_EIGHTY_DEGREES:
                return rotation(b, (peg, row, col) -> peg.getId() + (48 - (peg.getId() * 2)));
            case TWO_SEVENTY_DEGREES:
                return rotation(b, (peg, row, col) -> peg.getId() + (6 - (row * 8)) + (col * 6));
            case HORIZONTAL_REFLECTION:
                return rotation(b, (peg, row, col) -> (42 - (row * 7)) + col);
            case VERTICAL_REFLECTION:
                return rotation(b, (peg, row, col) -> (6 + (row * 7)) - col);
            default:
                return b; // if an invalid translation is provided just return the origin board
        }
    }
    
    private Board rotation(Board b, Translator t) {
        Board translatedBoard = new Board();
        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLUMNS; j++) {
                Peg p = b.get(i, j);
                if (p.isDiscarded()) {
                    translatedBoard.getPegById(t.translate(p, i, j)).discard();
                }
            }
        }
        return translatedBoard;
    }
}
