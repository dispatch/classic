Two Handlers Are Better Than One
--------------------------------

Dispatch has so many handy response handlers--but what if you want to
use two with the same request? You *can't* do that with a stream of
the response body, since this can only be consumed once, but for
**headers** it's essential and straightforward.

### Header Chaining

The recommended header-handling verb `>:+` provides a header `Map` and
a request object to chain a second handler for the body.

```scala
http(:/("dispatch.databinder.net") >:+ { (headers, req) =>
  headers("content-type").filter {
    _.contains("text/html")
  }.headOption.map { _ =>
    req </> { nodes => (nodes \\\\ "h1").text }
  }.getOrElse {
    req >> { _ => "unknown content type" }
  }
})
```

> To facilitate case-insensitive handling, the keys in the header
> `Map` are uniformly lowercase.

The function passed to `>:+` should produce a second handler for the
body of the response. In this case, it produces different response
handlers depending on the headers present.
