Two Handlers Are Better Than One
--------------------------------

Dispatch has so many handy response handlers--but what if you want to
use two with the same request? You *can't* do that with a stream of
the response body, since this can only be consumed once, but for
**headers** it's a simple and necessary operation.

### Split and Tuple

The simplest way to do this is with the handler-tupling verb, `>+`. It
takes a function from your request definition to a 2-tuple of response
handlers. When executed, its return value is a 2-tuple of those return
values.

```scala
import dispatch._
val http = new Http

val (lastmod, contents) = 
  http(:/("dispatch.databinder.net") >+ { req =>
    (req >:> { _("Last-Modified") }, req as_str)
  })
```

### Conditional Chain

The handler tuple above is too limited for some uses. For example, you
may want to handle a streaming response in a function that also has
the response headers in scope. Or you may want to apply an entirely
different handler depending on the header values.

The `>+>` handler-chaining verb provides all of this, with only a
slightly higher-flying functional structure to wrap your head around.

```scala
http(:/("dispatch.databinder.net") >+> { req =>
  req >:> {
    _("Content-Type").filter {
      _.contains("text/html")
    }.headOption.map { _ =>
      req </> { nodes => (nodes \\ "h1").text }
    }.getOrElse {
      req >> { _ => "unknown content type" }
    }
  }
})
```

The return value of the function passed to `>+>` must itself be a
handler. In this example, we use either a handler that parses HTML or
a handler that takes a stream and does nothing with it.
