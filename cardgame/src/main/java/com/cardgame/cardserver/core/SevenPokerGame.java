package com.cardgame.cardserver.core;

import java.util.*;

public class SevenPokerGame {

    private static final String[] STAGES = {
            "3rd Street", "4th Street", "5th Street", "6th Street", "7th Street", "Showdown"
    };

    private final Random rng = new Random();

    public static final class Side {
        public final List<Card> cards = new ArrayList<>();
        public String lastAction = "READY";
        public int lastAmount = 0;
        public int contributed = 0;
        public boolean ai = false;
        public boolean folded = false;
        public DealerProfile profile;

        public String profileName() {
            return profile != null ? profile.name : null;
        }
    }

    public static final class State {
        public boolean inProgress;
        public int stage;
        public String stageName = "READY";
        public int pot;
        public int ante;
        public final Map<String, Integer> bets = new LinkedHashMap<>();
        public final Map<String, Side> players = new LinkedHashMap<>();
        public String lastActor;
        public ActionType lastActionType = ActionType.NONE;
        public int lastActionAmount;
        public Deck deck;
        public final List<String> order = new ArrayList<>();
        public int turnIndex = -1;
        public String turn;
    }

    private enum ActionType { NONE, BET, CHECK, FOLD }

    private static final class AiDecision {
        final ActionType type;
        final int amount;

        AiDecision(ActionType type, int amount) {
            this.type = type;
            this.amount = amount;
        }
    }

    private static final class DealerProfile {
        final String name;
        final double bluffBase;
        final double betAgg;
        final double raiseAgg;
        final double callTight;
        final double semiBluff;
        final double[] streetMult;

        DealerProfile(String name, double bluffBase, double betAgg, double raiseAgg,
                      double callTight, double semiBluff, double[] streetMult) {
            this.name = name;
            this.bluffBase = bluffBase;
            this.betAgg = betAgg;
            this.raiseAgg = raiseAgg;
            this.callTight = callTight;
            this.semiBluff = semiBluff;
            this.streetMult = streetMult;
        }

        static DealerProfile random(Random r) {
            double p = r.nextDouble();
            if (p < 0.20) {
                return new DealerProfile("Nit", 0.02, 0.25, 0.15, 0.70, 0.05,
                        new double[]{1.0, 0.9, 0.8, 0.7, 0.6});
            } else if (p < 0.50) {
                return new DealerProfile("TAG", 0.05, 0.45, 0.35, 0.45, 0.15,
                        new double[]{1.0, 1.0, 0.9, 0.9, 0.8});
            } else if (p < 0.80) {
                return new DealerProfile("LAG", 0.08, 0.65, 0.55, 0.35, 0.25,
                        new double[]{1.1, 1.1, 1.0, 0.95, 0.9});
            } else if (p < 0.95) {
                return new DealerProfile("Maniac", 0.12, 0.85, 0.75, 0.20, 0.30,
                        new double[]{1.2, 1.2, 1.1, 1.0, 0.95});
            }
            return new DealerProfile("Balanced", 0.06, 0.50, 0.40, 0.40, 0.20,
                    new double[]{1.0, 1.0, 1.0, 1.0, 1.0});
        }
    }

    public State start(State s, Collection<String> users, int ante) {
        s.inProgress = true;
        s.stage = 0;
        s.stageName = STAGES[0];
        s.pot = 0;
        s.ante = Math.max(ante, 10);
        s.bets.clear();
        s.players.clear();
        s.order.clear();
        s.lastActor = null;
        s.lastActionType = ActionType.NONE;
        s.lastActionAmount = 0;
        s.deck = new Deck(1);

        for (String u : users) {
            Side side = new Side();
            side.ai = isAiUser(u);
            if (side.ai) {
                side.profile = DealerProfile.random(rng);
            }
            s.players.put(u, side);
            s.bets.put(u, 0);
            s.order.add(u);
            side.lastAction = "WAITING";
            side.lastAmount = 0;
            side.contributed = 0;
            side.folded = false;
        }

        dealInitial(s);
        collectAntes(s);
        resetTurn(s);
        autoAct(s);
        return s;
    }

    public State next(State s) {
        if (!s.inProgress) {
            return s;
        }
        if (s.stage < 4) {
            dealNextCard(s);
            s.stage++;
            s.stageName = STAGES[Math.min(s.stage, STAGES.length - 1)];
            prepareNextStreet(s);
            resetTurn(s);
            autoAct(s);
        } else {
            s.stage = Math.min(s.stage + 1, STAGES.length - 1);
            s.stageName = STAGES[STAGES.length - 1];
            s.inProgress = false;
            s.turn = null;
            s.turnIndex = -1;
        }
        return s;
    }

    public State bet(State s, String user, int amount) {
        applyAction(s, user, ActionType.BET, amount, true);
        return s;
    }

