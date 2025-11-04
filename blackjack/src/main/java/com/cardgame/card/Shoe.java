package com.cardgame.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// com.cardgame.card.Shoe
public class Shoe implements Deck {
    private final List<Card> cards = new ArrayList<>();
    private final int decks;
    private final Random rnd = new Random();

    private final int total;            // 총 장수 = 52 * decks
    private final double penetration;   // 0.0~1.0 (예: 0.75면 75% 소모 후 셔플)

    public Shoe(int decks) {
        this(decks, 1.0); // 기존 생성자 유지: "다 쓸 때"만 셔플
    }

    public Shoe(int decks, double penetration) {
        if (decks < 1) throw new IllegalArgumentException("decks must be >= 1");
        if (penetration <= 0.0 || penetration > 1.0)
            throw new IllegalArgumentException("penetration must be in (0,1]");
        this.decks = decks;
        this.penetration = penetration;
        this.total = 52 * decks;
        reshuffle();
    }

    private void reshuffle() {
        cards.clear();
        for (int d = 0; d < decks; d++) {
            for (Card.Suit s : Card.Suit.values()) {
                for (Card.Rank r : Card.Rank.values()) {
                    cards.add(new Card(s, r));
                }
            }
        }
        Collections.shuffle(cards, rnd);
    }

    private boolean needReshuffle() {
        if (penetration >= 1.0) return cards.isEmpty(); // 기존 동작과 동일
        int cut = (int)Math.round(total * (1.0 - penetration)); // 남겨둘 컷
        return cards.size() <= cut;
    }

    @Override public Card draw() {
        if (needReshuffle()) reshuffle();
        return cards.remove(cards.size() - 1);
    }

    @Override public boolean isEmpty() { return cards.isEmpty(); }
    @Override public int remaining()   { return cards.size(); }
}
