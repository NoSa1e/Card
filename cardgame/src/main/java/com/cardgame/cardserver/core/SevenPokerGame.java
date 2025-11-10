package com.cardgame.cardserver.core;

import com.cardgame.cardserver.core.poker.PokerEval;
import com.cardgame.cardserver.core.poker.PokerEval.HandScore;

import java.util.*;

public class SevenPokerGame {

    private static final String SHOWDOWN = "Showdown";
    private static final int MAX_RAISES_PER_STREET = 3;

    private static final StreetInfo[] STREETS = {
            new StreetInfo("3rd Street", 1, false, true),
            new StreetInfo("4th Street", 1, true, true),
            new StreetInfo("5th Street", 2, true, true),
            new StreetInfo("6th Street", 2, true, true),
            new StreetInfo("7th Street", 2, true, false)
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
        public boolean winner = false;
        public Integer payout;
        public HandScore showdownScore;

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
        public int currentBet;
        public int raisesThisStreet;
        public final Map<String, Integer> streetContribution = new LinkedHashMap<>();
        public final LinkedHashSet<String> pending = new LinkedHashSet<>();
        public final Map<String, Integer> payouts = new LinkedHashMap<>();
        public final Map<String, HandScore> showdownScores = new LinkedHashMap<>();
        public final List<String> winners = new ArrayList<>();
        public int settledPot;
    }

    private enum ActionType { NONE, BET, RAISE, CALL, CHECK, FOLD, WIN, LOSE }

    private static final class ActionResult {
        final ActionType type;
        final int paid;

        ActionResult(ActionType type, int paid) {
            this.type = type;
            this.paid = paid;
        }
    }

    private static final class AiDecision {
        final ActionType type;
        final int amount;

        AiDecision(ActionType type, int amount) {
            this.type = type;
            this.amount = amount;
        }
    }

    private static final class StreetInfo {
        final String name;
        final int betUnitMultiplier;
        final boolean dealCard;
        final boolean faceUp;

        StreetInfo(String name, int betUnitMultiplier, boolean dealCard, boolean faceUp) {
            this.name = name;
            this.betUnitMultiplier = betUnitMultiplier;
            this.dealCard = dealCard;
            this.faceUp = faceUp;
        }
    }

    private static final class LeadScore implements Comparable<LeadScore> {
        final int category;
        final int[] keys;

        LeadScore(int category, int[] keys) {
            this.category = category;
            this.keys = keys;
        }

