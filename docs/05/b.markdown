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

To use a handler, you pass it to a Dispatch [executor][executor].

[executor]: Choose+an+Executor.html

```scala
Http(ignore)
```

Okay, but let's say you actually want to do something with the
response. Assuming it's text of a reasonable size, you could retrieve
it as a string:

```scala
Http(learnScala >- { str =>
  str.length
})
```

Like most handlers this one takes a function from the transformed
response body to any type. Here the function is of type `String =>
Int`. It merely returns the string length of the response.

How about trying something more interesting with the body, like
extracting the page's title?

```scala
Http(learnScala </> { nodes =>
  (nodes \\\\ "title").text
})
```

The `</>` verb defines a handler that processes the response as XHTML
and calls its given function with a `scala.xml.NodeSeq`. The \\\\
method projects over the `<title>` node, and `text` gives its contents.
