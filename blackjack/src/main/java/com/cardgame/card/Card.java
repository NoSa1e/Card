package com.cardgame.card;


public record Card(Suit suit, Rank rank) {
    public enum Suit {
        C('♣', false), D('♦', true), H('♥', true), S('♠', false);

        private final char symbol;
        private final boolean red;
        Suit(char symbol, boolean red) { this.symbol = symbol; this.red = red; }

        public char symbol() { return symbol; }
        public boolean isRed() { return red; }
    }
        public enum Rank {ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING}
    }

