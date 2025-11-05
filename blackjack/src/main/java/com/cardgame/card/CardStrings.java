package com.cardgame.card;

public final class CardStrings {
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private CardStrings() {}

    /** 랭크를 A,2..10,J,Q,K 로 */
    public static String rankSymbol(Card.Rank r) {
        return switch (r) {
            case ACE -> "A";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
            default -> String.valueOf(r.ordinal() + 1); // TWO=1 → +1 = 2
        };
    }

    /** 카드 하나를 "A♠" 처럼 반환. color=true면 ♥♦ 는 빨간색 ANSI 적용 */
    public static String card(Card c, boolean color) {
        String base = rankSymbol(c.rank()) + c.suit().symbol();
        if (color && c.suit().isRed()) return RED + base + RESET;
        return base;
    }

    /** 핸드를 "[A♠, 10♥, 7♦]" 형태로 */
    public static String hand(java.util.List<Card> h, boolean color) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < h.size(); i++) {
            sb.append(card(h.get(i), color));
            if (i < h.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    public static String show(Card c) {
        return card(c, true);   // 컬러 출력 기본
    }
    // 필요하면 컬러 off 버전도
    public static String showMono(Card c) {
        return card(c, false);
    }
}


