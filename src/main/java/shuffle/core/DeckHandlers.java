package shuffle.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import shuffle.ports.ActionHandler;
import shuffle.ports.DeckStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DeckHandlers {

    private DeckStore store;
    private Integer pageSize;

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // allows for format evolution
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    private static <T> T require(T o) {
        if(o == null) {
            throw new IllegalArgumentException("missing required field");
        }

        return o;
    }

    private static String trimToNull(String s) {
        final String out = StringUtils.trim(s);
        return StringUtils.isEmpty(out)? null : out;
    }

    public DeckHandlers(DeckStore store, Integer pageSize) {
        this.store = store;
        this.pageSize = pageSize;
    }

    private static List<Card> initCards() {
        final List<Card> cards = new ArrayList<>();

        for(Card.Suit suit : Card.Suit.values()) {
            cards.addAll(IntStream.range(Card.MIN, Card.MAX + 1)
                .mapToObj(i -> new Card(suit, i))
                .collect(Collectors.toList()));
        }

        return cards;
    }

    private static List<Card> randomize(List<Card> in) {
        final List<Card> out = new ArrayList<>(in);
        Collections.shuffle(out);
        return out;
    }

    private static List<Card> shuffle(List<Card> in) {
        final List<Card> out = new ArrayList<>();
        final List<Card> left = in.subList(0, in.size()/2); // in odd cases this will always be the smaller half
        final List<Card> right = in.subList(in.size()/2, in.size());

        for(int i = 0; i<right.size(); i++) {
            out.add(left.get(i));
            out.add(right.get(i));
        }

        return out;
    }

    private static List<Card> shuffleALot(List<Card> in) {
        return shuffle( shuffle( shuffle( shuffle( shuffle(in)))));
    }

    public static ActionHandler.Result deckResult(Object o) {
        try {
            final byte[] json = mapper.writeValueAsBytes(o);

            return new ActionHandler.Result() {
                public byte[] payload() { return json; }
                public ActionHandler.Status status() { return ActionHandler.Status.SUCCESS; }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Error {
        public String message;

        public Error(String message) {
            this.message = message;
        }
    }

    public static ActionHandler.Result error(ActionHandler.Status status, String message) {
        try {
            final byte[] jsonErr = mapper.writeValueAsBytes(new Error(message));

            return new ActionHandler.Result() {
                public byte[] payload() { return jsonErr; }
                public ActionHandler.Status status() { return status; }
            };

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ActionHandler.Result rejection(String message) {
        return error(ActionHandler.Status.REJECTED, message);
    }

    public static ActionHandler.Result failure(String message) {
        return error(ActionHandler.Status.FAILED, message);
    }

    public ActionHandler CREATE = (requestId, bytes) -> {
        final String deckName = new String(bytes);

        final Deck deck = new Deck();
        final Deck created;

        try {
            deck.setName(   require(    trimToNull(deckName)));
            deck.setCards(  randomize(  initCards()         )); // do an initial randomize of the deck
            created = store.upsert(deck).orElse(null);

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }

        return created == null? failure("couldn't create deck"): deckResult(created);
    };

    public static class ShuffleRequest {
        public String name;
    }

    public ActionHandler RANDOMIZE = (requestId, bytes) -> {
        final ShuffleRequest req = mapper.readValue(bytes, ShuffleRequest.class);

        final Deck deck;
        final Deck randomized;
        try {
            deck = store.findOne(    require(    trimToNull(req.name))).orElse(null);

            if(deck != null) {
                deck.setCards(  randomize(    deck.cards()));
                randomized = store.upsert(deck).orElse(null);
                return randomized == null? failure("couldn't persist randomized deck") : deckResult(randomized);

            } else {
                return failure("couldn't find deck");
            }

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }
    };

    public ActionHandler SHUFFLE = (requestId, bytes) -> {
        final ShuffleRequest req = mapper.readValue(bytes, ShuffleRequest.class);

        final Deck deck;
        final Deck shuffled;
        try {
            deck = store.findOne(    require(    trimToNull(req.name))).orElse(null);

            if(deck != null) {
                deck.setCards(  shuffleALot(    deck.cards()));
                shuffled = store.upsert(deck).orElse(null);
                return shuffled == null? failure("couldn't persist shuffled deck") : deckResult(shuffled);

            } else {
                return failure("couldn't find deck");
            }

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }
    };

    public ActionHandler DESCRIBE = (requestId, bytes) -> {
        final String deckName = new String(bytes);

        try {
            final Deck deck = store.findOne(require(trimToNull(deckName))).orElse(null);
            return deck == null? failure("couldn't find deck") : deckResult(deck);

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }
    };

    public static class ListDecksRequest {
        public Integer offset;
        public Integer pageSize;
    }

    public ActionHandler LIST = (requestId, bytes) -> {
        final ListDecksRequest req = mapper.readValue(bytes, ListDecksRequest.class);

        final List<Deck> decks;
        try {
            decks = store.findPage(
                req.pageSize == null? pageSize : req.pageSize,
                require(req.offset)
            );

            if(decks != null) {
                return deckResult(decks);

            } else {
                return failure("couldn't find decks");
            }

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }
    };

    public ActionHandler DELETE = (requestId, bytes) -> {
        final String deckName = new String(bytes);

        try {
            final Deck deck = store.findOne(require(trimToNull(deckName))).orElse(null);
            store.delete(require(trimToNull(deckName)));
            return deck == null? failure("couldn't find deck") : deckResult("deleted deck");

        } catch (IllegalArgumentException bad) {
            return rejection(bad.getMessage());
        }
    };
}