    public State check(State s, String user) {
        applyAction(s, user, ActionType.CHECK, 0, true);
        return s;
    }

    public State fold(State s, String user) {
        applyAction(s, user, ActionType.FOLD, 0, true);
        return s;
    }

    private void applyAction(State s, String user, ActionType type, int amount, boolean triggerAuto) {
        if (!s.inProgress) {
            return;
        }
        Side side = s.players.get(user);
        if (side == null || side.folded) {
            return;
        }
        if (s.turn != null && !s.turn.equals(user)) {
            return;
        }

        int idx = s.order.indexOf(user);
        if (idx < 0) {
            return;
        }

        int betAmount = Math.max(0, amount);
        if (type == ActionType.BET) {
            betAmount = Math.max(s.ante, betAmount);
            side.contributed += betAmount;
            side.lastAmount = betAmount;
            side.lastAction = "BET";
            s.pot += betAmount;
            s.bets.put(user, side.contributed);
        } else if (type == ActionType.CHECK) {
            side.lastAmount = 0;
            side.lastAction = "CHECK";
        } else if (type == ActionType.FOLD) {
            side.lastAmount = 0;
            side.lastAction = "FOLD";
            side.folded = true;
            s.order.remove(user);
            s.bets.put(user, side.contributed);
            if (activePlayers(s) <= 1) {
                s.inProgress = false;
            }
        }

        s.lastActor = user;
        s.lastActionType = type;
        s.lastActionAmount = side.lastAmount;

        int startIdx = type == ActionType.FOLD ? idx - 1 : idx;
        advanceTurnFrom(s, startIdx);

        if (triggerAuto) {
            autoAct(s);
        }
    }

    private void dealInitial(State s) {
        for (String u : s.order) {
            draw(s, s.players.get(u), 2);
        }
        for (String u : s.order) {
            draw(s, s.players.get(u), 1);
        }
    }

    private void dealNextCard(State s) {
        for (String u : new ArrayList<>(s.order)) {
            Side side = s.players.get(u);
            if (side != null && !side.folded) {
                side.cards.add(s.deck.draw());
            }
        }
    }

    private void collectAntes(State s) {
        for (String u : s.order) {
            Side side = s.players.get(u);
            if (side == null) {
                continue;
            }
            side.contributed += s.ante;
            s.pot += s.ante;
            s.bets.put(u, side.contributed);
            side.lastAction = "WAITING";
            side.lastAmount = 0;
        }
    }

    private void prepareNextStreet(State s) {
        for (Side side : s.players.values()) {
            if (side.folded) {
                continue;
            }
            side.lastAction = "WAITING";
            side.lastAmount = 0;
        }
        s.lastActor = null;
        s.lastActionType = ActionType.NONE;
        s.lastActionAmount = 0;
    }

    private void resetTurn(State s) {
        s.turnIndex = -1;
        advanceTurnFrom(s, -1);
    }

    private void advanceTurnFrom(State s, int startIdx) {
        if (!s.inProgress || s.order.isEmpty()) {
            s.turn = null;
            return;
        }
        int size = s.order.size();
        int idx = startIdx;
        if (idx < -1) {
            idx = -1;
        }
        for (int i = 0; i < size; i++) {
            idx = (idx + 1 + size) % size;
            String candidate = s.order.get(idx);
            Side candSide = s.players.get(candidate);
            if (candSide != null && !candSide.folded) {
                s.turnIndex = idx;
                s.turn = candidate;
                return;
            }
        }
        s.turn = null;
    }

    private void autoAct(State s) {
        int guard = 0;
        while (s.inProgress && s.turn != null && guard++ < 10) {
            String actor = s.turn;
            Side side = s.players.get(actor);
            if (side == null || !side.ai || side.folded) {
                break;
            }
            AiDecision decision = decideFor(s, actor, side);
            if (decision == null || decision.type == ActionType.NONE) {
                break;
            }
            applyAction(s, actor, decision.type, decision.amount, false);
        }
    }

