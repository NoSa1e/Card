package com.cardgame.cardserver.api;

import com.cardgame.cardserver.core.Card;
import com.cardgame.cardserver.core.SevenPokerGame;
import com.cardgame.cardserver.util.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seven")
public class SevenPokerController {

    private final Map<String, SevenPokerGame.State> rooms = new HashMap<>();
    private final SevenPokerGame game = new SevenPokerGame();
    private SevenPokerGame.State st(String roomId){ return rooms.computeIfAbsent(roomId, k-> new SevenPokerGame.State()); }
    private static Map<String,Object> dto(Card c){ return Map.of("rank", c.rankStr(), "suit", c.suitStr()); }
    private static List<Map<String,Object>> maskFor(String viewer, Map<String,SevenPokerGame.Side> players){
        List<Map<String,Object>> out=new ArrayList<>();
        for(var e: players.entrySet()){
            String uid=e.getKey(); var side=e.getValue(); var cards=side.cards;
            List<Map<String,Object>> show=new ArrayList<>();
            for(int i=0;i<cards.size();i++){
                if(!viewer.equals(uid) && (i<2 || i==6)) show.add(Map.of("rank","BACK","suit",""));
                else show.add(dto(cards.get(i)));
            }
            Map<String,Object> entry=new LinkedHashMap<>();
            entry.put("user", uid);
            entry.put("cards", show);
            entry.put("bet", side.contributed);
            entry.put("action", side.lastAction);
            entry.put("actionAmount", side.lastAmount);
            entry.put("folded", side.folded);
            entry.put("ai", side.ai);
            if(side.profileName()!=null) entry.put("profile", side.profileName());
            out.add(entry);
        }
        return out;
    }

    @PostMapping("/start")
    public Object start(@RequestParam String roomId, @RequestParam String users, @RequestParam int ante){
        var list = Arrays.asList(users.split(","));
        var s = st(roomId);
        game.start(s, list, ante);
        return ApiResponse.of("ok", true).detail(Map.of(
                "inProgress", s.inProgress,
                "stage", s.stageName,
                "round", s.stage,
                "turn", s.turn
        ));
    }

    @GetMapping("/state")
    public Object state(@RequestParam String roomId, @RequestParam String viewer){
        var s = st(roomId);
        Map<String,Object> d=new LinkedHashMap<>();
        d.put("inProgress", s.inProgress);
        d.put("stage", s.stageName);
        d.put("round", s.stage);
        d.put("turn", s.turn);
        d.put("ante", s.ante);
        d.put("pot", s.pot);
        d.put("players", maskFor(viewer, s.players));
        return ApiResponse.of("ok", true).detail(d);
    }

    @PostMapping("/call")
    public Object call(@RequestParam String roomId, @RequestParam String user) {
        SevenPokerGame.State state = stateFor(roomId);
        game.call(state, user);
        return ApiResponse.of("ok", true).detail(Map.of(
                "pot", state.pot,
                "currentBet", state.currentBet,
                "turn", state.turn
        ));
    }

    @PostMapping("/check")
    public Object check(@RequestParam String roomId, @RequestParam String user) {
        SevenPokerGame.State state = stateFor(roomId);
        game.check(state, user);
        return ApiResponse.of("ok", true).detail(Map.of(
                "turn", state.turn
        ));
    }

    @PostMapping("/fold")
    public Object fold(@RequestParam String roomId, @RequestParam String user) {
        SevenPokerGame.State state = stateFor(roomId);
        game.fold(state, user);
        return ApiResponse.of("ok", true).detail(Map.of(
                "turn", state.turn,
                "inProgress", state.inProgress,
                "winners", new ArrayList<>(state.winners)
        ));
    }

    @PostMapping("/next")
    public Object next(@RequestParam String roomId) {
        SevenPokerGame.State state = stateFor(roomId);
        game.next(state);
        return ApiResponse.of("ok", true).detail(Map.of(
                "stage", state.stage,
                "stageName", state.stageName,
                "turn", state.turn,
                "inProgress", state.inProgress
        ));
    }
}
