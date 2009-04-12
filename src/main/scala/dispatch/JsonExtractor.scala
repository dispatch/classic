package dispatch.json

/** Json Extractor, extracts a value of type T from the given JsValue. */
trait Extract[T] {
  def unapply(js: JsValue): Option[T]
}
/** Json Inserter, adds or replaces properties in the given JsValue
    (error if not a JsObject) */
trait Insert[T] extends Extract[T] {
  def << (t: T)(js: JsValue): JsObject
}
/** Json Property Extractor, extracts a value of type T assigned to 
    the property sym in a given JsObject (checks any JsValue). Additionally,
    can replace the property sym with a new value T in the << opertion. */
case class Property[T](sym: Symbol, ext: Extract[T]) extends Extract[T] with Insert[T] {
  def unapply(js: JsValue) = js match {
    case js: JsObject => js.self.get(JsString(sym)) flatMap ext.unapply
    case _ => None
  }
  /** Adds or replaces the property sym in the given JsValue (JsObject) */
  def << (t: T)(js: JsValue) = js match {
    case JsObject(m) => JsObject(m + (JsString(sym) -> JsValue(t)))
    case js => error("Unable to replace property in " + js)
  }
}
/** Extractor that resolves first by its parent extractor, if present. */
case class Child[T, E <: Insert[T]](parent: Option[Obj], self: E) extends Extract[T] with Insert[T] {
  def unapply(js: JsValue) = parent map { parent =>  js match {
      case parent(self(t)) => Some(t)
    } } getOrElse { js match {
      case self(t) => Some(t)
      case _ => None
    }
  }
  /** Inserts the value t in self and replaces self in parent, if any. */
  def << (t: T)(js: JsValue) = parent map { parent => js match {
      case parent(my_js) => (parent << (self << t)(my_js))(js)
    } } getOrElse (self << t)(js)
}
/** Obj extractor, respects current parent context and sets a new context to itself. */
class Obj(sym: Symbol)(implicit parent: Option[Obj]) 
    extends Child[JsObject, Property[JsObject]](parent, Property(sym, Js.obj)) {
  implicit val ctx = Some(this)
}
/** Namespace and context for Json extraction. Client extraction 
    objects, e.g. dispatch.twitter.Search, may extend this trait to 
    receive all implicit functions and values. */
trait Js {
  object str extends Extract[String] {
    def unapply(js: JsValue) = js match {
      case JsString(v) => Some(v)
      case _ => None
    }
  }
  object num extends Extract[BigDecimal] {
    def unapply(js: JsValue) = js match {
      case JsNumber(v) => Some(v)
      case _ => None
    }
  }
  object bool extends Extract[Boolean] {
    def unapply(js: JsValue) = js match {
      case JsBoolean(v) => Some(v)
      case _ => None
    }
  }
  // keep in wrapped type to allow nested extractors
  object obj extends Extract[JsObject] {
    def unapply(js: JsValue) = js match {
      case JsObject(v) => Some(JsObject(v))
      case _ => None
    }
  }
  object list extends Extract[List[JsValue]] {
    def unapply(js: JsValue) = js match {
      case JsArray(v) => Some(v)
      case _ => None
    }
    def ! [T](ext: Extract[T]) = new Extract[List[T]] {
      def unapply(js: JsValue) = js match {
        case list(l) => Some(l map { _ match { case ext(v) => v } } )
        case _ => None
      }
    }
  }
  /** Converts a Child extractor to the wrapped extractor E, e.g. on << */
  implicit def child2self[T, E <: Property[T]](r: Child[T,E]) = r.self
  
  /** The parent Obj context, defaults to None for top level definitions. */
  implicit val ctx: Option[Obj] = None
  
  /** Assertion extracting function, error if expected Js type is not present. */
  type JsF[T] = JsValue => T

  /** Assertion inserting function, puts any type value into the given JsValue. */
  type JsIF = JsF[JsObject]
  
  /** Add operators to Symbol. */
  implicit def sym_add_operators[T](sym: Symbol) = new SymOp(sym)
  /** For ! , ? , and << operators on Symbol. */
  case class SymOp(sym: Symbol) {
    /** @return an extractor, respects current Obj context */
    def ? [T](cst: Extract[T])(implicit parent: Option[Obj]) = 
      new Child[T, Property[T]](parent, Property(sym, cst))
    
    /** @return an assertion extracting function (JsF) */
    def ! [T](cst: Extract[T]): JsF[T] = 
      new Property(sym, cst).unapply _ andThen { _.get }
    
    /** @return new JsObject with the given sym property replaced by t */
    def << [T](t: T): JsIF = _ match {
      case JsObject(m) => JsObject(m + (JsString(sym) -> JsValue(t)))
      case js => error("Unable to replace property in " + js)
    }
    /** Chain this to another assertion inserting function to replace deep properties */
    def << (f: JsIF): JsIF = js => (this << f((this ! obj)(js)))(js)
  }
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B](a: JsF[A], b: JsF[B])(js: JsValue) = (a(js), b(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B,C](a: JsF[A], b: JsF[B], c: JsF[C])(js: JsValue) = (a(js), b(js), c(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B,C,D](a: JsF[A], b: JsF[B], c: JsF[C], d: JsF[D])(js: JsValue) = (a(js), b(js), c(js), d(js))
 }

/** Factory for JsValues as well as a global access point for
    implicit functions and values. This object extends the Js
    trait so that `import Js._` brings implicits into scope. */
object Js extends Js {
  def apply(): JsValue = JsValue()
  def apply(stream: java.io.InputStream): JsValue = JsValue.fromStream(stream)
  def apply(string: String): JsValue = JsValue.fromString(string)
  /** Converts  Json a extrator to an assertion extracting function (JsF). */
  implicit def ext2fun[T](ext: Extract[T]): JsF[T] = ext.unapply _ andThen { _.get }
}
