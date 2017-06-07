package com.tstorm.solitaire.pieces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Peg implements Serializable {
    private static final long serialVersionUID = 20170601L;
    
    private final int id;
    private final transient List<Optional<Peg>> neighbors;
    private boolean enabled, discarded;

    public Peg(int id) {
        this.id = id;
        this.neighbors = new ArrayList<>(4);
        this.enabled = true;
        this.discarded = false;
    }
    
    public int getId() {
        return id;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void discard() {
        this.discarded = true;
    }

    public boolean isDiscarded() {
        return this.discarded;
    }

    @Override
    public String toString() {
        return "peg" + id;
    }

    public void setNeighbors(Peg... pegs) {
        if (pegs.length == 4) {
            neighbors.add(0, Optional.ofNullable(pegs[0]));
            neighbors.add(1, Optional.ofNullable(pegs[1]));
            neighbors.add(2, Optional.ofNullable(pegs[2]));
            neighbors.add(3, Optional.ofNullable(pegs[3]));
        }
    }

    public List<Optional<Peg>> getNeighbors() {
        return neighbors;
    }

    public void putBack() {
        discarded = false;
    }
}
