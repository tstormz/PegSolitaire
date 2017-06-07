package com.tstorm.solitaire.server;

import com.tstorm.solitaire.App;
import com.tstorm.solitaire.moves.MasterMoveEvaluator;
import com.tstorm.solitaire.pieces.Peg;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebSocket
public class Controller {
    private interface Strategy {
        String process(String message) throws JSONException;
    }

    private class FirstMoveStrategy implements Strategy {
        @Override
        public String process(String message) throws JSONException {
            final int id = Integer.parseInt(message.substring("peg".length()));
            Peg peg = model.getPegById(id);
            peg.discard();
            model.getBoard().emptyHoles().put(id, peg);
            return message;
        }
    }

    private class GamePlayStrategy implements Strategy {
        @Override
        public String process(String message) throws JSONException {
            // extract id as an int ("pegx" - "peg" = x)
            if (message.equals("suggestion")) {
                return master.evaluate(model.getBoard());
            } else {
                final int id = Integer.parseInt(message.substring("peg".length()));
                if (model.hasSelection()) {
                    return tryJump(model.getSelection().get(), model.getPegById(id));
                } else {
                    return trySelect(id, message);
                }
            }
        }
    }
    
    private final Object lock = new Object();
    private volatile boolean gameIsRunning = false;
    
    private final Master master;
    private Model model = new Model();
    private Strategy strategy = new FirstMoveStrategy();
    
    public Controller() {
        master = new Master().startListening();
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws IOException {
        gameIsRunning = true;
        user.getRemote().sendString(App.createBoard(model.getBoard()));
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) throws IOException, JSONException {
        String result = strategy.process(message);
        if (strategy instanceof FirstMoveStrategy) {
            strategy = new GamePlayStrategy();
        }
        user.getRemote().sendString(result);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        model = new Model();
        strategy = new FirstMoveStrategy();
        System.out.println(String.format("Connection with %s closed with code %d (%s)",
                user.getRemoteAddress().toString(), statusCode, reason));
        gameIsRunning = false;
        master.shutdown();
    }
    
    public void addSlave(String name, InetAddress node) {
        new Thread(() -> {
            final int MAX_TRIES = 60;
            int tries = 0;
            while (tries++ < MAX_TRIES) {
                try {
                    Slave s = new Slave(node);
                    if (!gameIsRunning) {
                        synchronized (lock) {
                            master.addSlave(s);
                            return; // success
                        }
                    }
                } catch (ConnectException e) {
                    try {
                        Thread.sleep(1000 * 5);
                    } catch (InterruptedException interrupted) {
                        interrupted.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.err.println(String.format("Unable to connect with %s @ %s", name, node.getHostAddress()));
        }).start();
    }
    
    private String trySelect(int id, String message) {
        try {
            model.setSelection(Optional.of(model.getPegById(id)));
            if (model.hasSelection() && isValidSelection(model.getSelection().get())) {
                return message;
            } else {
                model.setSelection(Optional.empty());
                return "error";
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return "error";
        }
    }

    private boolean isValidSelection(Peg p) {
        return p.isEnabled() && !p.isDiscarded();
    }

    private String tryJump(Peg firstSelection, Peg secondSelection) throws JSONException {
        if (!secondSelection.isDiscarded()) {
            model.setSelection(Optional.empty());
            return jumpError(firstSelection);
        }
        Optional<Peg> jumped = model.findPegBetween(firstSelection, secondSelection);
        if (jumped.isPresent() && model.isValidJump(jumped.get(), secondSelection)) {
            JSONObject jumpData = new JSONObject();
            jumpData.put("source", "peg" + firstSelection.getId());
            jumpData.put("jumped", "peg" + jumped.get().getId());
            jumpData.put("destination", "peg" + secondSelection.getId());
            model.jump(firstSelection, jumped.get(), secondSelection);
            model.setSelection(Optional.empty());
            return String.valueOf(jumpData);
        } else {
            model.setSelection(Optional.empty());
            return jumpError(firstSelection);
        }
    }

    private String jumpError(Peg pegToUnselect) throws JSONException {
        JSONObject errorData = new JSONObject();
        errorData.put("error", "peg" + pegToUnselect.getId());
        return String.valueOf(errorData);
    }
}