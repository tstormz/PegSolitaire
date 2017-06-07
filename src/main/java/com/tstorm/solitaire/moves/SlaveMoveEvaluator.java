package com.tstorm.solitaire.moves;

import com.tstorm.solitaire.pieces.Board;

import java.util.Optional;

public class SlaveMoveEvaluator extends MoveEvaluator {
    private final Board board;
    
    public SlaveMoveEvaluator(Board board, Optional<Move> move) {
        super(move);
        this.board = board;
    }
    
    @Override
    public Board getBoard() {
        return board;
    }
    
    @Override
    public String getRole() {
        return "Slave";
    }
    
    public Optional<Move> getEvaluationMove() {
        return evaluationMove;
    }
}
