Lift JSON
---------

Lift-JSON is developed and supported by the [Lift][lift]
framework. These are some basic instructions for using it with
Dispatch.

[lift]: http://liftweb.net/

### Example Usage

Using Lift-JSON is very similar to using Dispatch's internal JSON
representation. As always, start by importing the main Dispatch
methods.

```scala
import dispatch._
```
But instead of importing the internal JSON methods, import Dispatch's
interface to lift-json:

```scala
import dispatch.liftjson.Js._
```

We will now be able to use the `>#` operator in our handlers. For
example, if we wanted to search for scala podcasts, we might use
something like

```scala
import net.liftweb.json.JsonAST._

val http = new Http()
val u = url("http://gpodder.net/search.json") <<? Map("q" -> "scala")
http(u ># { json => 
  (json \ "title" children) flatMap( _ match {
    case JField("title", JString(d)) => Some(d)
    case JString(d) => Some(d)
    case _ => None
  })
})
```

This script starts by importing lift's JSON values (for pattern
matching) and then creates the http executor and the gpodder url. We
then execute with a handler which accepts a lift JValue and returns a
list of the podcast titles. Simple, no?
