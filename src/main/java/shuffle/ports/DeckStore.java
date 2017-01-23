package shuffle.ports;

import shuffle.core.Deck;

import java.util.List;
import java.util.Optional;

/**
 * @author Drew Fead
 */
public interface DeckStore {
    Optional<Deck> upsert(Deck deck);
    Optional<Deck> findOne(String name);
    List<Deck> findPage(Integer pageSize, Integer offset);
    void delete(String name);
}
