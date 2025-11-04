// com/cardgame/baccarat/Baccarat.java
package com.cardgame.baccarat;

import com.cardgame.card.*;
import com.cardgame.card.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.cardgame.card.CardStrings.*; // hand(...), card(...)

public class Baccarat implements Game {

    /** 표준 베팅 타깃 */
    public enum Bet { PLAYER, BANKER, TIE }

    // 콘솔 1판(베팅 없는 호환용)
    @Override
    public void playOneRound(Scanner in, Deck deck) {
        // 기본: PLAYER에 0원 베팅으로 돌린 뒤 결과만 콘솔 표시
        playOneRoundForBet(in, deck, 0, Bet.PLAYER);
    }

    /**
     * 베팅 포함 1라운드.
     * @param in    입력(액션 안내용 메시지)
     * @param deck  덱/슈
     * @param bet   베팅 금액(0이면 정산 없이 룰만 진행)
     * @param on    Bet.PLAYER / Bet.BANKER / Bet.TIE
     * @return      delta 정산액(+이익/−손실/0)
     */
    public int playOneRoundForBet(Scanner in, Deck deck, int bet, Bet on) {
        boolean withBet = bet > 0;

        // 초기 2장씩
        List<Card> player = new ArrayList<>();
        List<Card> banker = new ArrayList<>();
        player.add(deck.draw()); player.add(deck.draw());
        banker.add(deck.draw()); banker.add(deck.draw());

        int pv = value(player), bv = value(banker);

        System.out.println("Player: " + hand(player, true) + " (" + pv + ")");
        System.out.println("Banker: " + hand(banker, true) + " (" + bv + ")");

        // 내추럴(8/9) 즉시 종료
        if (pv >= 8 || bv >= 8) {
            return settleAndPrint(pv, bv, bet, on);
        }

        // 플레이어 규칙: 0~5 드로우, 6·7 스탠드
        Card playerThird = null;
        if (pv <= 5) {
            playerThird = deck.draw();
            player.add(playerThird);
            pv = value(player);
            System.out.println("Player draws: " + card(playerThird, true) + " → (" + pv + ")");
        } // 6·7은 스탠드

        // 뱅커 드로우 규칙 (테이블)
        boolean bankerDraws = bankerShouldDraw(bv, playerThird);
        if (bankerDraws) {
            Card b3 = deck.draw();
            banker.add(b3);
            bv = value(banker);
            System.out.println("Banker draws: " + card(b3, true) + " → (" + bv + ")");
        }

        // 최종 결과
        System.out.println("Final Player: " + hand(player, true) + " (" + pv + ")");
        System.out.println("Final Banker: " + hand(banker, true) + " (" + bv + ")");

        return settleAndPrint(pv, bv, bet, on);
    }

    // ===== 점수 계산(A=1, 2..9=숫자, 10/J/Q/K=0, 합%10) =====
    private int value(List<Card> h) {
        int s = 0;
        for (Card c : h) {
            switch (c.rank()) {
                case ACE -> s += 1;
                case TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE -> s += c.rank().ordinal() + 1; // TWO=1 → 2
                default -> {} // TEN, JACK, QUEEN, KING → 0
            }
        }
        return s % 10;
    }

    // ===== 뱅커 드로우 테이블 =====
    // 플레이어가 2장 스탠드한 경우: 뱅커 0~5 드로우, 6·7 스탠드
    // 플레이어가 3장째 뽑은 경우: 아래 상세 표 적용
    private boolean bankerShouldDraw(int bankerTotal, Card playerThird) {
        if (playerThird == null) {
            // 플레이어 스탠드
            return bankerTotal <= 5;
        }
        int pt = thirdCardValue(playerThird); // 0~9
        // 표준 테이블
        // 0~2 : 무조건 드로우
        if (bankerTotal <= 2) return true;
        // 3 : 플레이어 3rd != 8
        if (bankerTotal == 3) return pt != 8;
        // 4 : 플레이어 3rd ∈ 2..7
        if (bankerTotal == 4) return pt >= 2 && pt <= 7;
        // 5 : 플레이어 3rd ∈ 4..7
        if (bankerTotal == 5) return pt >= 4 && pt <= 7;
        // 6 : 플레이어 3rd ∈ 6..7
        if (bankerTotal == 6) return pt == 6 || pt == 7;
        // 7 : 스탠드
        return false;
    }

    private int thirdCardValue(Card c) {
        return switch (c.rank()) {
            case ACE -> 1;
            case TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE -> c.rank().ordinal() + 1;
            default -> 0; // TEN,J,Q,K
        } % 10;
    }

    // ===== 정산 & 출력 =====
    // 베팅 표준: Player 1:1, Banker 1:1 (5% 커미션), Tie 8:1
    // Player/Banker에 베팅했는데 타이면 푸시(0)
    private int settleAndPrint(int pv, int bv, int bet, Bet on) {
        String winner = (pv > bv) ? "PLAYER" : (pv < bv) ? "BANKER" : "TIE";
        System.out.println("결과: " + winner);

        if (bet <= 0) return 0; // 베팅 없으면 정산 X

        int delta = 0;
        switch (on) {
            case PLAYER -> {
                if (pv > bv) delta = +bet;          // 1:1
                else if (pv < bv) delta = -bet;
                else delta = 0;                     // 타이 → 푸시
            }
            case BANKER -> {
                if (bv > pv) {
                    // 1:1에서 5% 커미션
                    int win = bet;
                    int commission = (int)Math.round(win * 0.05); // 반올림
                    delta = win - commission;
                    System.out.println("뱅커 승: +"+win+" -커미션("+commission+") = +" + delta);
                } else if (bv < pv) {
                    delta = -bet;
                } else {
                    delta = 0; // 타이 푸시
                }
            }
            case TIE -> {
                if (pv == bv) delta = bet * 8;      // 8:1
                else delta = -bet;
            }
        }
        System.out.println("정산: " + (delta >= 0 ? "+" : "") + delta);
        return delta;
    }
}
