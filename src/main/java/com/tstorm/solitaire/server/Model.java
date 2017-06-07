package com.tstorm.solitaire.server;

import com.tstorm.solitaire.pieces.Board;
import com.tstorm.solitaire.pieces.Peg;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Model {
    private final Board board;
    private Optional<Peg> selection = Optional.empty();
    
    public Model() {
        this.board = new Board();
    }

    public Board getBoard() {
        return board;
    }

    public boolean hasSelection() {
        return selection.isPresent();
    }

    public Optional<Peg> getSelection() {
        return selection;
    }

    public void setSelection(Optional<Peg> peg) {
        selection = peg;
    }

    public boolean isValidJump(Peg jumpedPeg, Peg landingSpot) {
        if (landingSpot.isEnabled() && landingSpot.isDiscarded()) {
            return !jumpedPeg.isDiscarded();
        } else {
            return false;
        }
    }

    public Optional<Peg> findPegBetween(Peg firstSelectedPeg, Peg secondSelectedPeg) {
        List<Optional<Peg>> matches = firstSelectedPeg.getNeighbors().stream()
                .filter(peg -> peg.isPresent())
                .filter(peg -> hasNeighborSelected(() -> peg.get(), secondSelectedPeg))
                .collect(Collectors.toList());
        if (matches.size() == 0) {
            return Optional.empty();
        } else if (matches.size() == 1) {
            return matches.get(0);
        } else {
            System.err.println("Error: found more than one match on a unique ID");
            return Optional.empty();
        }
    }

    private boolean hasNeighborSelected(Supplier<Peg> firstPegNeighbor, Peg secondSelectedPeg) {
        for (Optional<Peg> p : firstPegNeighbor.get().getNeighbors()) {
            if (p.isPresent() && p.get().getId() == secondSelectedPeg.getId()) {
                return true;
            }
        }
        return false;
    }

    public Peg getPegById(int id) throws ArrayIndexOutOfBoundsException {
        int row = findRow(id);
        int col = id - (row * Board.ROWS);
        if (col >= Board.COLUMNS)
            throw new ArrayIndexOutOfBoundsException("Column invalid");
        return board.get(row, col);
    }

    // TODO add out of bound exception
    private int findRow(int id) throws ArrayIndexOutOfBoundsException {
        int x = 1;
        while (id >= x * Board.ROWS) {
            x += 1;
            if (x > Board.ROWS)
                throw new ArrayIndexOutOfBoundsException("Row invalid");
        }
        return x - 1;
    }
    
    public void jump(Peg firstSelection, Peg jumpedPeg, Peg secondSelection) {
        board.jump(firstSelection.getId(), jumpedPeg.getId(), secondSelection.getId());
    }
}