        @Override
        public int compareTo(LeadScore other) {
            int cmp = Integer.compare(this.category, other.category);
            if (cmp != 0) {
                return cmp;
            }
            int len = Math.min(this.keys.length, other.keys.length);
            for (int i = 0; i < len; i++) {
                cmp = Integer.compare(this.keys[i], other.keys[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(this.keys.length, other.keys.length);
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

    public State createState() {
        return new State();
    }

    public State start(State s, Collection<String> users, int ante) {
        s.inProgress = true;
        s.stage = 0;
        s.stageName = STREETS[0].name;
        s.pot = 0;
        s.ante = Math.max(10, ante);
        s.bets.clear();
        s.players.clear();
        s.order.clear();
        s.lastActor = null;
        s.lastActionType = ActionType.NONE;
        s.lastActionAmount = 0;
        s.deck = new Deck(1);
        s.turnIndex = -1;
        s.turn = null;
        s.currentBet = 0;
        s.raisesThisStreet = 0;
        s.streetContribution.clear();
        s.pending.clear();
        s.payouts.clear();
        s.showdownScores.clear();
        s.winners.clear();
        s.settledPot = 0;

        for (String raw : users) {
            String u = raw == null ? "" : raw.trim();
            if (u.isEmpty()) {
                continue;
            }
            Side side = new Side();
            side.ai = isAiUser(u);
            if (side.ai) {
                side.profile = DealerProfile.random(rng);
            }
            s.players.put(u, side);
            s.bets.put(u, 0);
            s.order.add(u);
        }

        dealInitial(s);
        collectAntes(s);
        startStreet(s);
        autoAct(s);
        return s;
    }

    public State bet(State s, String user, int amount) {
        applyAction(s, user, ActionType.BET, Math.max(0, amount), true);
        return s;
    }

    public State call(State s, String user) {
        applyAction(s, user, ActionType.CALL, 0, true);
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

    public State next(State s) {
        if (!s.inProgress) {
            return s;
        }
        s.pending.clear();
        advanceStreetOrFinish(s);
        autoAct(s);
        return s;
    }

    private void applyAction(State s, String user, ActionType requested, int amount, boolean triggerAuto) {
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
        if (!s.pending.isEmpty() && !s.pending.contains(user)) {
            return;
        }

        ActionResult result = switch (requested) {
            case BET -> handleBet(s, user, amount);
            case CALL -> handleCall(s, user);
            case CHECK -> handleCheck(s, user);
            case FOLD -> handleFold(s, user);
            default -> null;
        };

        if (result == null) {
            return;
        }

        s.lastActor = user;
        s.lastActionType = result.type;
        s.lastActionAmount = result.paid;

        if (!s.inProgress) {
            s.turn = null;
            s.turnIndex = -1;
            return;
        }

        if (isStreetComplete(s)) {
            advanceStreetOrFinish(s);
            if (triggerAuto) {
                autoAct(s);
            }
        } else {
            int idx = s.order.indexOf(user);
            advanceTurnFrom(s, idx);
            if (triggerAuto) {
                autoAct(s);
            }
        }
    }

    private ActionResult handleBet(State s, String user, int amount) {
        int betUnit = streetBetUnit(s);
        int effectiveRaise = Math.max(betUnit, amount);
        int previous = s.streetContribution.getOrDefault(user, 0);
        Side side = s.players.get(user);

        if (s.currentBet == 0) {
            int target = effectiveRaise;
            int pay = Math.max(0, target - previous);
            s.currentBet = target;
            s.raisesThisStreet = 0;
            s.streetContribution.put(user, target);
            contribute(s, user, pay);
            side.lastAction = ActionType.BET.name();
            side.lastAmount = pay;
            resetPendingAfterAggressiveAction(s, user);
            s.pending.remove(user);
            return new ActionResult(ActionType.BET, pay);
        }

        if (s.raisesThisStreet >= MAX_RAISES_PER_STREET) {
            return handleCall(s, user);
        }

        int target = s.currentBet + effectiveRaise;
        int pay = Math.max(0, target - previous);
        s.currentBet = target;
        s.raisesThisStreet++;
        s.streetContribution.put(user, target);
        contribute(s, user, pay);
        side.lastAction = ActionType.RAISE.name();
        side.lastAmount = pay;
        resetPendingAfterAggressiveAction(s, user);
        s.pending.remove(user);
        return new ActionResult(ActionType.RAISE, pay);
    }

    private ActionResult handleCall(State s, String user) {
        int toCall = neededToCall(s, user);
        if (s.currentBet == 0 || toCall <= 0) {
            return handleCheck(s, user);
        }
        Side side = s.players.get(user);
        int pay = Math.max(0, toCall);
        contribute(s, user, pay);
        int newContribution = Math.min(s.currentBet, s.streetContribution.getOrDefault(user, 0) + pay);
        s.streetContribution.put(user, newContribution);
        s.pending.remove(user);
        side.lastAction = ActionType.CALL.name();
        side.lastAmount = pay;
        return new ActionResult(ActionType.CALL, pay);
    }

    private ActionResult handleCheck(State s, String user) {
        if (s.currentBet > 0) {
            return null;
        }
        Side side = s.players.get(user);
        s.pending.remove(user);
        side.lastAction = ActionType.CHECK.name();
        side.lastAmount = 0;
        return new ActionResult(ActionType.CHECK, 0);
    }

    private ActionResult handleFold(State s, String user) {
        Side side = s.players.get(user);
        side.folded = true;
        side.lastAction = ActionType.FOLD.name();
        side.lastAmount = 0;
        s.pending.remove(user);
        s.streetContribution.remove(user);
        if (activePlayers(s) <= 1) {
            settleByFold(s, user);
        }
        return new ActionResult(ActionType.FOLD, 0);
    }

    private void dealInitial(State s) {
        for (String u : s.order) {
            Side side = s.players.get(u);
            if (side == null) {
                continue;
            }
            draw(s, side, 2);
        }
        for (String u : s.order) {
            Side side = s.players.get(u);
            if (side == null) {
                continue;
            }
            draw(s, side, 1);
        }
    }

    private void collectAntes(State s) {
        for (String u : s.order) {
            Side side = s.players.get(u);
            if (side == null) {
                continue;
            }
            contribute(s, u, s.ante);
            side.lastAction = "ANTE";
            side.lastAmount = s.ante;
        }
    }

    private void startStreet(State s) {
        if (!s.inProgress) {
            return;
        }
        s.currentBet = 0;
        s.raisesThisStreet = 0;
        s.streetContribution.clear();
        s.pending.clear();
        for (Side side : s.players.values()) {
            if (side == null || side.folded) {
                continue;
            }
            side.lastAction = "WAITING";
            side.lastAmount = 0;
        }
        int leadIndex = findLeadIndex(s);
        if (leadIndex < 0) {
            s.turn = null;
            s.turnIndex = -1;
            return;
        }
        s.turnIndex = leadIndex;
        s.turn = s.order.get(leadIndex);
        int size = s.order.size();
        for (int offset = 0; offset < size; offset++) {
            int idx = (leadIndex + offset) % size;
            String uid = s.order.get(idx);
            Side side = s.players.get(uid);
            if (side == null || side.folded) {
                continue;
            }
            s.streetContribution.put(uid, 0);
            s.pending.add(uid);
        }
        s.lastActor = null;
        s.lastActionType = ActionType.NONE;
        s.lastActionAmount = 0;
    }

    private void advanceStreetOrFinish(State s) {
        if (!s.inProgress) {
            return;
        }
        if (activePlayers(s) <= 1) {
            settleByFold(s, null);
            return;
        }
        if (s.stage >= STREETS.length - 1) {
            resolveShowdown(s);
            return;
        }
        s.stage++;
        StreetInfo info = STREETS[s.stage];
        if (info.dealCard) {
            dealStreetCards(s, info.faceUp);
        }
        s.stageName = info.name;
        startStreet(s);
    }

    private void dealStreetCards(State s, boolean faceUp) {
        for (String u : s.order) {
            Side side = s.players.get(u);
            if (side == null || side.folded) {
                continue;
            }
            side.cards.add(s.deck.draw());
        }
    }

    private void advanceTurnFrom(State s, int startIdx) {
        if (!s.inProgress) {
            s.turn = null;
            s.turnIndex = -1;
            return;
        }
        if (s.pending.isEmpty()) {
            s.turn = null;
            s.turnIndex = -1;
            return;
        }
        int size = s.order.size();
        int idx = startIdx;
        for (int i = 0; i < size; i++) {
            idx = (idx + 1 + size) % size;
            String candidate = s.order.get(idx);
            if (!s.pending.contains(candidate)) {
                continue;
            }
            Side side = s.players.get(candidate);
            if (side == null || side.folded) {
                continue;
            }
            s.turnIndex = idx;
            s.turn = candidate;
            return;
        }
        s.turn = null;
        s.turnIndex = -1;
    }

    private void autoAct(State s) {
        int guard = 0;
        while (s.inProgress && s.turn != null && guard++ < 20) {
            String actor = s.turn;
            Side side = s.players.get(actor);
            if (side == null || !side.ai || side.folded) {
                break;
            }
            AiDecision decision = decideFor(s, actor, side);
            if (decision == null) {
                break;
            }
            applyAction(s, actor, decision.type, decision.amount, false);
        }
    }

    private AiDecision decideFor(State s, String user, Side side) {
        int toCall = neededToCall(s, user);
        double strength = evaluateStrength(side.cards);
        double streetWeight = side.profile != null && s.stage < side.profile.streetMult.length
                ? side.profile.streetMult[s.stage]
                : 1.0;

        if (toCall > 0) {
            double threshold = 0.25 + side.profile.callTight * 0.5;
            double adjusted = strength + rng.nextDouble() * 0.2 - side.profile.callTight * 0.2;
            if (adjusted < threshold) {
                if (rng.nextDouble() < side.profile.callTight + 0.15) {
                    return new AiDecision(ActionType.FOLD, 0);
                }
            }
            if (s.raisesThisStreet < MAX_RAISES_PER_STREET) {
                double raiseChance = strength * side.profile.raiseAgg * streetWeight;
                if (rng.nextDouble() < raiseChance) {
                    return new AiDecision(ActionType.BET, streetBetUnit(s));
                }
            }
            return new AiDecision(ActionType.CALL, 0);
        }

        double semi = hasDrawPotential(side.cards) && side.profile != null ? side.profile.semiBluff : 0.0;
        double betProb = strength * (side.profile != null ? side.profile.betAgg : 0.4) * streetWeight
                + (side.profile != null ? side.profile.bluffBase : 0.05) + semi;
        betProb = Math.min(0.95, betProb);
        if (rng.nextDouble() < betProb) {
            return new AiDecision(ActionType.BET, streetBetUnit(s));
        }
        return new AiDecision(ActionType.CHECK, 0);
    }

    private void settleByFold(State s, String folder) {
        String winner = null;
        for (String uid : s.order) {
            Side side = s.players.get(uid);
            if (side != null && !side.folded) {
                winner = uid;
                break;
            }
        }
        if (winner == null) {
            s.inProgress = false;
            s.turn = null;
            s.turnIndex = -1;
            s.stage = STREETS.length;
            s.stageName = SHOWDOWN;
            return;
        }
        Side winSide = s.players.get(winner);
        winSide.winner = true;
        winSide.lastAction = ActionType.WIN.name();
        winSide.lastAmount = s.pot;
        winSide.payout = s.pot;
        s.winners.clear();
        s.winners.add(winner);
        s.payouts.clear();
        s.payouts.put(winner, s.pot);
        s.settledPot = s.pot;
        for (String uid : s.order) {
            if (uid.equals(winner)) {
                continue;
            }
            Side side = s.players.get(uid);
            if (side == null) {
                continue;
            }
            side.lastAction = side.folded ? ActionType.FOLD.name() : ActionType.LOSE.name();
            side.lastAmount = 0;
            side.payout = 0;
        }
        s.pot = 0;
        s.inProgress = false;
        s.turn = null;
        s.turnIndex = -1;
        s.pending.clear();
        s.stage = STREETS.length;
        s.stageName = SHOWDOWN;
    }

    private void resolveShowdown(State s) {
        List<String> contenders = new ArrayList<>();
        for (String uid : s.order) {
            Side side = s.players.get(uid);
            if (side != null && !side.folded) {
                contenders.add(uid);
            }
        }
        if (contenders.isEmpty()) {
            settleByFold(s, null);
            return;
        }
        s.showdownScores.clear();
        HandScore best = null;
        for (String uid : contenders) {
            Side side = s.players.get(uid);
            HandScore score = bestFiveScore(side.cards);
            side.showdownScore = score;
            s.showdownScores.put(uid, score);
            if (best == null || score.compareTo(best) > 0) {
                best = score;
            }
        }
        List<String> winners = new ArrayList<>();
        for (String uid : contenders) {
            HandScore score = s.showdownScores.get(uid);
            if (score != null && score.compareTo(best) == 0) {
                winners.add(uid);
            }
        }
        int pot = s.pot;
        s.settledPot = pot;
        int share = winners.isEmpty() ? 0 : pot / winners.size();
        int remainder = winners.isEmpty() ? 0 : pot % winners.size();
        s.payouts.clear();
        for (String uid : s.order) {
            Side side = s.players.get(uid);
            if (side == null) {
                continue;
            }
            side.winner = false;
        }
        for (String uid : winners) {
            int payout = share;
            if (remainder > 0) {
                payout += 1;
                remainder--;
            }
            Side side = s.players.get(uid);
            if (side != null) {
                side.winner = true;
                side.lastAction = ActionType.WIN.name();
                side.lastAmount = payout;
                side.payout = payout;
            }
            s.payouts.put(uid, payout);
        }
        for (String uid : contenders) {
            if (winners.contains(uid)) {
                continue;
            }
            Side side = s.players.get(uid);
            if (side != null) {
                side.lastAction = ActionType.LOSE.name();
                side.lastAmount = 0;
                side.payout = 0;
            }
        }
        s.pot = 0;
        s.inProgress = false;
        s.stage = STREETS.length;
        s.stageName = SHOWDOWN;
        s.turn = null;
        s.turnIndex = -1;
        s.pending.clear();
        s.winners.clear();
        s.winners.addAll(winners);
    }

    private int findLeadIndex(State s) {
        LeadScore best = null;
        int bestIdx = -1;
        for (int i = 0; i < s.order.size(); i++) {
            String uid = s.order.get(i);
            Side side = s.players.get(uid);
            if (side == null || side.folded) {
                continue;
            }
            LeadScore score = leadScore(side, s.stage);
            if (score == null) {
                continue;
            }
            if (best == null || score.compareTo(best) > 0) {
                best = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private LeadScore leadScore(Side side, int stage) {
        List<Card> openCards = openCards(side, stage);
        if (openCards.isEmpty()) {
            return new LeadScore(-1, new int[0]);
        }
        int category = leadCategory(openCards);
        int[] keys = openCards.stream()
                .sorted(Comparator.<Card>comparingInt(this::cardKey).reversed())
                .mapToInt(this::cardKey)
                .toArray();
        return new LeadScore(category, keys);
    }

    private int leadCategory(List<Card> openCards) {
        Map<Integer, Integer> rankCounts = new HashMap<>();
        Map<Card.Suit, Integer> suitCounts = new EnumMap<>(Card.Suit.class);
        SortedSet<Integer> ranks = new TreeSet<>();
        for (Card c : openCards) {
            int rv = rankValue(c.rank());
            rankCounts.merge(rv, 1, Integer::sum);
            suitCounts.merge(c.suit(), 1, Integer::sum);
            ranks.add(rv);
        }
        int maxCount = 0;
        int pairCount = 0;
        int tripleCount = 0;
        for (int cnt : rankCounts.values()) {
            maxCount = Math.max(maxCount, cnt);
            if (cnt == 2) {
                pairCount++;
            } else if (cnt == 3) {
                tripleCount++;
            }
        }
        boolean flush = false;
        for (int cnt : suitCounts.values()) {
            if (cnt == openCards.size()) {
                flush = true;
                break;
            }
        }
        boolean straight = isStraight(ranks);
        if (straight && flush && openCards.size() >= 3) {
            return 8;
        }
        if (maxCount >= 4) {
            return 7;
        }
        if (tripleCount >= 1 && pairCount >= 1) {
            return 6;
        }
        if (flush && openCards.size() >= 3) {
            return 5;
        }
        if (straight && openCards.size() >= 3) {
            return 4;
        }
        if (tripleCount >= 1) {
            return 3;
        }
        if (pairCount >= 2) {
            return 2;
        }
        if (pairCount == 1) {
            return 1;
        }
        return 0;
    }

    private boolean isStreetComplete(State s) {
        if (!s.inProgress) {
            return true;
        }
        if (!s.pending.isEmpty()) {
            return false;
        }
        if (s.currentBet == 0) {
            return true;
        }
        for (String uid : s.order) {
            Side side = s.players.get(uid);
            if (side == null || side.folded) {
                continue;
            }
            int paid = s.streetContribution.getOrDefault(uid, 0);
            if (paid < s.currentBet) {
                return false;
            }
        }
        return true;
    }

    private void resetPendingAfterAggressiveAction(State s, String aggressor) {
        s.pending.clear();
        int size = s.order.size();
        int startIdx = s.order.indexOf(aggressor);
        for (int offset = 1; offset <= size; offset++) {
            int idx = (startIdx + offset) % size;
            String uid = s.order.get(idx);
            if (uid.equals(aggressor)) {
                continue;
            }
            Side side = s.players.get(uid);
            if (side == null || side.folded) {
                continue;
            }
            s.streetContribution.putIfAbsent(uid, 0);
            s.pending.add(uid);
        }
    }

    private List<Card> openCards(Side side, int stage) {
        List<Card> open = new ArrayList<>();
        if (side.cards.size() <= 2) {
            return open;
        }
        if (stage <= 0) {
            if (side.cards.size() > 2) {
                open.add(side.cards.get(2));
            }
            return open;
        }
        int maxIndex = Math.min(2 + stage, side.cards.size() - 1);
        if (stage >= 4) {
            maxIndex = Math.min(5, side.cards.size() - 1);
        }
        for (int i = 2; i <= maxIndex; i++) {
            open.add(side.cards.get(i));
        }
        return open;
    }

    private int neededToCall(State s, String user) {
        if (s.currentBet == 0) {
            return 0;
        }
        int paid = s.streetContribution.getOrDefault(user, 0);
        return Math.max(0, s.currentBet - paid);
    }

    private int streetBetUnit(State s) {
        if (s.stage < 0) {
            return s.ante;
        }
        int idx = Math.min(s.stage, STREETS.length - 1);
        int base = Math.max(10, s.ante);
        return base * STREETS[idx].betUnitMultiplier;
    }

    private void contribute(State s, String user, int amount) {
        if (amount <= 0) {
            return;
        }
        s.pot += amount;
        Side side = s.players.get(user);
        side.contributed += amount;
        s.bets.put(user, side.contributed);
    }

    private void draw(State s, Side side, int n) {
        for (int i = 0; i < n; i++) {
            side.cards.add(s.deck.draw());
        }
    }

    private int activePlayers(State s) {
        int count = 0;
        for (Side side : s.players.values()) {
            if (side != null && !side.folded) {
                count++;
            }
        }
        return count;
    }

    private HandScore bestFiveScore(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalStateException("Need at least 5 cards for showdown");
        }
        HandScore best = null;
        int n = cards.size();
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            List<Card> hand = List.of(
                                    cards.get(a),
                                    cards.get(b),
                                    cards.get(c),
                                    cards.get(d),
                                    cards.get(e));
                            HandScore score = PokerEval.evaluate(hand);
                            if (best == null || score.compareTo(best) > 0) {
                                best = score;
                            }
                        }
                    }
                }
            }
        }
        return best;
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
            } else if (!Objects.equals(vals.get(i), vals.get(i - 1))) {
                run = 1;
            }
        }
        if (rankCounts.containsKey(Card.Rank.ACE)
                && rankCounts.containsKey(Card.Rank.TWO)
                && rankCounts.containsKey(Card.Rank.THREE)
                && rankCounts.containsKey(Card.Rank.FOUR)
                && rankCounts.containsKey(Card.Rank.FIVE)) {
            return true;
        }
        return false;
    }

