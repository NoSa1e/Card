package com.cardgame.app;

import com.cardgame.card.*;
import com.cardgame.blackjack.BlackJack;
import com.cardgame.sevenpoker.SevenPoker;
import com.cardgame.sevenpoker.SevenStudFixedLimit;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        var sc = new Scanner(System.in);

        int decks = askInt(sc, "덱 수 입력 (예: 2 또는 3) > ", 1, 8);
        Deck shoe = new Shoe(decks); // 필요하면 new Shoe(decks, 0.75)로 침투율 적용

        int initial = askInt(sc, "초기 자금(예: 1000) > ", 1, 1_000_000);
        Wallet wallet = new Wallet(initial);
        if (exitIfBankrupt(wallet)) return;

        outer:
        while (true) {
            System.out.println("\n== 게임 선택 ==");
            System.out.println("1) 블랙잭");
            System.out.println("2) 바카라");
            System.out.println("3) 세븐 포커(Seven-Card Stud)");
            System.out.println("0) 종료");
            int sel = askInt(sc, "> ", 0, 3);
            if (sel == 0) {
                System.out.println("종료합니다.");
                return;
            }

            switch (sel) {
                case 1 -> {
                    var game = new BlackJack();
                    while (true) {
                        System.out.println("\n[블랙잭] 잔액: " + wallet.balance());
                        if (exitIfBankrupt(wallet)) return;

                        int bet = askInt(sc, "베팅 금액(0: 메뉴로) > ", 0, wallet.balance());
                        if (bet == 0) continue outer;

                        // 베팅 포함 라운드 실행
                        int delta = game.playOneRoundForBet(sc, shoe, bet);
                        wallet.applyDelta(delta);
                        System.out.println("라운드 종료. 현재 잔액: " + wallet.balance());
                        if (exitIfBankrupt(wallet)) return;

                        // 계속 여부
                        int cont = askInt(sc, "계속(1) / 메뉴로(0) > ", 0, 1);
                        if (cont == 0) continue outer;
                    }
                }
                case 2 -> {
                    var game = new com.cardgame.baccarat.Baccarat();
                    while (true) {
                        System.out.println("\n[바카라] 잔액: " + wallet.balance());
                        if (exitIfBankrupt(wallet)) return;
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
                        if (exitIfBankrupt(wallet)) return;

                        int cont = askInt(sc, "계속(1) / 메뉴로(0) > ", 0, 1);
                        if (cont == 0) continue outer;
                    }

                }
                case 3 -> {
                    var game = new SevenStudFixedLimit();
                    while (true) {
                        System.out.println("\n잔액: " + wallet.balance());
                        if (exitIfBankrupt(wallet)) return;

                        // (1) Ante는 잔액의 10% 이내로 강제
                        int maxAnte = Math.max(1, wallet.balance() / 10);
                        int anteReq = askInt(sc, "기본 베팅금 (최대 " + maxAnte + ") > ", 1, maxAnte);

                        // (2) 플레이어가 이 핸드에서 추가로 투자할 수 있는 최대치(=현재 잔액)
                        int stackForHand = wallet.balance();

                        // (3) 라운드 실행: ante + 올인 고려 & 레이즈캡 3
                        int delta = game.playOneRoundForBet(sc, shoe, anteReq, stackForHand);
                        wallet.applyDelta(delta);
                        System.out.println("라운드 종료. 현재 잔액: " + wallet.balance());
                        if (exitIfBankrupt(wallet)) return;

                        int cont = askInt(sc, "계속(1) / 메뉴로(0) > ", 0, 1);
                        if (cont == 0) continue outer;
                    }
                }



                default -> {
                }
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


    private static boolean exitIfBankrupt(Wallet wallet) {
        if (wallet.balance() <= 0) {
            System.out.println("\n잔액이 0원입니다. 프로그램을 종료합니다.");
            return true;
        }
        return false;
    }
}