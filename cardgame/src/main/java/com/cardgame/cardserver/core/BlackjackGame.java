package com.cardgame.cardserver.core;
import java.util.*;
public class BlackjackGame {
    public static class Hand { public final List<Card> cards=new ArrayList<>(); public int total; public boolean done; }
    public static class State {
        public boolean inProgress;
        public int bet; public int delta; public int decks;
        public List<Hand> playerHands = new ArrayList<>(); public int activeIndex;
        public Hand dealer = new Hand();
    }
    private final Deck deck; private final State s=new State();
    public BlackjackGame(int decks){ this.deck=new Deck(decks); s.decks=decks; }
    private static int bestTotal(List<Card> cs){
        int sum=0, aces=0;
        for(Card c: cs){ sum += c.bjValue(); if(c.rank()== Card.Rank.ACE) aces++; }
        while(sum>21 && aces>0){ sum -= 10; aces--; }
        return sum;
    }
    private Hand newHand(){ Hand h=new Hand(); s.playerHands.add(h); return h; }
    public int currentBet(){ return s.bet; }
    public int handCount(){ return s.playerHands.size(); }
    public State start(int bet){
        s.inProgress=true; s.bet=bet; s.delta=0; s.playerHands.clear(); s.activeIndex=0;
        s.dealer=new Hand();
        Hand h1=newHand();
        h1.cards.add(deck.draw()); s.dealer.cards.add(deck.draw());
        h1.cards.add(deck.draw()); s.dealer.cards.add(deck.draw());
        h1.total = bestTotal(h1.cards); s.dealer.total = bestTotal(s.dealer.cards);
        return s;
    }
    public boolean canSplit(){
        if(!s.inProgress || s.playerHands.size()!=1) return false;
        Hand h=s.playerHands.get(0);
        if(h.cards.size()!=2) return false;
        int v1=h.cards.get(0).bjValue(), v2=h.cards.get(1).bjValue();
        return v1==v2;
    }
    public State split(){
        if(!canSplit()) return s;
        Hand h=s.playerHands.get(0);
        Hand hA=new Hand(); Hand hB=new Hand();
        hA.cards.add(h.cards.get(0)); hA.cards.add(deck.draw());
        hB.cards.add(h.cards.get(1)); hB.cards.add(deck.draw());
        hA.total=bestTotal(hA.cards); hB.total=bestTotal(hB.cards);
        s.playerHands.clear(); s.playerHands.add(hA); s.playerHands.add(hB);
        s.activeIndex=0;
        return s;
    }
    private void settleIfAllDone(){
        boolean all=true; for(Hand h: s.playerHands) if(!h.done) { all=false; break; }
        if(!all) return;
        while(s.dealer.total<17){ s.dealer.cards.add(deck.draw()); s.dealer.total=bestTotal(s.dealer.cards); }
        s.inProgress=false;
        int totalReturned=0;
        for(Hand h: s.playerHands){
            int returned=0;
            if(h.total>21){
                returned = 0;
            }else if(s.dealer.total>21 || h.total> s.dealer.total){
                returned = s.bet * 2;
            }else if(h.total == s.dealer.total){
                returned = s.bet;
            }
            totalReturned += returned;
        }
        s.delta=totalReturned;
    }
    public State hit(){
        if(!s.inProgress) return s;
        Hand h=s.playerHands.get(s.activeIndex);
        h.cards.add(deck.draw()); h.total=bestTotal(h.cards);
        if(h.total>21){ h.done=true; nextHand(); }
        return s;
    }
    public State stand(){
        if(!s.inProgress) return s;
        Hand h=s.playerHands.get(s.activeIndex); h.done=true; nextHand(); return s;
    }
    private void nextHand(){
        while(s.activeIndex < s.playerHands.size() && s.playerHands.get(s.activeIndex).done) s.activeIndex++;
        if(s.activeIndex>=s.playerHands.size()) settleIfAllDone();
    }
    public State dbl(){
        if(!s.inProgress) return s;
        s.bet *= 2;
        hit();
        if(s.inProgress) stand();
        return s;
    }
    public State surrender(){
        if(!s.inProgress) return s;
        s.inProgress=false; s.delta = -(s.bet/2); return s;
    }
}
