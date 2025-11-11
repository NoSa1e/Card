package com.cardgame.cardserver.core;

import java.util.*;

public class BaccaratRoom {
    public static class Side { public final List<Card> cards=new ArrayList<>(); public int total; }
    public static class Round {
        public boolean inProgress;
        public Side player=new Side(); public Side banker=new Side();
        public Map<String, Map<String,Integer>> ledger = new LinkedHashMap<>(); // user -> {MAIN:amount, PP:0/amount, BP:..., S6:...}
        public Map<String,Integer> settle = new LinkedHashMap<>(); // user -> delta
        public boolean settlementApplied;
    }
    private final Deck deck;
    public final Round r = new Round();

    public BaccaratRoom(int decks){ this.deck = new Deck(Math.max(1,decks)); }

    private static int point(List<Card> cs){ int t=0; for(Card c: cs) t+=c.pip(); return t%10; }

    public void resetRound(){
        r.inProgress = false;
        r.player = new Side();
        r.banker = new Side();
        r.settle.clear();
        r.ledger.clear();
        r.settlementApplied = false;
    }

    public void place(String user, String main, int amount, boolean pp, boolean bp, boolean s6){
        r.ledger.putIfAbsent(user, new LinkedHashMap<>());
        var m = r.ledger.get(user);
        m.put("MAIN_"+main.toUpperCase(), m.getOrDefault("MAIN_"+main.toUpperCase(),0)+amount);
        if(pp) m.put("PAIR_P", m.getOrDefault("PAIR_P",0)+amount);
        if(bp) m.put("PAIR_B", m.getOrDefault("PAIR_B",0)+amount);
        if(s6) m.put("SUPER6", m.getOrDefault("SUPER6",0)+amount);
    }

    private static int settleMain(String pick, String winner, int amount, boolean commission){
        if("PLAYER".equals(pick)){
            if("PLAYER".equals(winner)) return amount * 2;
            if("TIE".equals(winner)) return amount;
            return 0;
        }
        if("BANKER".equals(pick)){
            if("BANKER".equals(winner)){
                int profit = commission ? (int)Math.round(amount * 0.95) : amount;
                return amount + profit;
            }
            if("TIE".equals(winner)) return amount;
            return 0;
        }
        if("TIE".equals(pick)){
            return "TIE".equals(winner) ? amount * 9 : 0;
        }
        return 0;
    }

    public void dealAndSettle(boolean commission){
        r.inProgress = true; r.player = new Side(); r.banker = new Side(); r.settle.clear(); r.settlementApplied = false;
        r.player.cards.add(deck.draw()); r.banker.cards.add(deck.draw());
        r.player.cards.add(deck.draw()); r.banker.cards.add(deck.draw());
        r.player.total = point(r.player.cards); r.banker.total = point(r.banker.cards);
        if(r.player.total<=5){ r.player.cards.add(deck.draw()); r.player.total = point(r.player.cards); }
        if(r.banker.total<=5){ r.banker.cards.add(deck.draw()); r.banker.total = point(r.banker.cards); }
        String winner = r.player.total==r.banker.total ? "TIE" : (r.player.total>r.banker.total?"PLAYER":"BANKER");
        boolean pPair = r.player.cards.size()>=2 && r.player.cards.get(0).rank()==r.player.cards.get(1).rank();
        boolean bPair = r.banker.cards.size()>=2 && r.banker.cards.get(0).rank()==r.banker.cards.get(1).rank();

        for(var e: r.ledger.entrySet()){
            String user = e.getKey(); var bet = e.getValue();
            int delta=0;
            for(var b: bet.entrySet()){
                String k=b.getKey(); int amt=b.getValue();
                if(k.startsWith("MAIN_")){
                    String pick = k.substring("MAIN_".length());
                    delta += settleMain(pick, winner, amt, commission);
                }else if("PAIR_P".equals(k) && pPair){ delta += amt*12; }
                else if("PAIR_B".equals(k) && bPair){ delta += amt*12; }
                else if("SUPER6".equals(k) && "BANKER".equals(winner) && r.banker.total==6){ delta += amt*13; }
            }
            r.settle.put(user, delta);
        }
        r.inProgress=false;
    }

    public boolean ready(Collection<String> users){
        if(users==null || users.isEmpty()) return false;
        for(String user: users){
            if(user==null) continue;
            var m = r.ledger.get(user);
            if(m==null || m.isEmpty()) return false;
            boolean hasMain=false;
            for(var e: m.entrySet()){
                if(e.getKey().startsWith("MAIN_") && e.getValue()>0){ hasMain=true; break; }
            }
            if(!hasMain) return false;
        }
        return true;
    }
}
