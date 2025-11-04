// com/cardgame/blackjack/BlackJack.java  (핵심만 추가/수정)
package com.cardgame.blackjack;

import com.cardgame.card.*;
import com.cardgame.card.Game;
import static com.cardgame.card.CardStrings.*;

import java.util.*;

public class BlackJack implements Game {

    // 기존 메서드는 호환용(베팅 없이 1판만)
    @Override
    public void playOneRound(Scanner in, Deck deck) {
        playOneRoundForBet(in, deck, 0); // delta는 무시
    }

    /** 베팅 포함: 결과 리턴 */
    public int playOneRoundForBet(Scanner in, Deck deck, int bet) {
        boolean withBet = bet > 0;

        // 초기 딜
        List<Card> dealer = new ArrayList<>();
        List<Card> player = new ArrayList<>();
        player.add(deck.draw()); player.add(deck.draw());
        dealer.add(deck.draw()); dealer.add(deck.draw());

        boolean canSplit = canSplit(player);
        List<List<Card>> hands = new ArrayList<>();
        hands.add(new ArrayList<>(player));

        System.out.println("딜러: " + dealerMasked(dealer));
        System.out.println("플레이어 손 #1: " + hand(player, true) + " (합=" + value(player) + ")");
        if (canSplit) System.out.println("3: SPLIT (초기 1회만 가능)");

        boolean didSplit = false;
        while (true) {
            int act = askInt(in, "1: HIT, 2: STAND" + (canSplit ? ", 3: SPLIT" : "") + " > ", 1, canSplit ? 3 : 2);
            if (act == 3 && canSplit) {
                didSplit = true;
                Card a = player.get(0), b = player.get(1);
                List<Card> handA = new ArrayList<>(List.of(a, deck.draw()));
                List<Card> handB = new ArrayList<>(List.of(b, deck.draw()));
                hands.set(0, handA);
                hands.add(handB);
                System.out.println("== 스플릿! ==");
                System.out.println("손 #1: " + hand(handA, true) + " (합=" + value(handA) + ")");
                System.out.println("손 #2: " + hand(handB, true) + " (합=" + value(handB) + ")");
                break;
            } else if (act == 1) {
                hands.get(0).add(deck.draw());
                System.out.println("손 #1: " + hand(hands.get(0), true) + " (합=" + value(hands.get(0)) + ")");
                if (value(hands.get(0)) > 21) {
                    System.out.println("손 #1 버스트! 패배");
                    // 베팅 모드면 정산 후 종료
                    if (withBet) {
                        int delta = -bet;
                        System.out.println("정산: " + delta);
                        return delta;
                    }
                    return 0;
                }
            } else {
                break;
            }
        }

        if (didSplit) {
            for (int idx = 0; idx < hands.size(); idx++) {
                List<Card> h = hands.get(idx);
                while (value(h) <= 21) {
                    int act = askInt(in, "손 #" + (idx+1) + " 1:HIT 2:STAND > ", 1, 2);
                    if (act == 1) {
                        h.add(deck.draw());
                        System.out.println("손 #" + (idx+1) + ": " + hand(h, true) + " (합=" + value(h) + ")");
                        if (value(h) > 21) { System.out.println("손 #" + (idx+1) + " 버스트!"); break; }
                    } else break;
                }
            }
        }

        // 딜러 턴(S17)
        while (value(dealer) < 17) {
            dealer.add(deck.draw());
        }
        int dv = value(dealer);
        System.out.println("딜러 공개: " + hand(dealer, true) + " (합=" + dv + ")");

        // ===== 정산 =====
        int delta = 0;
        if (!didSplit) {
            int pv = value(hands.get(0));
            String label = "단일 손";
            int sub = settleDelta(pv, dv, label, bet);
            delta += sub;
        } else {
            for (int i = 0; i < hands.size(); i++) {
                int pv = value(hands.get(i));
                int sub = settleDelta(pv, dv, "손 #" + (i+1), bet);
                delta += sub;
            }
        }
        if (withBet) System.out.println("총 정산: " + (delta >= 0 ? "+" : "") + delta);
        return delta;
    }

    private int settleDelta(int pv, int dv, String label, int bet) {
        if (pv > 21) { System.out.println(label + " 결과: 버스트(패)"); return bet > 0 ? -bet : 0; }
        if (dv > 21) { System.out.println(label + " 결과: 딜러 버스트(승)"); return bet > 0 ? +bet : 0; }
        if (pv > dv) { System.out.println(label + " 결과: 승"); return bet > 0 ? +bet : 0; }
        if (pv < dv) { System.out.println(label + " 결과: 패"); return bet > 0 ? -bet : 0; }
        System.out.println(label + " 결과: 무승부");
        return 0;
    }

    private boolean canSplit(List<Card> hand) {
        if (hand.size() != 2) return false;
        var r1 = hand.get(0).rank();
        var r2 = hand.get(1).rank();
        if (r1 == r2) return true;
        return isTenValue(r1) && isTenValue(r2);
    }
    private boolean isTenValue(Card.Rank r) {
        return r == Card.Rank.TEN || r == Card.Rank.JACK
                || r == Card.Rank.QUEEN || r == Card.Rank.KING;
    }

    private int value(List<Card> hand) {
        int sum = 0, aces = 0;
        for (Card c : hand) {
            switch (c.rank()) {
                case ACE -> { sum += 11; aces++; }
                case TEN, JACK, QUEEN, KING -> sum += 10;
                default -> sum += c.rank().ordinal() + 1;
            }
        }
        while (sum > 21 && aces-- > 0) sum -= 10;
        return sum;
    }

    private String dealerMasked(List<Card> d) {
        if (d.isEmpty()) return "[]";
        return "[" + card(d.get(0), true) + ", □]";
    }

    private int askInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                System.out.println("숫자 " + min + "~" + max + " 범위로 다시 입력해주세요.");
            }
        }
    }
}
