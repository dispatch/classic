TagSoup
-------

[TagSoup][tagsoup] is a SAX-compliant parser, that parses HTML instead
of well formed XML. TagSoup is very resilient, allowing you to load HTML
as found in the wild, to be processed as a scala NodeSeq.

[tagsoup]: http://ccil.org/~cowan/XML/tagsoup/

### Example Usage

To process the response with TagSoup you first have to make the handler
verbs available. This can be done in several ways.

```scala
import dispatch.tagsoup.TagSoupHttp._
```
Now we can use the operators `</>`, `tagsouped` and `as_tagsouped` in
our handlers. If we want to find the title of a HTML page it can look
something like this.

```scala
import dispatch.tagsoup.TagSoupHttp._

val title = Http(:/("example.org") </> { ns =>
              (ns \\ "title").text
            })
```
TagSoup let's you work with the HTML as a `scala.xml.NodeSeq` and as a
convenience you can use `as_tagsouped` to retrieve it.

```scala
val ns = Http(:/("example.com") as_tagsouped)
```

