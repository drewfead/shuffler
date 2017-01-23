package unit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import shuffle.adapters.store.InMemoryStore;
import shuffle.core.Card;
import shuffle.core.Deck;
import shuffle.core.DeckHandlers;
import shuffle.ports.ActionHandler;
import shuffle.ports.ActionHandler.Result;
import shuffle.ports.ActionHandler.Status;
import shuffle.ports.DeckStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/*
 * @author Drew Fead
 */
public class DeckTest {
    ObjectMapper mapper = new ObjectMapper();
    DeckStore deckStore;
    DeckHandlers handlers;

    public static void assertDeckOk(Deck deck) {
        assertNotNull("name is a required field", deck.name());
        assertEquals("should have 52 cards", 52, deck.cards().size());
        deck.cards().stream()
            .collect(Collectors.groupingBy(Card::suit))
            .entrySet()
            .forEach(e ->
                assertEquals("should have 13 " + e.getKey(), 13, e.getValue().size())
            );
        deck.cards().stream()
            .collect(Collectors.groupingBy(Card::value))
            .entrySet()
            .forEach(e ->
                assertEquals("should have 4 " + e.getKey() + "'s", 4, e.getValue().size())
            );
    }

    @Before public void init() {
        deckStore = new InMemoryStore();
        handlers = new DeckHandlers(deckStore, 2);
    }

    @Test public void create() throws Exception { // put
        final String name = RandomStringUtils.randomAlphanumeric(10, 100); // tune the max here to spec
        final Result result = handlers.CREATE.handle("test-create", name.getBytes());

        final Deck deck = mapper.readValue(result.payload(), Deck.class);
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(name, deck.name());
        assertDeckOk(deck);

        final Deck persisted = deckStore.findOne(name).orElse(null);
        assertNotNull(persisted);
        assertEquals(name, persisted.name());
        assertDeckOk(persisted);

        for(int i = 0; i < 52; i++) {
            final Card a = deck.cards().get(i);
            final Card b = persisted.cards().get(i);

            assertEquals(a.suit(), b.suit());
            assertEquals(a.value(), b.value());
        }
    }

    @Test public void shuffle() throws Exception { // post
        final String name = "shuffle-test";
        final Deck old = mapper.readValue(
            handlers.CREATE.handle("pre-insert-shuffle", name.getBytes()).payload(),
            Deck.class);

        final DeckHandlers.ShuffleRequest req = new DeckHandlers.ShuffleRequest();
        req.name = name;

        final byte[] json = mapper.writeValueAsBytes(req);

        final Result result = handlers.SHUFFLE.handle("test-shuffle", json);

        final Deck deck = mapper.readValue(result.payload(), Deck.class);
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(name, deck.name());
        assertDeckOk(deck);

        final Deck persisted = deckStore.findOne(name).orElse(null);
        assertNotNull(persisted);
        assertEquals(name, persisted.name());
        assertDeckOk(persisted);

        boolean foundDifference = false;
        for(int i = 0; i < 52; i++) {
            final Card a = deck.cards().get(i);
            final Card b = persisted.cards().get(i);
            final Card c = old.cards().get(i);

            assertEquals(a.suit(), b.suit());
            assertEquals(a.value(), b.value());
            if(c != a) {
                foundDifference = true;
            }
        }

        assertTrue("deck was not shuffled", foundDifference);
    }

    @Test public void randomize() throws Exception { // post
        final String name = "randomize-test";
        final Deck old = mapper.readValue(
                handlers.CREATE.handle("pre-insert-randomize", name.getBytes()).payload(),
                Deck.class);

        final DeckHandlers.ShuffleRequest req = new DeckHandlers.ShuffleRequest();
        req.name = name;

        final byte[] json = mapper.writeValueAsBytes(req);

        final Result result = handlers.RANDOMIZE.handle("test-randomize", json);

        final Deck deck = mapper.readValue(result.payload(), Deck.class);
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(name, deck.name());
        assertDeckOk(deck);

        final Deck persisted = deckStore.findOne(name).orElse(null);
        assertNotNull(persisted);
        assertEquals(name, persisted.name());
        assertDeckOk(persisted);

        boolean foundDifference = false;
        for(int i = 0; i < 52; i++) {
            final Card a = deck.cards().get(i);
            final Card b = persisted.cards().get(i);
            final Card c = old.cards().get(i);

            assertEquals(a.suit(), b.suit());
            assertEquals(a.value(), b.value());
            if(c != a) {
                foundDifference = true;
            }
        }

        assertTrue("deck was not shuffled", foundDifference);
    }

    @Test public void describe() throws Exception { // get
        final String name = "describe-test";
        final Deck old = mapper.readValue(
                handlers.CREATE.handle("pre-insert-describe", name.getBytes()).payload(),
                Deck.class);

        final Result result = handlers.DESCRIBE.handle("test-describe", name.getBytes());

        final Deck deck = mapper.readValue(result.payload(), Deck.class);
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(name, deck.name());
        assertDeckOk(deck);

        for(int i = 0; i < 52; i++) {
            final Card a = deck.cards().get(i);
            final Card b = old.cards().get(i);

            assertEquals(a.suit(), b.suit());
            assertEquals(a.value(), b.value());
        }
    }

    @Test public void list() throws Exception { // post
        final String name1 = "list-test-1";
        handlers.CREATE.handle("pre-insert-list", name1.getBytes());

        final String name2 = "list-test-2";
        handlers.CREATE.handle("pre-insert-list", name2.getBytes());

        final DeckHandlers.ListDecksRequest req = new DeckHandlers.ListDecksRequest();
        req.pageSize = 1;
        req.offset = 1;

        final byte[] json = mapper.writeValueAsBytes(req);

        final Result result = handlers.LIST.handle("test-list", json);

        final List<Deck> decks = mapper.readValue(result.payload(), new TypeReference<List<Deck>>() {});
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(1, decks.size());

        final Deck deck = decks.get(0);
        assertEquals(name2, deck.name());
        assertDeckOk(deck);
    }

    @Test public void delete() throws Exception { // delete
        final String name = "delete-test";
        handlers.CREATE.handle("pre-insert-delete", name.getBytes());

        final Result result = handlers.DELETE.handle("test-delete", name.getBytes());

        assertEquals("\"deleted deck\"", new String(result.payload()));
        assertEquals(Status.SUCCESS, result.status());

        final Deck persisted = deckStore.findOne(name).orElse(null);
        assertNull(persisted);
    }

    @Test public void upsertOverwrites() throws Exception {
        final String name1 = "overwrite-test-1";
        handlers.CREATE.handle("pre-insert-overwrite", name1.getBytes());

        final String name2 = name1; // same name should overwrite
        handlers.CREATE.handle("pre-insert-overwrite", name2.getBytes());

        final DeckHandlers.ListDecksRequest req = new DeckHandlers.ListDecksRequest();
        req.pageSize = 1;
        req.offset = 0;

        final byte[] json = mapper.writeValueAsBytes(req);

        final Result result = handlers.LIST.handle("test-overwrite", json);

        final List<Deck> decks = mapper.readValue(result.payload(), new TypeReference<List<Deck>>() {});
        assertEquals(Status.SUCCESS, result.status());
        assertEquals(1, decks.size());

        final Deck deck = decks.get(0);
        assertEquals(name2, deck.name());
        assertDeckOk(deck);
    }
}
