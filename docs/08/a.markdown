Lift JSON
---------

Dispatch has two JSON handlers, one developed internally and one
developed to work with the [Lift][lift] framework. This reflects two
major representations of JSON data within Dispatch: dispatch.json and
net.liftweb.json.JsonAST.

[lift]: http://liftweb.net/

### Example Usage

Lift's JSON usage is very similar to using Dispatch's internal JSON
representation. As always, start by importing the main dispatch
methods.

```scala
import dispatch._
```
Instead of importing the internal JSON values, import Dispatch's
liftjson:

```scala
import dispatch.liftjson.Js._
```
We will now be able to use the ># operator in our handlers. For
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