    private AiDecision decideFor(State s, String user, Side side) {
        double strength = evaluateStrength(side.cards);
        boolean opponentBet = s.lastActionType == ActionType.BET && !Objects.equals(s.lastActor, user);
        double streetWeight = side.profile != null && s.stage < side.profile.streetMult.length
                ? side.profile.streetMult[s.stage] : 1.0;

        if (opponentBet) {
            double threshold = 0.25 + side.profile.callTight * 0.5;
            double adjusted = strength + rng.nextDouble() * 0.2 - side.profile.callTight * 0.2;
            if (adjusted < threshold) {
                if (rng.nextDouble() < side.profile.callTight + 0.15) {
                    return new AiDecision(ActionType.FOLD, 0);
                }
            }
            int amount = Math.max(s.lastActionAmount, s.ante);
            if (strength > 0.65) {
                amount += s.ante;
            }
            return new AiDecision(ActionType.BET, amount);
        }

        double semi = hasDrawPotential(side.cards) ? side.profile.semiBluff : 0.0;
        double betProb = strength * side.profile.betAgg * streetWeight + side.profile.bluffBase + semi;
        betProb = Math.min(0.95, betProb);
        if (rng.nextDouble() < betProb) {
            int base = Math.max(10, s.ante);
            int amount = base;
            if (strength > 0.7) {
                amount = (int) Math.round(base * 1.5);
            }
            if (strength > 0.85) {
                amount = base * 2;
            }
            return new AiDecision(ActionType.BET, amount);
        }
        return new AiDecision(ActionType.CHECK, 0);
    }

    private double evaluateStrength(List<Card> cards) {
        if (cards.isEmpty()) {
            return 0.0;
        }
        Map<Card.Rank, Integer> rankCounts = new EnumMap<>(Card.Rank.class);
        Map<Card.Suit, Integer> suitCounts = new EnumMap<>(Card.Suit.class);
        int highest = 0;
        for (Card c : cards) {
            rankCounts.merge(c.rank(), 1, Integer::sum);
            suitCounts.merge(c.suit(), 1, Integer::sum);
            highest = Math.max(highest, c.rank().ordinal());
        }

        int pairs = 0;
        int trips = 0;
        int quads = 0;
        for (int cnt : rankCounts.values()) {
            if (cnt == 4) {
                quads++;
            } else if (cnt == 3) {
                trips++;
            } else if (cnt == 2) {
                pairs++;
            }
        }

        double strength = (highest / 12.0) * 0.3;
        strength += pairs * 0.2;
        strength += trips * 0.35;
        strength += quads * 0.6;
        if (pairs >= 2) {
            strength += 0.15;
        }
        if (isFlush(suitCounts)) {
            strength += 0.4;
        }
        if (isStraight(rankCounts)) {
            strength += 0.35;
        }
        return Math.max(0.0, Math.min(1.0, strength));
    }

    private boolean hasDrawPotential(List<Card> cards) {
        if (cards.size() < 4) {
            return false;
        }
        Map<Card.Suit, Integer> suitCounts = new EnumMap<>(Card.Suit.class);
        Set<Integer> ranks = new HashSet<>();
        for (Card c : cards) {
            suitCounts.merge(c.suit(), 1, Integer::sum);
            ranks.add(c.rank().ordinal());
        }
        for (int cnt : suitCounts.values()) {
            if (cnt >= Math.max(3, cards.size() - 1)) {
                return true;
            }
        }
        List<Integer> sorted = new ArrayList<>(ranks);
        Collections.sort(sorted);
        int run = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) == sorted.get(i - 1) + 1) {
                run++;
                if (run >= cards.size() - 1) {
                    return true;
                }
            } else {
                run = 1;
            }
        }
        return false;
    }

    private boolean isFlush(Map<Card.Suit, Integer> suitCounts) {
        for (int cnt : suitCounts.values()) {
            if (cnt >= 5) {
                return true;
            }
        }
        return false;
    }

    private boolean isStraight(Map<Card.Rank, Integer> rankCounts) {
        if (rankCounts.size() < 5) {
            return false;
        }
        List<Integer> vals = new ArrayList<>();
        for (Card.Rank r : rankCounts.keySet()) {
            vals.add(r.ordinal());
        }
        Collections.sort(vals);
        int run = 1;
        for (int i = 1; i < vals.size(); i++) {
            if (vals.get(i) == vals.get(i - 1) + 1) {
                run++;
                if (run >= 5) {
                    return true;
                }
            } else if (vals.get(i) != vals.get(i - 1)) {
                run = 1;
            }
        }
        // Wheel straight (A-2-3-4-5)
        if (rankCounts.containsKey(Card.Rank.ACE)
                && rankCounts.containsKey(Card.Rank.TWO)
                && rankCounts.containsKey(Card.Rank.THREE)
                && rankCounts.containsKey(Card.Rank.FOUR)
                && rankCounts.containsKey(Card.Rank.FIVE)) {
            return true;
        }
        return false;
    }

    private int activePlayers(State s) {
        int count = 0;
        for (Side side : s.players.values()) {
            if (!side.folded) {
                count++;
            }
        }
        return count;
    }

    private void draw(State s, Side side, int n) {
        if (side == null) {
            return;
        }
        for (int i = 0; i < n; i++) {
            side.cards.add(s.deck.draw());
        }
    }

    private static boolean isAiUser(String user) {
        String u = user == null ? "" : user.toUpperCase(Locale.ROOT);
        return u.contains("_AI") || u.startsWith("AI_") || u.endsWith("AI");
    }
}
