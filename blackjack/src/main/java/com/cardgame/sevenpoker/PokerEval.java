package com.cardgame.sevenpoker;

import com.cardgame.card.Card;
import java.util.*;

public final class PokerEval {

    private PokerEval(){}

    /** 낮→높 */
    public enum HandRank {
        HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND,
        STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH
    }

    /** 결과: 족보 + 타이브레이커 배열(왼쪽부터 비교) */
    public static final class HandScore implements Comparable<HandScore> {
        public final HandRank rank;
        public final int[] tb; // tie-breakers

        public HandScore(HandRank rank, int... tb) {
            this.rank = rank;
            this.tb = tb;
        }
        @Override public int compareTo(HandScore o) {
            int r = Integer.compare(this.rank.ordinal(), o.rank.ordinal());
            if (r != 0) return r;
            for (int i = 0; i < Math.min(tb.length, o.tb.length); i++) {
                int c = Integer.compare(this.tb[i], o.tb[i]);
                if (c != 0) return c;
            }
            return Integer.compare(this.tb.length, o.tb.length);
        }
        @Override public String toString() { return rank.toString(); }
    }

    /* ===================== public API ===================== */

    /** 5장 손패 평가 (A=14, A-로우 스트레이트 지원) */
    public static HandScore evaluate(List<Card> hand) {
        if (hand.size() != 5) throw new IllegalArgumentException("need exactly 5 cards");

        int[] rc = new int[15];   // rank counts (2..14)
        int[] sc = new int[4];    // suit counts (0..3)
        for (Card c : hand) {
            rc[rankValue(c.rank())]++;
            sc[c.suit().ordinal()]++;
        }

        boolean isFlush = false;
        for (int s : sc) if (s == 5) { isFlush = true; break; }

        int straightHigh = straightHigh(rc); // 없으면 -1, wheel일 때 5
        boolean isStraight = straightHigh != -1;

        // 같은 랭크 묶음: cnt -> ranks(desc)
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int rv = 14; rv >= 2; rv--) {
            int cnt = rc[rv];
            if (cnt > 0) groups.computeIfAbsent(cnt, k -> new ArrayList<>()).add(rv);
        }

        // ===== 스트레이트 플러시 / 플러시 / 스트레이트 =====
        if (isFlush && isStraight) {
            int suitTie = highestSuitForRank(hand, straightHigh);
            return new HandScore(HandRank.STRAIGHT_FLUSH, straightHigh, suitTie);
        }

        List<Integer> fours = groups.getOrDefault(4, List.of());
        List<Integer> trips = groups.getOrDefault(3, List.of());
        List<Integer> pairs = groups.getOrDefault(2, List.of());

        if (!fours.isEmpty()) {
            int four = fours.get(0);
            int fourSuitMax = maxSuitAmong(hand, four);
            int kicker = highestOfCount(groups, 1, four);
            int kickerSuit = maxSuitAmong(hand, kicker);
            return new HandScore(HandRank.FOUR_OF_A_KIND, four, fourSuitMax, kicker, kickerSuit);
        }
        if (!trips.isEmpty() && !pairs.isEmpty()) {
            int t = trips.get(0), p = pairs.get(0);
            int tSuit = maxSuitAmong(hand, t);
            int pSuit = maxSuitAmong(hand, p);
            return new HandScore(HandRank.FULL_HOUSE, t, tSuit, p, pSuit);
        }
        if (isFlush) {
            // 플러시는 5장 모두 같은 문양 → cardKey 내림차순 비교
            int[] keys = hand.stream()
                    .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                    .mapToInt(PokerEval::cardKey).toArray();
            return new HandScore(HandRank.FLUSH, keys);
        }
        if (isStraight) {
            int suitTie = highestSuitForRank(hand, straightHigh);
            return new HandScore(HandRank.STRAIGHT, straightHigh, suitTie);
        }
        if (!trips.isEmpty()) {
            int t = trips.get(0);
            int tSuit = maxSuitAmong(hand, t);
            int[] kickKeys = kickerKeys(hand, Set.of(t), 2);
            return new HandScore(HandRank.THREE_OF_A_KIND, t, tSuit, kickKeys[0], kickKeys[1]);
        }
        if (pairs.size() >= 2) {
            int p1 = pairs.get(0), p2 = pairs.get(1);
            int p1s = maxSuitAmong(hand, p1), p2s = maxSuitAmong(hand, p2);
            int[] kick = kickerKeys(hand, Set.of(p1, p2), 1);
            return new HandScore(HandRank.TWO_PAIR, p1, p1s, p2, p2s, kick[0]);
        }
        if (pairs.size() == 1) {
            int p = pairs.get(0);
            int ps = maxSuitAmong(hand, p);
            int[] kick = kickerKeys(hand, Set.of(p), 3);
            return new HandScore(HandRank.ONE_PAIR, p, ps, kick[0], kick[1], kick[2]);
        }

