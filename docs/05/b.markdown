Response Bodies
---------------

Request definitions, by themselves, are not enough information for
Dispatch to do its job. The response that the server returns may have
no body at all—or it could contain too much data to hold in memory.

Before Dispatch makes a request, then, you must tell it how to handle
the server's response. For this we have *handlers*. Handlers are
created from request definitions using handler verbs. Taking up the
request defined on the last page, we could simply ignore the response.

```scala
val ignore = learnScala >|
```

This would be very unusual for a GET request in particular, which
shouldn't have any side effects. (It's also not speaking well of our
commitment to learning Scala.) 
