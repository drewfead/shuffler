package shuffle.core;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author Drew Fead
 */
public class Card {
    public static final Integer MIN = 1;
    public static final Integer MAX = 13;

    private static Map<Integer, String> valueNames = ImmutableMap.<Integer, String>builder()
        .put(MIN, "A")
        .put(2, "2")
        .put(3, "3")
        .put(4, "4")
        .put(5, "5")
        .put(6, "6")
        .put(7, "7")
        .put(8, "8")
        .put(9, "9")
        .put(10, "10")
        .put(11, "J")
        .put(12, "Q")
        .put(MAX, "K")
    .build();

    public enum Suit {
        SPADE, HEART, DIAMOND, CLUB;

        public String toString() {
            return name().toLowerCase();
        }
    }

    private Suit suit;
    private Integer value;

    public Card() {}

    public Card(Suit suit, Integer value) {
        this.suit = suit;
        this.value = value;
    }

    public Suit suit() { return suit; }
    public void setSuit(Suit suit) { this.suit = suit; }
    public Integer value() { return value; }
    public void setValue(Integer value) { this.value = value; }

    public String toString() {
        return String.format("%s-%s", valueNames.get(value), suit);
    }
}
