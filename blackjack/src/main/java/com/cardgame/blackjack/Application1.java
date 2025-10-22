package com.cardgame.blackjack;

import java.util.ArrayList;
import java.util.*;
import java.util.Scanner;

public class Application1 {
    public final class HandUtils { // 패 관리 클래스
        private HandUtils() {}

        //카드를 한 장 추가한 '직후' 호출: 합이 21을 넘으면 A(=11)를 1로 낮춰 버스트 방지
        public static void adjustAcesIfBust(List<Integer> hand) {
            while (rawSum(hand) > 21) {
                int i = indexOfAce11(hand);
                if (i < 0) break;      // 더 낮출 A 없음
                hand.set(i, 1);        // 11 -> 1 로 '실제' 변경 (합에서 10 감소)
            }
        }

        // 현재 손패 합(변경 방식에서는 그냥 합이면 충분)
        public static int value(List<Integer> hand) {
            return rawSum(hand);
        }

        // 버스트 여부 (변경 방식에서는 보통 adjust 호출 직후 체크)
        public static boolean isBusted(List<Integer> hand) {
            return rawSum(hand) > 21;
        }

        // 에이스 변환 여부 : 여전히 11로 남아 있는 에이스가 있으면 소프트
        public static boolean isSoft(List<Integer> hand) {
            for (int v : hand) if (v == 11) return true;
            return false;
        }

        // ---- 내부 유틸 ----
        private static int rawSum(List<Integer> hand) {
            int s = 0;
            for (int v : hand) s += v;
            return s;
        }
        private static int indexOfAce11(List<Integer> hand) {
            for (int i = 0; i < hand.size(); i++) if (hand.get(i) == 11) return i;
            return -1;
        }
    }


    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        List<Integer> hand = new ArrayList<>(); // 유저 핸드
        List<Integer> dealer = new ArrayList<>(); // 딜러 핸드
        CardSource cs = new CardSource.WeightedRanks();
        hand.add(cs.draw());
        hand.add(cs.draw()); // 초기 2장 제공
        dealer.add(cs.draw());
        dealer.add(cs.draw()); // 딜러도 초기 2장 제공
        HandUtils.adjustAcesIfBust(dealer);
        System.out.println(hand);
        System.out.println("초기 손패: " + hand);
        HandUtils.adjustAcesIfBust(hand); // 초기 보정
        System.out.println("합 = " + HandUtils.value(hand));

        while (!HandUtils.isBusted(hand)) { // 유저 실행
            System.out.print("숫자로 입력해주세요 \n 1. 계속 받기 \n 2. 멈추기\n> ");
            int input = sc.nextInt();
            sc.nextLine(); //

            if (input == 1) {
                hand.add(cs.draw());
                HandUtils.adjustAcesIfBust(hand); // ★ 추가 직후 보정
                System.out.println("손패 : " + hand + " (합=" + HandUtils.value(hand) + ")");
                if (HandUtils.isBusted(hand)) {
                    System.out.println("버스트! 패배");
                    return;
                }
            } else if (input == 2) {
                System.out.println("스탠드. 최종 합 = " + HandUtils.value(hand));
                break;
            } else {
                System.out.println("1 또는 2만 입력해주세요.");
            }
        }

        while (HandUtils.value(dealer) < 17) { // 딜러 실행
            dealer.add(cs.draw());
            HandUtils.adjustAcesIfBust(dealer);
            if (HandUtils.isBusted(dealer)) {
                System.out.println("딜러 버스트! 승리");
                return;
            }
        }

                if (HandUtils.value(hand) > HandUtils.value(dealer)){
            System.out.println("딜러 패 :" + dealer + "\n딜러 패 합: "+HandUtils.value(dealer)+"\n유저 승리!");
        } else if (HandUtils.value(hand) == HandUtils.value(dealer)){
            if(hand.size() > dealer.size() && dealer.size() == 2 && HandUtils.value(hand) == 21 && HandUtils.value(dealer) == 21){
                System.out.println("딜러의 패가 자연 블랙잭으로 딜러가 승리하였습니다.");
            } else if(hand.size() < dealer.size() && hand.size() == 2 && HandUtils.value(hand) == 21 && HandUtils.value(dealer) == 21){
                System.out.println("유저의 패가 자연 블랙잭으로 유저가 승리하였습니다!");
            } else {
                System.out.println("무승부 입니다!");
            }
        } else {
            System.out.println("딜러 패 :\" + dealer + \"\\n딜러 패 합: \"+HandUtils.value(dealer)+\"\\n유저 패배");
        }
    }
}

