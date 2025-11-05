package com.cardgame.sevenpoker;

import com.cardgame.card.*;
import com.cardgame.card.Game;
import com.cardgame.sevenpoker.PokerEval.*;


import java.util.*;

public class SevenPoker implements Game {

    @Override
    public void playOneRound(java.util.Scanner in, Deck deck) {
        // 베팅 없는 호환 모드
        playOneRoundForBet(in, deck, 0);
    }

    /** 베팅 포함: 단판 쇼다운(무승부 푸시), 승리 +bet / 패배 -bet */
    public int playOneRoundForBet(java.util.Scanner in, Deck deck, int bet) {
        List<Card> player = draw7(deck);
        List<Card> dealer = draw7(deck);

        // 표시(원하면 일부만 공개해도 됨)
        System.out.println("플레이어: " + player.stream().map(CardStrings::show).toList());
        System.out.println("딜러    : " + dealer.stream().map(CardStrings::show).toList());

        HandScore ps = best5Score(player);
        HandScore ds = best5Score(dealer);
        System.out.println("플레이어 족보: " + ps);
        System.out.println("딜러 족보   : " + ds);

        int cmp = ps.compareTo(ds);
        String result = (cmp > 0) ? "플레이어 승" : (cmp < 0) ? "딜러 승" : "무승부";
        System.out.println("결과: " + result);

        if (bet <= 0) return 0;
        if (cmp > 0)  return +bet;
        if (cmp < 0)  return -bet;
        return 0; // 푸시
    }

    // 7장 뽑기
    private List<Card> draw7(Deck deck) {
        List<Card> h = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) h.add(deck.draw());
        return h;
    }

    // 7장 중 베스트 5장 평가 (조합 21개 완전탐색)
    private HandScore best5Score(List<Card> seven) {
        int n = seven.size();
        if (n != 7) throw new IllegalArgumentException("need 7 cards");
        HandScore best = null;

        // 0..6 중 5개 고르는 조합
        for (int a = 0; a < 3; a++) {
            for (int b = a+1; b < 4; b++) {
                for (int c = b+1; c < 5; c++) {
                    for (int d = c+1; d < 6; d++) {
                        for (int e = d+1; e < 7; e++) {
                            var five = List.of(seven.get(a), seven.get(b), seven.get(c), seven.get(d), seven.get(e));
                            HandScore sc = PokerEval.evaluate(five);
                            if (best == null || sc.compareTo(best) > 0) best = sc;
                        }
                    }
                }
            }
        }
        return best;
    }
}
