package shuffle.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Drew Fead
 */
public class Deck {
    private String name;
    private List<Card> cards = new ArrayList<>();

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Card> cards() { return cards;}
    public void setCards(List<Card> cards) { this.cards = cards; }
}
