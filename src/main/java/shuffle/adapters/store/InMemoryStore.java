package shuffle.adapters.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shuffle.core.Deck;
import shuffle.ports.DeckStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Drew Fead
 */
public class InMemoryStore implements DeckStore {
    private static final Logger log = LoggerFactory.getLogger(InMemoryStore.class);

    private Map<String, Deck> cache = new HashMap<>();

    public Optional<Deck> upsert(Deck deck) {
        if(log.isInfoEnabled()) { log.info(String.format("upserting deck: %s", deck.name())); }

        cache.put(deck.name(), deck);
        return Optional.of(deck);
    }

    public Optional<Deck> findOne(String name) {
        if(log.isInfoEnabled()) { log.info(String.format("finding deck: %s", name)); }

        return Optional.ofNullable(cache.get(name));
    }

    public List<Deck> findPage(Integer pageSize, Integer offset) {
        if(log.isInfoEnabled()) { log.info(String.format("finding %s decks from offset: %s", pageSize, offset)); }

        return cache.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue)
            .skip(offset)
            .limit(pageSize)
            .collect(Collectors.toList());
    }

    public void delete(String name) {
        if(log.isInfoEnabled()) { log.info(String.format("deleting deck: %s", name)); }

        cache.remove(name);
    }
}
