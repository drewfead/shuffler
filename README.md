## Shuffle
To start the project, use `gradle run` from the root directory.

You must have a file at `/opt/apps/shuffle/config.properties`.  Here's a
reasonable template:

```
driver.http.port=9421
driver.store.pagesize=5
core.shuffle.human=true
```

Example usage of the service can be found in IntegrationTest using an
HttpClient programmatically, but the available endpoints are as follows:
 
* `/deck/create` - PUT (params: name)
* `/deck/shuffle` -- POST (json like `{"name":"xxxxx"}`
* `/deck/describe` -- GET (params: name)
* `/deck/list` -- POST (json like `{"pageSize":10, "offset":1}`. `pageSize` is optional        
* `/deck/delete`  -- DELETE (params: name)

Here are the approximate requirements for the project (note that I made
some adjustments regarding list, to avoid the unbounded data problem):
* Create a microservice that stores and shuffles card decks.
* A card may be represented as a simple string such as “5-heart”, or “K-spade”.
* A deck is an ordered list of 52 standard playing cards.
* Expose a RESTful interface that allows a user to:
* PUT an idempotent request for the creation of a new named deck.  New decks are created in some initial sorted order.
* POST a request to shuffle an existing named deck.
* GET a list of the current decks persisted in the service.
* GET a named deck in its current sorted/shuffled order.
* DELETE a named deck.
* Design your own data and API structure(s) for the deck.
* Persist the decks in-memory only, but stub the persistence layer such that it can be later upgraded to a durable datastore.
* Implement a simple shuffling algorithm that simply randomizes the deck in-place.
* Implement a more complex algorithm that simulates hand-shuffling, i.e. splitting the deck in half and interleaving the two halves, repeating the process multiple times.
* Allow switching the algorithms at deploy-time only via configuration.

