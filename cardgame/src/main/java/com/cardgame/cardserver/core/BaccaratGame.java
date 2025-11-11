package com.cardgame.cardserver.core;
import java.util.*;
public class BaccaratGame {
    public static class Side { public final List<Card> cards=new ArrayList<>(); public int total; }
    public static class State { public boolean inProgress; public Side player=new Side(); public Side banker=new Side(); public int delta; }
    private final Deck deck;
    public BaccaratGame(int decks){ this.deck=new Deck(decks); }
    private static int point(List<Card> cs){ int t=0; for(Card c:cs) t+=c.pip(); return t%10; }
    public State bet(String main, boolean pp, boolean pb, boolean super6, int amount, boolean commission){
        State s=new State(); s.inProgress=true;
        s.player.cards.add(deck.draw()); s.banker.cards.add(deck.draw());
        s.player.cards.add(deck.draw()); s.banker.cards.add(deck.draw());
        s.player.total=point(s.player.cards); s.banker.total=point(s.banker.cards);
        if(s.player.total<=5) { s.player.cards.add(deck.draw()); s.player.total=point(s.player.cards); }
        if(s.banker.total<=5) { s.banker.cards.add(deck.draw()); s.banker.total=point(s.banker.cards); }
        s.inProgress=false;
        String winner = s.player.total==s.banker.total ? "TIE" : (s.player.total > s.banker.total ? "PLAYER" : "BANKER");
        int settle=0;
        if(main!=null){
            switch(main.toUpperCase()){
                case "PLAYER" -> {
                    if("PLAYER".equals(winner)){
                        settle += amount * 2;
                    }else if("TIE".equals(winner)){
                        settle += amount;
                    }
                }
                case "BANKER" -> {
                    if("BANKER".equals(winner)){
                        int profit = commission ? (int)Math.round(amount*0.95) : amount;
                        settle += amount + profit;
                    }else if("TIE".equals(winner)){
                        settle += amount;
                    }
                }
                case "TIE" -> {
                    if("TIE".equals(winner)){
                        settle += amount * 9;
                    }
                }
            }
        }
        boolean pPair = s.player.cards.size()>=2 && s.player.cards.get(0).rank()==s.player.cards.get(1).rank();
        boolean bPair = s.banker.cards.size()>=2 && s.banker.cards.get(0).rank()==s.banker.cards.get(1).rank();
        if(pp && pPair) settle += amount*12;
        if(pb && bPair) settle += amount*12;
        if(super6 && "BANKER".equals(winner) && s.banker.total==6) settle += amount*13;
        s.delta=settle; return s;
    }
}
