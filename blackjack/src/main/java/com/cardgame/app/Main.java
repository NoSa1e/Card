// com/cardgame/app/Main.java
package com.cardgame.app;

import com.cardgame.card.*;
import com.cardgame.blackjack.BlackJack;
// import com.cardgame.baccarat.Baccarat;
// import com.cardgame.indianpoker.IndianPoker;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        var sc = new Scanner(System.in);

        int decks = askInt(sc, "덱 수 입력 (예: 2 또는 3) > ", 1, 8);
        Deck shoe = new Shoe(decks); // 필요하면 new Shoe(decks, 0.75)로 침투율 적용

        int initial = askInt(sc, "초기 자금(예: 1000) > ", 1, 1_000_000);
        Wallet wallet = new Wallet(initial);

        outer:
        while (true) {
            System.out.println("\n== 게임 선택 ==");
            System.out.println("1) 블랙잭");
            System.out.println("2) 바카라 (추가 예정)");
            System.out.println("3) 인디언포커 (추가 예정)");
            System.out.println("0) 종료");
            int sel = askInt(sc, "> ", 0, 3);
            if (sel == 0) { System.out.println("종료합니다."); return; }

            switch (sel) {
                case 1 -> {
                    var game = new BlackJack();
                    while (true) {
                        System.out.println("\n[블랙잭] 잔액: " + wallet.balance());
                        if (wallet.balance() <= 0) {
                            System.out.println("잔액이 0원입니다. 메뉴로 돌아갑니다.");
                            continue outer;
                        }
                        int bet = askInt(sc, "베팅 금액(0: 메뉴로) > ", 0, wallet.balance());
                        if (bet == 0) continue outer;

                        // 베팅 포함 라운드 실행
                        int delta = game.playOneRoundForBet(sc, shoe, bet);
                        wallet.applyDelta(delta);
                        System.out.println("라운드 종료. 현재 잔액: " + wallet.balance());

                        // 계속 여부
                        int cont = askInt(sc, "계속(1) / 메뉴로(0) > ", 0, 1);
                        if (cont == 0) continue outer;
                    }
                }
                case 2 -> {
                    var game = new com.cardgame.baccarat.Baccarat();
                    while (true) {
                        System.out.println("\n[바카라] 잔액: " + wallet.balance());
                        if (wallet.balance() <= 0) {
                            System.out.println("잔액이 0원입니다. 메뉴로 돌아갑니다.");
                            continue outer;
                        }
                        // 베팅 타깃 선택
                        System.out.println("베팅 타깃: 1) PLAYER  2) BANKER  3) TIE  (0: 메뉴로)");
                        int t = askInt(sc, "> ", 0, 3);
                        if (t == 0) continue outer;
                        var target = switch (t) {
                            case 1 -> com.cardgame.baccarat.Baccarat.Bet.PLAYER;
                            case 2 -> com.cardgame.baccarat.Baccarat.Bet.BANKER;
                            case 3 -> com.cardgame.baccarat.Baccarat.Bet.TIE;
                            default -> com.cardgame.baccarat.Baccarat.Bet.PLAYER;
                        };

                        int bet = askInt(sc, "베팅 금액(0: 메뉴로) > ", 0, wallet.balance());
                        if (bet == 0) continue outer;

                        int delta = game.playOneRoundForBet(sc, shoe, bet, target);
                        wallet.applyDelta(delta);
                        System.out.println("라운드 종료. 현재 잔액: " + wallet.balance());

                        int cont = askInt(sc, "계속(1) / 메뉴로(0) > ", 0, 1);
                        if (cont == 0) continue outer;
                    }

                }
                case 3 -> {
                    System.out.println("인디언포커는 베팅 연동을 곧 추가할 예정입니다.");
                }
                default -> {}
            }
        }
    }

    private static int askInt(Scanner sc, String prompt, int min, int max) {
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
