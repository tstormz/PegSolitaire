package com.tstorm.solitaire.moves;

import com.tstorm.solitaire.pieces.Board;
import com.tstorm.solitaire.server.Master;
import com.tstorm.solitaire.server.Slave;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MasterMoveEvaluator extends MoveEvaluator {
    private final Board board;
    
    public MasterMoveEvaluator(Board b, Optional<Move> evaluationMove) {
        super(evaluationMove);
        board = b;
    }
    
    @Override
    public Board getBoard() {
        return board;
    }
    
    @Override
    public String getRole() {
        return "Master";
    }
    
    public JSONArray evaluate(List<MoveEvaluator> evaluations) throws JSONException, IOException {
        JSONArray suggestions = new JSONArray();
        for (int i = 0; i < evaluations.size(); i++) {
            if (evaluations.get(i).join()) {
                if (evaluations.get(i).evaluationMove.isPresent()) {
                    Move move = evaluations.get(i).evaluationMove.get();
                    System.out.println("suggestion: " + move.toString());
                    suggestions.put(buildSuggestion(move));
                }
            }
        }
        System.out.println("finished");
        return suggestions;
    }
    
    public JSONObject buildSuggestion(Move m) throws JSONException {
        JSONObject suggestion = new JSONObject();
        suggestion.put("start", m.start);
        suggestion.put("end", m.end);
        return suggestion;
    }
}
