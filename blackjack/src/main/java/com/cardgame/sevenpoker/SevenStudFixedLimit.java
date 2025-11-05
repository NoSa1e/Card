package com.cardgame.sevenpoker;

import com.cardgame.card.*;
import com.cardgame.card.Game;
import com.cardgame.sevenpoker.PokerEval.*;

import java.util.*;
import static com.cardgame.card.CardStrings.*;

public class SevenStudFixedLimit implements Game {

    private static final class StreetPaid {
        final int player, dealer;
        final boolean playerFolded, playerAllIn;
        StreetPaid(int player, int dealer, boolean playerFolded, boolean playerAllIn){
            this.player = player; this.dealer = dealer;
            this.playerFolded = playerFolded; this.playerAllIn = playerAllIn;
        }
    }

    @Override
    public void playOneRound(Scanner in, Deck deck) { playOneRoundForBet(in, deck, 0, Integer.MAX_VALUE); }

    /** ante: 기본 베팅(잔액의 10% 이내로 Main에서 보정)
     *  stackForHand: 이 핸드에서 추가로 투자 가능한 총액(현재 잔액)
     */
    public int playOneRoundForBet(Scanner in, Deck deck, int ante, int stackForHand) {
        final int small = Math.max(ante, 1);
        final int big   = Math.max(small * 2, small + 1);

        List<Card> pDown = new ArrayList<>(3), pUp = new ArrayList<>(4);
        List<Card> dDown = new ArrayList<>(3), dUp = new ArrayList<>(4);

        // 초기 배분: 2down+1up
        pDown.add(deck.draw()); dDown.add(deck.draw());
        pDown.add(deck.draw()); dDown.add(deck.draw());
        pUp.add(deck.draw());   dUp.add(deck.draw());

        int pot = 0, delta = 0;
        boolean playerAllInEver = false;
        int totalP = 0, totalD = 0; // 이 핸드에서 각자 납입 총액

        // Ante 납부(플레이어만, 스택 차감)
        int maxPay = Math.min(ante, stackForHand);
        pot += maxPay; delta -= maxPay; totalP += maxPay;
        stackForHand -= maxPay;
        if (maxPay < ante) {
            System.out.println("※ 잔액 부족으로 Ante는 올인 처리(" + maxPay + ")");
            playerAllInEver = true;
        }

        banner("Seven-Card Stud (Fixed-Limit)");
        printState(pDown, pUp, dDown, dUp);
        printPotAndBet(pot, small);

        // 3rd
        int[] stackRef = new int[]{ stackForHand };
        StreetPaid r3 = bettingRound(in, "3rd", small, pUp, dUp, stackRef);
        pot += r3.player + r3.dealer; delta -= r3.player; totalP += r3.player; totalD += r3.dealer;
        playerAllInEver |= r3.playerAllIn;
        System.out.printf("[정산] 3rd: P=%+d, D=%+d → Pot=%,d%n", r3.player, r3.dealer, pot);
        if (r3.playerFolded) { System.out.println("플레이어 폴드"); return delta; }

        // 4th
        pUp.add(deck.draw()); dUp.add(deck.draw());
        banner("4th Street - 카드 배분");
        printState(pDown, pUp, dDown, dUp);
        printPotAndBet(pot, small);
        StreetPaid r4 = bettingRound(in, "4th", small, pUp, dUp, stackRef);
        pot += r4.player + r4.dealer; delta -= r4.player; totalP += r4.player; totalD += r4.dealer;
        playerAllInEver |= r4.playerAllIn;
        System.out.printf("[정산] 4th: P=%+d, D=%+d → Pot=%,d%n", r4.player, r4.dealer, pot);
        if (r4.playerFolded) { System.out.println("플레이어 폴드"); return delta; }

        // 5th
        pUp.add(deck.draw()); dUp.add(deck.draw());
        banner("5th Street - 카드 배분");
        printState(pDown, pUp, dDown, dUp);
        printPotAndBet(pot, big);
        StreetPaid r5 = bettingRound(in, "5th", big, pUp, dUp, stackRef);
        pot += r5.player + r5.dealer; delta -= r5.player; totalP += r5.player; totalD += r5.dealer;
        playerAllInEver |= r5.playerAllIn;
        System.out.printf("[정산] 5th: P=%+d, D=%+d → Pot=%,d%n", r5.player, r5.dealer, pot);
        if (r5.playerFolded) { System.out.println("플레이어 폴드"); return delta; }

        // 6th
        pUp.add(deck.draw()); dUp.add(deck.draw());
        banner("6th Street - 카드 배분");
        printState(pDown, pUp, dDown, dUp);
        printPotAndBet(pot, big);
        StreetPaid r6 = bettingRound(in, "6th", big, pUp, dUp, stackRef);
        pot += r6.player + r6.dealer; delta -= r6.player; totalP += r6.player; totalD += r6.dealer;
        playerAllInEver |= r6.playerAllIn;
        System.out.printf("[정산] 6th: P=%+d, D=%+d → Pot=%,d%n", r6.player, r6.dealer, pot);
        if (r6.playerFolded) { System.out.println("플레이어 폴드"); return delta; }

        // 7th
        pDown.add(deck.draw()); dDown.add(deck.draw());
        banner("7th Street - 다운카드 배분");
        System.out.println("다운카드가 배분되었습니다. (딜러 다운은 가림)");
        printState(pDown, pUp, dDown, dUp);
        printPotAndBet(pot, big);
        StreetPaid r7 = bettingRound(in, "7th", big, pUp, dUp, stackRef);
        pot += r7.player + r7.dealer; delta -= r7.player; totalP += r7.player; totalD += r7.dealer;
        playerAllInEver |= r7.playerAllIn;
        System.out.printf("[정산] 7th: P=%+d, D=%+d → Pot=%,d%n", r7.player, r7.dealer, pot);
        if (r7.playerFolded) { System.out.println("플레이어 폴드"); return delta; }

        // 쇼다운
        List<Card> pAll = new ArrayList<>(pDown); pAll.addAll(pUp);
        List<Card> dAll = new ArrayList<>(dDown); dAll.addAll(dUp);

        banner("쇼다운 (딜러 히든 3장 공개)");
        System.out.println("플레이어 전체: " + pAll.stream().map(c -> card(c, true)).toList());
        System.out.println("딜러    전체: " + dAll.stream().map(c -> card(c, true)).toList());

        HandScore ps = PokerEval.evaluate(best5(pAll));
        HandScore ds = PokerEval.evaluate(best5(dAll));
        System.out.println("플레이어 족보: " + ps);
        System.out.println("딜러   족보 : " + ds);

        int cmp = ps.compareTo(ds);
        if (cmp > 0) {
            // 올인 승리 시에도 사이드팟 없이 매칭만 했으므로 pot 전액이 곧 매칭 금액.
            System.out.println("플레이어 승 → 팟 획득 +" + pot);
            delta += pot;
        } else if (cmp < 0) {
            System.out.println("딜러 승");
        } else {
            System.out.println("무승부 → 팟 반분");
            delta += pot / 2;
        }
        return delta;
    }

