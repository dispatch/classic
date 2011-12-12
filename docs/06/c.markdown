JSoup
-----

[JSoup][jsoup] is a library for working with real-world HTML. It parses HTML
and provides a convenient API for extracting and manipulating data.
JSoup is similar to TagSoup as they both lets you work with real-world HTML,
but JSoup provide more functionality for working with the extracted result.

[jsoup]: http://jsoup.org/

### Example Usage

To use JSoup you have to make it handler verbs available. This is easiest done
by importing them from JSoupHttp.

```scala
import dispatch.jsoup.JSoupHttp._
```
Now we can use the operators `\\>`, jsouped, as_jsouped and as_jsoupedNodeSeq
in our handlers. As a start we can extract the title from a HTML page.

```scala
import dispatch.jsoup.JSoupHttp._

val title = Http(:/("example.org") \\> { doc =>
              doc.title
            })
```
JSoup parse the HTML to a DOM like structure `org.jsoup.nodes.Document`. There
is a rich set of find and search methods returning `org.jsoup.nodes.Elements`.
Elements implements java List so they are very easy to use with scala
collections, just make the usual `import scala.collection.JavaConversions._`.

So, to extract all links from a page and put them in a list as absolute paths
looks like this:

```scala
import dispatch.jsoup.JSoupHttp._
import scala.collection.JavaConversions._

val request = :/("example.org") / "test.html"
val list = Http(request \\> { doc =>
  doc.search("a[href]").asList.map(_.attr("abs:href"))
}
```
JSoup is a great api for processing HTML, and scala makes it even better.
To learn more of it's capabilities take a look in the [JSoup Cookbook][cookbook]

[cookbook]: http://jsoup.org/cookbook/