        // 하이카드: 5장의 cardKey 내림차순
        int[] keys = hand.stream()
                .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                .mapToInt(PokerEval::cardKey).toArray();
        return new HandScore(HandRank.HIGH_CARD, keys);
    }

    /* ===================== helpers ===================== */

    // A=14
    private static int rankValue(Card.Rank r) {
        return switch (r) {
            case ACE -> 14;
            case KING -> 13; case QUEEN -> 12; case JACK -> 11; case TEN -> 10;
            case NINE -> 9; case EIGHT -> 8; case SEVEN -> 7; case SIX -> 6; case FIVE -> 5;
            case FOUR -> 4; case THREE -> 3; case TWO -> 2;
        };
    }

    // ♣0 < ♥1 < ♦2 < ♠3  (원하면 바꿔도 됨)
    private static int suitValue(Card.Suit s) {
        return switch (s) {
            case C -> 0; case H -> 1; case  D -> 2; case S -> 3;
        };
    }

    // 랭크 우선 + 문양 보조 (숫자 하나로 비교)
    private static int cardKey(Card c) {
        return rankValue(c.rank()) * 10 + suitValue(c.suit());
    }

    // 스트레이트 최고랭크(없으면 -1) — wheel(A-2-3-4-5)는 5
    private static int straightHigh(int[] rc) {
        for (int hi = 14; hi >= 6; hi--) {
            boolean ok = true;
            for (int d = 0; d < 5; d++) if (rc[hi - d] == 0) { ok = false; break; }
            if (ok) return hi;
        }
        // wheel
        if (rc[14] > 0 && rc[5] > 0 && rc[4] > 0 && rc[3] > 0 && rc[2] > 0) return 5;
        return -1;
    }

    // 해당 랭크의 카드들 중 가장 높은 문양값
    private static int maxSuitAmong(List<Card> hand, int rankVal) {
        int best = -1;
        for (Card c : hand) if (rankValue(c.rank()) == rankVal) {
            best = Math.max(best, suitValue(c.suit()));
        }
        return Math.max(best, 0);
    }

    // 스트레이트(또는 스트레이트 플러시)의 최고랭크 카드 중 최고 문양값
    private static int highestSuitForRank(List<Card> hand, int rankHigh) {
        int best = -1;
        for (Card c : hand) {
            int rv = rankValue(c.rank());
            // wheel(5-high)일 때 A를 1로 보정해 문양 비교에 포함하고 싶으면 여기 보정 가능
            if (rv == rankHigh) best = Math.max(best, suitValue(c.suit()));
        }
        return Math.max(best, 0);
    }

    // 그룹 외 나머지 킥커들: cardKey 내림차순으로 need개
    private static int[] kickerKeys(List<Card> hand, Set<Integer> excludeRanks, int need) {
        return hand.stream()
                .filter(c -> !excludeRanks.contains(rankValue(c.rank())))
                .sorted(Comparator.<Card>comparingInt(PokerEval::cardKey).reversed())
                .limit(need)
                .mapToInt(PokerEval::cardKey)
                .toArray();
    }

    private static int highestOfCount(Map<Integer, List<Integer>> groups, int cnt, int... exclude) {
        var list = new ArrayList<>(groups.getOrDefault(cnt, List.of()));
        for (int ex : exclude) list.remove(Integer.valueOf(ex));
        return list.isEmpty() ? 0 : list.get(0);
    }
}