    /* ----------------- 레이즈 캡 3 + 올인 처리 ----------------- */
    private StreetPaid bettingRound(Scanner in, String street, int betSize,
                                    List<Card> pUp, List<Card> dUp, int[] stackRef) {
        int payP = 0, payD = 0;
        int toCall = 0;
        int raises = 0;                   // 스트리트당 총 레이즈 수(플레이어/딜러 합산)
        boolean bettingOpen = true;
        boolean playerAllIn = false;

        System.out.printf("%n[%s] 베팅 사이즈 %,d — 당신 선행%n", street, betSize);
        System.out.printf("업카드 요약: 플레이어 Max=%d vs 딜러 Max=%d%n",
                upMaxRank(pUp), upMaxRank(dUp));

        // --- 플레이어 선행 ---
        if (toCall == 0 && bettingOpen) {
            int choice = ask(in,
                    (raises < 3 ? "1) 체크  2) 베트(" + betSize + ")" : "1) 체크") + "  3) 폴드 > ",
                    1, 3);
            if (choice == 1) {
                // 체크
            } else if (choice == 2 && raises < 3) {
                int need = betSize;
                int pay = Math.min(need, stackRef[0]);
                payP += pay; stackRef[0] -= pay;
                toCall = pay; bettingOpen = false; // 상대가 동일 금액 콜 필요
                if (pay < need) { // 올인
                    playerAllIn = true;
                } else {
                    raises++; // 새로 베팅(=첫 레이즈로 간주)
                }
            } else if (choice == 3) {
                return new StreetPaid(payP, payD, true, playerAllIn);
            }
        } else {
            // (이 분기는 이번 구조에선 도달 X)
        }

        // --- 딜러 응답 ---
        if (toCall == 0 && bettingOpen) {
            // 플레이어가 체크했으므로 딜러 액션
            boolean canBet = (raises < 3);
            boolean bet = canBet && ((upMaxRank(dUp) >= upMaxRank(pUp)) || Math.random() < 0.15);
            if (bet) {
                int need = betSize;
                System.out.println("딜러: 베트(" + need + ")");
                payD += need;
                toCall = need; bettingOpen = false; raises++;
            } else {
                System.out.println("딜러: 체크");
                return new StreetPaid(payP, payD, false, playerAllIn);
            }
        } else {
            // 플레이어가 베팅(또는 올인) → 딜러가 대응
            if (toCall > 0) {
                // 플레이어가 올인이면 딜러는 그 금액까지만 콜 가능(추가 레이즈 금지)
                boolean allowRaise = (!playerAllIn && raises < 3);
                int dChoice;
                if (allowRaise) {
                    dChoice = (upMaxRank(dUp) + (Math.random()<0.1?1:0) >= upMaxRank(pUp))
                            ? (Math.random() < 0.2 ? 2 : 1) // 20% 레이즈, 아니면 콜
                            : 1; // 보수적으로 콜
                } else {
                    dChoice = 1; // 콜만
                }

                if (dChoice == 1) { // 콜
                    System.out.println("딜러: 콜(" + toCall + ")");
                    payD += toCall; toCall = 0;
                } else { // 레이즈 (플레이어가 올인이 아닌 경우에만)
                    int need = toCall + betSize;
                    if (raises < 3) {
                        System.out.println("딜러: 레이즈(" + need + ")");
                        payD += need; toCall = betSize; raises++;
                    } else {
                        // 이론상 오지 않음(allowRaise로 걸렀음)
                        System.out.println("딜러: (캡) 콜(" + toCall + ")");
                        payD += toCall; toCall = 0;
                    }
                }
            }
        }

        // --- 플레이어 마무리(딜러가 베팅/레이즈했다면) ---
        if (toCall > 0) {
            // 플레이어 잔액 한도 내에서 콜/리레이즈/폴드
            boolean canReRaise = (!playerAllIn && raises < 3);
            String menu = canReRaise
                    ? "1) 콜(" + toCall + ")  2) 리레이즈(" + (toCall + betSize) + ")  3) 폴드 > "
                    : "1) 콜(" + toCall + ")  3) 폴드 > ";
            int fin = ask(in, menu, 1, 3);

            if (fin == 1) { // 콜 (올인 보정)
                int need = toCall;
                int pay = Math.min(need, stackRef[0]);
                payP += pay; stackRef[0] -= pay; toCall -= pay;
                if (pay < need) { playerAllIn = true; toCall = 0; }
            } else if (fin == 2 && canReRaise) { // 리레이즈 (올인 보정)
                int need = toCall + betSize;
                int pay = Math.min(need, stackRef[0]);
                payP += pay; stackRef[0] -= pay;
                if (pay < need) { // 리레이즈 올인 → 딜러는 콜/폴드만
                    playerAllIn = true; toCall = pay - (need - betSize); // 실질적 추가분
                    if (toCall < 0) toCall = 0;
                } else {
                    toCall = betSize; raises++;
                }

                // 딜러 최종 응답(레이즈 캡 또는 플레이어 올인으로 추가 레이즈 불가)
                if (toCall > 0) {
                    System.out.println("딜러: 최종 콜(" + toCall + ")");
                    payD += toCall; toCall = 0;
                }
            } else {
                return new StreetPaid(payP, payD, true, playerAllIn);
            }
        }

        return new StreetPaid(payP, payD, false, playerAllIn);
    }

