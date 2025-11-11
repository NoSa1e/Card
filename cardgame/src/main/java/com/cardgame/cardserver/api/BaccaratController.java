package com.cardgame.cardserver.api;
import com.cardgame.cardserver.core.*; import com.cardgame.cardserver.util.ApiResponse;
import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/baccarat")
public class BaccaratController {
    private final Map<String,BaccaratGame> solos = new HashMap<>();
    private BaccaratGame solo(String u, int decks){ return solos.computeIfAbsent(u, k->new BaccaratGame(decks)); }
    private static Map<String,Object> side(String name, java.util.List<Card> cs, int total){
        return Map.of("name", name, "cards", cs.stream().map(BaccaratController::dto).toList(), "total", total);
    }
    private static Map<String,Object> dto(Card c){ return Map.of("rank", c.rankStr(), "suit", c.suitStr()); }
    @PostMapping("/solo/bet")
    public Object bet(@RequestParam String user, @RequestParam int amount,
                      @RequestParam String main,
                      @RequestParam(defaultValue="false") boolean pairPlayer,
                      @RequestParam(defaultValue="false") boolean pairBanker,
                      @RequestParam(defaultValue="false") boolean super6,
                      @RequestParam(defaultValue="true") boolean commission,
                      @RequestParam(defaultValue="6") int decks){
        int stake = amount;
        if(pairPlayer) stake += amount;
        if(pairBanker) stake += amount;
        if(super6) stake += amount;
        SessionStore.add(user, -stake);
        var st = solo(user,decks).bet(main, pairPlayer, pairBanker, super6, amount, commission);
        int newBal = SessionStore.add(user, st.delta);
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("player", side("PLAYER", st.player.cards, st.player.total));
        d.put("banker", side("BANKER", st.banker.cards, st.banker.total));
        d.put("delta", st.delta); d.put("balance", newBal); d.put("inProgress", false);
        return ApiResponse.of("ok", true).detail(d);
    }
}
