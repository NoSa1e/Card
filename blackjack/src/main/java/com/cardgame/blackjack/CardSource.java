package com.cardgame.blackjack;

import java.util.concurrent.ThreadLocalRandom;

public interface CardSource {
    int draw(); // 한장 뽑기

    public class WeightedRanks implements CardSource {
        private static final int[] R = {2,3,4,5,6,7,8,9,10,10,10,10,11};
        public int draw() {
            return R[java.util.concurrent.ThreadLocalRandom.current().nextInt(R.length)];
        }
    }

}