    /* ----------------- 표시/보조 ----------------- */
    private void banner(String title) { System.out.println("\n==== " + title + " ===="); }

    private void printPotAndBet(int pot, int betSize) {
        System.out.printf("Pot: %,d   BetSize: %,d%n", pot, betSize);
    }

    private void printState(List<Card> pDown, List<Card> pUp,
                            List<Card> dDown, List<Card> dUp) {
        List<String> pAll = new ArrayList<>(pDown.size() + pUp.size());
        for (Card c : pDown) pAll.add(card(c, true));
        for (Card c : pUp)   pAll.add(card(c, true));
        System.out.println("플레이어: " + pAll);

        String dealerLine = "[" + maskList(dDown.size())
                + (dUp.isEmpty() ? "]" : (dDown.isEmpty() ? "" : ", ") + upList(dUp) + "]");
        System.out.println("딜러    : " + dealerLine);
    }

    private String upList(List<Card> ups) {
        List<String> s = new ArrayList<>(ups.size());
        for (Card c : ups) s.add(card(c, true));
        return String.join(", ", s);
    }
    private String maskList(int n) {
        if (n <= 0) return "";
        String[] arr = new String[n]; Arrays.fill(arr, "□");
        return String.join(", ", arr);
    }

    private int upMaxRank(List<Card> up) {
        int best = 0;
        for (Card c : up) {
            int v = switch (c.rank()) {
                case ACE -> 14;
                case KING -> 13; case QUEEN -> 12; case JACK -> 11; case TEN -> 10;
                case NINE -> 9; case EIGHT -> 8; case SEVEN -> 7; case SIX -> 6; case FIVE -> 5;
                case FOUR -> 4; case THREE -> 3; case TWO -> 2;
            };
            best = Math.max(best, v);
        }
        return best;
    }

    // 7장 중 최적 5장 (리스트 반환)
    private List<Card> best5(List<Card> seven) {
        if (seven.size() != 7) throw new IllegalArgumentException("need 7 cards");
        List<Card> best = null;
        for (int a = 0; a < 3; a++)
            for (int b = a+1; b < 4; b++)
                for (int c = b+1; c < 5; c++)
                    for (int d = c+1; d < 6; d++)
                        for (int e = d+1; e < 7; e++) {
                            var five = List.of(seven.get(a), seven.get(b), seven.get(c), seven.get(d), seven.get(e));
                            var ps = PokerEval.evaluate(five);
                            if (best == null) { best = new ArrayList<>(five); }
                            else {
                                var bs = PokerEval.evaluate(best);
                                if (ps.compareTo(bs) > 0) best = new ArrayList<>(five);
                            }
                        }
        return best;
    }

    private int ask(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                System.out.println("숫자 " + min + "~" + max + " 범위로 다시 입력");
            }
        }
    }
}