    private boolean isStraight(SortedSet<Integer> ranks) {
        if (ranks.size() < 3) {
            return false;
        }
        int run = 1;
        Integer prev = null;
        for (Integer v : ranks) {
            if (prev != null) {
                if (v == prev + 1) {
                    run++;
                    if (run >= 3) {
                        return true;
                    }
                } else if (!v.equals(prev)) {
                    run = 1;
                }
            }
            prev = v;
        }
        if (ranks.contains(rankValue(Card.Rank.ACE)) && ranks.contains(rankValue(Card.Rank.TWO))
                && ranks.contains(rankValue(Card.Rank.THREE))) {
            return true;
        }
        return false;
    }

    private int cardKey(Card card) {
        return rankValue(card.rank()) * 10 + suitValue(card.suit());
    }

    private int rankValue(Card.Rank rank) {
        return rank.ordinal() + 2;
    }

    private int suitValue(Card.Suit suit) {
        return switch (suit) {
            case C -> 0;
            case D -> 1;
            case H -> 2;
            case S -> 3;
        };
    }

    public int minRaise(State s) {
        return streetBetUnit(s);
    }

    public int toCall(State s, String user) {
        return neededToCall(s, user);
    }

    public List<String> pendingOrder(State s) {
        return new ArrayList<>(s.pending);
    }

    public int currentBet(State s) {
        return s != null ? s.currentBet : 0;
    }

    public List<String> winners(State s) {
        if (s == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(s.winners);
    }

    private static boolean isAiUser(String user) {
        String u = user == null ? "" : user.toUpperCase(Locale.ROOT);
        return u.contains("_AI") || u.startsWith("AI_") || u.endsWith("AI");
    }
}
