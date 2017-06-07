package com.tstorm.solitaire;

import com.tstorm.solitaire.pieces.Board;
import com.tstorm.solitaire.pieces.Peg;
import com.tstorm.solitaire.server.Controller;
import j2html.tags.Tag;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static j2html.TagCreator.*;
import static spark.Spark.*;

public class App {
    public static final boolean EUROPEAN = false;
    public static final int PORT = 4296;
    public static final String SLAVE_CONFIGURATION_FILE = "conf/slaves.conf";
    
    public static void main(String[] args) {
        Controller controller = new Controller();
        staticFileLocation("/public");
        webSocket("/solitaire", controller);
        init();
        try (BufferedReader reader = new BufferedReader(new FileReader(SLAVE_CONFIGURATION_FILE))) {
            String line;
            while((line = reader.readLine()) != null) {
                String[] slaveInfo = line.split(": ");
                String name = slaveInfo[0];
                String address = slaveInfo[1];
                controller.addSlave(name, InetAddress.getByName(address));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String createBoard(Board b) {
        try {
            JSONObject board = new JSONObject();
            board.put("table", table().with(
                    createRows(b.array())
            ));
            return String.valueOf(board);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static List<Tag> createRows(Peg[][] board) {
        return Arrays.stream(board).map(row ->
            tr().with(
                createColumn(row)
            )
        ).collect(Collectors.toList());
    }

    // TODO move "select" to a config file
    private static List<Tag> createColumn(Peg[] row) {
        return Arrays.stream(row).map(column ->
            td().withId("peg" + column.getId())
                .withClass(column.isEnabled() && !column.isDiscarded() ? "on" : "off")
                .attr("onclick", "select(this)")
                .with(text(String.format("%d", column.getId())))
        ).collect(Collectors.toList());
    }
}