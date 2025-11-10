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

    private SevenPokerGame.State stateFor(String roomId) {
        return rooms.computeIfAbsent(roomId, key -> new SevenPokerGame.State());
    }

    private static Map<String, Object> dto(Card card) {
        return Map.of(
                "rank", card.rankStr(),
                "suit", card.suitStr()
        );
    }

    private List<Map<String, Object>> maskFor(String viewer, SevenPokerGame.State state) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, SevenPokerGame.Side> entry : state.players.entrySet()) {
            String uid = entry.getKey();
            SevenPokerGame.Side side = entry.getValue();
            List<Map<String, Object>> visible = new ArrayList<>();
            for (int i = 0; i < side.cards.size(); i++) {
                boolean hide = !Objects.equals(viewer, uid) && (i < 2 || i == 6);
                visible.add(hide ? Map.of("rank", "BACK", "suit", "") : dto(side.cards.get(i)));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user", uid);
            row.put("cards", visible);
            row.put("bet", side.contributed);
            row.put("action", side.lastAction);
            row.put("actionAmount", side.lastAmount);
            row.put("folded", side.folded);
            row.put("ai", side.ai);
            row.put("toCall", game.toCall(state, uid));
            row.put("streetBet", state.streetContribution.getOrDefault(uid, 0));
            row.put("winner", side.winner);
            if (side.payout != null) {
                row.put("payout", side.payout);
            }
            if (side.showdownScore != null) {
                row.put("handRank", side.showdownScore.rank.name());
                row.put("handScore", side.showdownScore.toString());
            }
            if (side.profileName() != null) {
                row.put("profile", side.profileName());
            }
            out.add(row);
        }
        return out;
    }

    @PostMapping("/start")
    public Object start(@RequestParam String roomId,
                        @RequestParam String users,
                        @RequestParam int ante) {
        List<String> list = Arrays.stream(users.split(","))
                .map(String::trim)
                .filter(u -> !u.isEmpty())
                .collect(Collectors.toList());
        SevenPokerGame.State state = stateFor(roomId);
        game.start(state, list, ante);
        return ApiResponse.of("ok", true).detail(Map.of(
                "inProgress", state.inProgress,
                "stage", state.stageName,
                "round", state.stage,
                "turn", state.turn
        ));
    }

    @GetMapping("/state")
    public Object state(@RequestParam String roomId, @RequestParam String viewer) {
        SevenPokerGame.State state = stateFor(roomId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("inProgress", state.inProgress);
        detail.put("stage", state.stageName);
        detail.put("round", state.stage);
        detail.put("turn", state.turn);
        detail.put("ante", state.ante);
        detail.put("pot", state.pot);
        detail.put("currentBet", state.currentBet);
        detail.put("minRaise", game.minRaise(state));
        detail.put("pending", game.pendingOrder(state));
        detail.put("winners", new ArrayList<>(state.winners));
        detail.put("payouts", new LinkedHashMap<>(state.payouts));
        detail.put("settledPot", state.settledPot);
        detail.put("players", maskFor(viewer, state));
        return ApiResponse.of("ok", true).detail(detail);
    }

    @PostMapping("/bet")
    public Object bet(@RequestParam String roomId,
                      @RequestParam String user,
                      @RequestParam int amount) {
        SevenPokerGame.State state = stateFor(roomId);
        game.bet(state, user, amount);
        return ApiResponse.of("ok", true).detail(Map.of(
                "pot", state.pot,
                "currentBet", state.currentBet,
                "turn", state.turn
        ));
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
