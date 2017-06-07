package com.tstorm.solitaire.moves;

import com.tstorm.solitaire.pieces.Peg;

public class Move {
    protected final int start;
    protected final int jumped;
    protected final int end;
    
    public Move(Peg start, Peg jumped, Peg end) {
        this(start.getId(), jumped.getId(), end.getId());
    }
    
    public Move (int start, int jumped, int end) {
        this.start = start;
        this.jumped = jumped;
        this.end = end;
    }
    
    public int start() {
        return start;
    }
    
    public int jumped() {
        return jumped;
    }
    
    public int end() {
        return end;
    }
    
    @Override
    public String toString() {
        return String.format("peg%d -> peg%d (discard peg%d)", start, end, jumped);
    }
}
