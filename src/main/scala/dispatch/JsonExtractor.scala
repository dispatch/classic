package dispatch.json

/** Json Extractor, extracts a value of type T from the given JsValue. */
trait Extract[T] {
  def unapply(js: JsValue): Option[T]
}
/** Namespace and context for Json extraction. Nested extraction objects
    should extend this trait to receive an initial Obj context of None
    that Obj instances reset to themselves. For assertion extraction
    and flat extractors it is sufficient to import Js._ */
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
  /** Json Property Extractor, extracts a value of type T assigned to 
      the property sym in a given JsObject (checks any JsValue). Additionally,
      can replace the property sym with a new value T in the << opertion. */
  case class Property[T](sym: Symbol, ext: Extract[T]) extends Extract[T] {
    def unapply(js: JsValue) = js match {
      case js: JsObject => js.self.get(JsString(sym)) flatMap ext.unapply
      case _ => None
    }
    def << (t: T): JsValue => JsObject = {
      case JsObject(m) => JsObject(m + (JsString(sym) -> JsValue(t)))
      case js => error("Unable to replace property in " + js)
    }
  }
  /** Extractor that resolves first by its parent extractor, if present. */
  case class Child[T, E <: Extract[T]](parent: Option[Obj], self: E) extends Extract[T] {
    def unapply(js: JsValue) = parent match {
      case Some(parent) => js match {
        case parent(self(t)) => Some(t)
      }
      case None => js match {
        case self(t) => Some(t)
        case _ => None
      }
    }
  }
  /** Converts a Child extractor to the wrapped extractor E, e.g. on << */
  implicit def child2self[T, E <: Extract[T]](r: Child[T,E]) = r.self
  
  /** The parent Obj context, defaults to None for top level definitions. */
  implicit val ctx: Option[Obj] = None
  
  /** Obj extractor, respects current parent context and sets a new context to itself. */
  class Obj(sym: Symbol)(implicit parent: Option[Obj]) 
      extends Child[JsObject, Property[JsObject]](parent, Property(sym, obj)) {
    implicit val ctx = Some(this)
  }
  /** Assertion extracting function, error if expected Js type is not present. */
  type JsF[T] = JsValue => T
  
  /** Add operators to Symbol. */
  implicit def sym_add_operators[T](sym: Symbol) = new SymOp(sym)
  /** For ! and ? operators on Symbol. */
  case class SymOp(sym: Symbol) {
    /** @return an extractor, respects current Obj context */
    def ? [T](cst: Extract[T])(implicit parent: Option[Obj]) = 
      new Child[T, Property[T]](parent, Property(sym, cst))
    /** @return an assertion extracting function (JsF) */
    def ! [T](cst: Extract[T]): JsF[T] = 
      new Property(sym, cst).unapply _ andThen { _.get }
  }
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B](a: JsF[A], b: JsF[B])(js: JsValue) = (a(js), b(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B,C](a: JsF[A], b: JsF[B], c: JsF[C])(js: JsValue) = (a(js), b(js), c(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  def %[A,B,C,D](a: JsF[A], b: JsF[B], c: JsF[C], d: JsF[D])(js: JsValue) = (a(js), b(js), c(js), d(js))
 }

object Js extends Js {
  def apply(): JsValue = JsValue()
  def apply(stream: java.io.InputStream): JsValue = JsValue.fromStream(stream)
  def apply(string: String): JsValue = JsValue.fromString(string)
  implicit def ext2fun[T](ext: Extract[T]): JsF[T] = ext.unapply _ andThen { _.get }
}
