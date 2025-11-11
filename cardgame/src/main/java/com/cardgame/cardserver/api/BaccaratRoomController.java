package com.cardgame.cardserver.api;

import com.cardgame.cardserver.core.*;
import com.cardgame.cardserver.util.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/baccarat/room")
public class BaccaratRoomController {
    private final Map<String,BaccaratRoom> rooms = new HashMap<>();
    private BaccaratRoom getOrCreate(String roomId, int decks){ return rooms.computeIfAbsent(roomId, k-> new BaccaratRoom(decks)); }
    private static Map<String,Object> dto(Card c){ return Map.of("rank", c.rankStr(), "suit", c.suitStr()); }

    @PostMapping("/bet")
    public Object bet(@RequestParam String roomId, @RequestParam String user,
                      @RequestParam String main, @RequestParam int amount,
                      @RequestParam(defaultValue="false") boolean pairP,
                      @RequestParam(defaultValue="false") boolean pairB,
                      @RequestParam(defaultValue="false") boolean super6,
                      @RequestParam(defaultValue="6") int decks){
        Room r = RoomStore.get(roomId);
        if(r==null) return ApiResponse.of("ok", false).detail("no room");
        var g = getOrCreate(roomId, decks);
        if(!g.r.inProgress){
            g.resetRound();
            g.r.inProgress = true;
        }
        int stake = amount;
        if(pairP) stake += amount;
        if(pairB) stake += amount;
        if(super6) stake += amount;
        SessionStore.add(user, -stake);
        g.place(user, main, amount, pairP, pairB, super6);
        if(g.ready(r.players)){
            g.dealAndSettle(true);
            var round = g.r;
            Map<String,Integer> balances = new LinkedHashMap<>();
            if(!round.settlementApplied){
                for(var entry: round.settle.entrySet()){
                    balances.put(entry.getKey(), SessionStore.add(entry.getKey(), entry.getValue()));
                }
                round.settlementApplied = true;
            }
            return ApiResponse.of("ok", true).detail(Map.of(
                    "inProgress", round.inProgress,
                    "player", Map.of(
                            "cards", round.player.cards.stream().map(BaccaratRoomController::dto).toList(),
                            "total", round.player.total
                    ),
                    "banker", Map.of(
                            "cards", round.banker.cards.stream().map(BaccaratRoomController::dto).toList(),
                            "total", round.banker.total
                    ),
                    "ledger", new LinkedHashMap<>(round.ledger),
                    "settle", new LinkedHashMap<>(round.settle),
                    "balances", balances
            ));
        }
        return ApiResponse.of("ok", true).detail(Map.of(
                "ledger", g.r.ledger,
                "inProgress", true,
                "balance", SessionStore.get(user)
        ));
    }

    @PostMapping("/deal")
    public Object deal(@RequestParam String roomId, @RequestParam(defaultValue="true") boolean commission){
        var g = rooms.get(roomId);
        if(g==null) return ApiResponse.of("ok", false).detail("no game");
        g.dealAndSettle(commission);
        var r = g.r;
        Map<String,Integer> balances = new LinkedHashMap<>();
        if(!r.settlementApplied){
            for(var entry: r.settle.entrySet()){
                balances.put(entry.getKey(), SessionStore.add(entry.getKey(), entry.getValue()));
            }
            r.settlementApplied = true;
        }
        return ApiResponse.of("ok", true).detail(Map.of(
                "inProgress", r.inProgress,
                "player", Map.of("cards", r.player.cards.stream().map(BaccaratRoomController::dto).toList(), "total", r.player.total),
                "banker", Map.of("cards", r.banker.cards.stream().map(BaccaratRoomController::dto).toList(), "total", r.banker.total),
                "ledger", r.ledger,
                "settle", r.settle,
                "balances", balances
        ));
    }

    @GetMapping("/state")
    public Object state(@RequestParam String roomId){
        var g = rooms.get(roomId);
        if(g==null) return ApiResponse.of("ok", false).detail("no game");
        var r = g.r;
        return ApiResponse.of("ok", true).detail(Map.of(
                "inProgress", r.inProgress,
                "ledger", r.ledger,
                "player", r.player.cards.isEmpty()? null : Map.of("cards", r.player.cards.stream().map(BaccaratRoomController::dto).toList(), "total", r.player.total),
                "banker", r.banker.cards.isEmpty()? null : Map.of("cards", r.banker.cards.stream().map(BaccaratRoomController::dto).toList(), "total", r.banker.total),
                "settle", r.settle,
                "settled", r.settlementApplied
        ));
    }
}
