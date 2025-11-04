package com.cardgame.app;

public class Wallet {
    private int balance;
    public Wallet(int initial) {
        if (initial < 0) throw new IllegalArgumentException("초기 잔액은 0 이상");
        this.balance = initial;
    }
    public int balance() { return balance; }

    /* 베팅 가능 여부 체크 */
    public boolean canBet(int amount) { return amount > 0 && amount <= balance; }

    /* 라운드가 끝난 후 정산을 적용 */
    public void applyDelta(int delta) { balance += delta; }
}
