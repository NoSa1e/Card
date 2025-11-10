package com.cardgame.cardserver.core.poker;

import com.cardgame.cardserver.core.Card;

import java.util.*;

public final class PokerEval {

    private PokerEval() {
    }

    public enum HandRank {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH
    }

    public static final class HandScore implements Comparable<HandScore> {
        public final HandRank rank;
        public final int[] tieBreakers;

        public HandScore(HandRank rank, int... tieBreakers) {
            this.rank = Objects.requireNonNull(rank);
            this.tieBreakers = tieBreakers;
        }

        @Override
        public int compareTo(HandScore other) {
            int cmp = Integer.compare(this.rank.ordinal(), other.rank.ordinal());
            if (cmp != 0) {
                return cmp;
            }
            int len = Math.min(this.tieBreakers.length, other.tieBreakers.length);
            for (int i = 0; i < len; i++) {
                cmp = Integer.compare(this.tieBreakers[i], other.tieBreakers[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(this.tieBreakers.length, other.tieBreakers.length);
        }

        @Override
        public String toString() {
            return rank.toString();
        }
    }

    public static HandScore evaluate(List<Card> hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("need exactly 5 cards");
        }

        int[] rankCounts = new int[15];
        int[] suitCounts = new int[4];
        for (Card c : hand) {
            rankCounts[rankValue(c.rank())]++;
            suitCounts[c.suit().ordinal()]++;
        }

        boolean flush = false;
        for (int s : suitCounts) {
            if (s == 5) {
                flush = true;
                break;
            }
        }

        int straightHigh = straightHigh(rankCounts);
        boolean straight = straightHigh != -1;

        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int rv = 14; rv >= 2; rv--) {
            int cnt = rankCounts[rv];
            if (cnt > 0) {
                groups.computeIfAbsent(cnt, k -> new ArrayList<>()).add(rv);
            }
        }

        if (flush && straight) {
            int suitTie = highestSuitForRank(hand, straightHigh);
            return new HandScore(HandRank.STRAIGHT_FLUSH, straightHigh, suitTie);
        }

        List<Integer> fours = groups.getOrDefault(4, List.of());
        List<Integer> trips = groups.getOrDefault(3, List.of());
        List<Integer> pairs = groups.getOrDefault(2, List.of());

        if (!fours.isEmpty()) {
            int four = fours.get(0);
            int fourSuit = maxSuitAmong(hand, four);
            int kicker = highestOfCount(groups, 1, four);
            int kickerSuit = maxSuitAmong(hand, kicker);
            return new HandScore(HandRank.FOUR_OF_A_KIND, four, fourSuit, kicker, kickerSuit);
        }
        if (!trips.isEmpty() && !pairs.isEmpty()) {
            int three = trips.get(0);
            int pair = pairs.get(0);
            int threeSuit = maxSuitAmong(hand, three);
            int pairSuit = maxSuitAmong(hand, pair);
            return new HandScore(HandRank.FULL_HOUSE, three, threeSuit, pair, pairSuit);
        }
        if (flush) {
            int[] keys = hand.stream()
                    .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                    .mapToInt(PokerEval::cardKey)
                    .toArray();
            return new HandScore(HandRank.FLUSH, keys);
        }
        if (straight) {
            int suitTie = highestSuitForRank(hand, straightHigh);
            return new HandScore(HandRank.STRAIGHT, straightHigh, suitTie);
        }
        if (!trips.isEmpty()) {
            int three = trips.get(0);
            int threeSuit = maxSuitAmong(hand, three);
            int[] kickers = kickerKeys(hand, Set.of(three), 2);
            return new HandScore(HandRank.THREE_OF_A_KIND, three, threeSuit, kickers[0], kickers[1]);
        }
        if (pairs.size() >= 2) {
            int highPair = pairs.get(0);
            int lowPair = pairs.get(1);
            int highSuit = maxSuitAmong(hand, highPair);
            int lowSuit = maxSuitAmong(hand, lowPair);
            int[] kicker = kickerKeys(hand, Set.of(highPair, lowPair), 1);
            return new HandScore(HandRank.TWO_PAIR, highPair, highSuit, lowPair, lowSuit, kicker[0]);
        }
        if (pairs.size() == 1) {
            int pair = pairs.get(0);
            int pairSuit = maxSuitAmong(hand, pair);
            int[] kickers = kickerKeys(hand, Set.of(pair), 3);
            return new HandScore(HandRank.ONE_PAIR, pair, pairSuit, kickers[0], kickers[1], kickers[2]);
        }

        int[] keys = hand.stream()
                .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                .mapToInt(PokerEval::cardKey)
                .toArray();
        return new HandScore(HandRank.HIGH_CARD, keys);
    }

    private static int rankValue(Card.Rank rank) {
        return rank.ordinal() + 2;
    }

    private static int suitValue(Card.Suit suit) {
        return switch (suit) {
            case C -> 0;
            case D -> 1;
            case H -> 2;
            case S -> 3;
        };
    }

    private static int cardKey(Card card) {
        return rankValue(card.rank()) * 10 + suitValue(card.suit());
    }

    private static int straightHigh(int[] rankCounts) {
        for (int high = 14; high >= 6; high--) {
            boolean ok = true;
            for (int d = 0; d < 5; d++) {
                if (rankCounts[high - d] == 0) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return high;
            }
        }
        if (rankCounts[14] > 0 && rankCounts[5] > 0 && rankCounts[4] > 0
                && rankCounts[3] > 0 && rankCounts[2] > 0) {
            return 5;
        }
        return -1;
    }

    private static int maxSuitAmong(List<Card> hand, int rankValue) {
        int best = -1;
        for (Card c : hand) {
            if (rankValue(c.rank()) == rankValue) {
                best = Math.max(best, suitValue(c.suit()));
            }
        }
        return Math.max(best, 0);
    }

    private static int highestSuitForRank(List<Card> hand, int rankHigh) {
        int best = -1;
        for (Card c : hand) {
            int value = rankValue(c.rank());
            if (value == rankHigh || (rankHigh == 5 && value == 14)) {
                best = Math.max(best, suitValue(c.suit()));
            }
        }
        return Math.max(best, 0);
    }

    private static int[] kickerKeys(List<Card> hand, Set<Integer> excludeRanks, int need) {
        return hand.stream()
                .filter(c -> !excludeRanks.contains(rankValue(c.rank())))
                .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                .limit(need)
                .mapToInt(PokerEval::cardKey)
                .toArray();
    }

    private static int highestOfCount(Map<Integer, List<Integer>> groups, int count, int... exclude) {
        List<Integer> list = new ArrayList<>(groups.getOrDefault(count, List.of()));
        for (int ex : exclude) {
            list.remove(Integer.valueOf(ex));
        }
        return list.isEmpty() ? 0 : list.get(0);
    }
}
