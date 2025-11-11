package com.cardgame.cardserver.api;
import com.cardgame.cardserver.core.*;
import com.cardgame.cardserver.util.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/blackjack")
public class BlackjackController {
    private final Map<String,BlackjackGame> solos = new HashMap<>();
    private BlackjackGame solo(String u, int decks){ return solos.computeIfAbsent(u, k->new BlackjackGame(decks)); }
    private static Map<String,Object> hand(BlackjackGame.Hand h){
        return Map.of("cards", h.cards.stream().map(BlackjackController::dto).toList(), "total", h.total, "done", h.done);
    }
    private static Map<String,Object> dto(Card c){ return Map.of("rank", c.rankStr(), "suit", c.suitStr()); }
    private Object wrap(String user, BlackjackGame.State s, int settle){
        int bal = com.cardgame.cardserver.core.SessionStore.add(user, settle);
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("inProgress", s.inProgress); d.put("bet", s.bet); d.put("delta", s.delta); d.put("balance", bal);
        d.put("playerHands", s.playerHands.stream().map(BlackjackController::hand).toList());
        d.put("activeIndex", s.activeIndex); d.put("dealer", hand(s.dealer));
        return ApiResponse.of("ok", true).detail(d);
    }
    @PostMapping("/solo/start") public Object start(@RequestParam String user, @RequestParam int bet, @RequestParam(defaultValue="4") int decks){
        SessionStore.add(user, -bet);
        var st = solo(user,decks).start(bet); return wrap(user, st, 0);
    }
    @PostMapping("/solo/hit") public Object hit(@RequestParam String user){
        var g=solos.get(user); var st=g.hit(); return wrap(user, st, st.inProgress?0:st.delta);
    }
    @PostMapping("/solo/stand") public Object stand(@RequestParam String user){
        var g=solos.get(user); var st=g.stand(); return wrap(user, st, st.inProgress?0:st.delta);
    }
    @PostMapping("/solo/double") public Object dbl(@RequestParam String user){
        var g=solos.get(user);
        int stake = g!=null ? g.currentBet() : 0;
        var st=g.dbl();
        if(stake>0){
            SessionStore.add(user, -stake);
        }
        return wrap(user, st, st.inProgress?0:st.delta);
    }
    @PostMapping("/solo/surrender") public Object sur(@RequestParam String user){
        var g=solos.get(user); var st=g.surrender(); return wrap(user, st, st.inProgress?0:st.delta);
    }
    @PostMapping("/solo/split") public Object split(@RequestParam String user){
        var g=solos.get(user);
        int beforeHands = g!=null ? g.handCount() : 0;
        int stake = g!=null ? g.currentBet() : 0;
        var st=g.split();
        if(st.playerHands.size()>beforeHands && stake>0){
            SessionStore.add(user, -stake);
        }
        return wrap(user, st, 0);
    }
}
