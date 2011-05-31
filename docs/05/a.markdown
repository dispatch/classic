URLs and Paths
--------------

Hosts are defined with the `:/` verb. It should remind you (a
little) of the characters that come before hosts in URLs.

```scala
import dispatch._
val sl = :/("www.scala-lang.org")
```

This value `sl` is a request to the root path of the Scala home
domain. What if we want some sub-path of it? There's an obvious verb
for that: `/`

```scala
val learnScala = sl / "node" / "1305"
```

Now we have a reference to a page (the "Learning Scala" page
actually), and `sl` is unchanged.

Request definitions such as these are immutable; by appending a path
to one you are creating a new request object, similar to string
concatenation in Java. If we don't need to use the host for different
requests, we would probably have defined this all at once:

```scala
val learnScala2 = :/("www.scala-lang.org") / "node" / "1305"
```

But—can't you define this with its actual URL? Of course!

```scala
val learnScala3 = url("http://www.scala-lang.org/node/1305")
```

The `url` verb is also imported from the `dispatch` package and it
produces a request object like the others. It is most useful when
dealing with URLs stored externally, or discovered at runtime.  For
fixed requests in your application code, building up from host and
path components is usually preferred as it lends itself to reuse.
