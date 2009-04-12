package dispatch.json

/** Json Extractor, matches if an expected property and type exists in a given JsValue. */
trait Extract[T] {
  def unapply(js: JsValue): Option[T]
}
trait Js {
  implicit val ctx: Option[Obj] = None
  /** Extractor that may have a parent. */
  case class Rel[T, E <: Extract[T]](parent: Option[Obj], self: E) extends Extract[T] {
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
  case class Member[T](sym: Symbol, ext: Extract[T]) extends Extract[T] {
    def unapply(js: JsValue) = js match {
      case js: JsObject => js.self.get(JsString(sym)) flatMap ext.unapply
      case _ => None
    }
    def << (t: T): JsValue => JsObject = {
      case js: JsObject => JsObject(js.self + (JsString(sym) -> JsValue(t)))
      case _ => error("a")
    }
  }
  implicit def rel2self[T, E <: Extract[T]](r: Rel[T,E]) = r.self
  
  class Obj(sym: Symbol)(implicit parent: Option[Obj]) 
      extends Rel[JsObject, Member[JsObject]](parent, Member(sym, obj)) {
    implicit val ctx = Some(this)
  }
  /** Assertion extracting function, error if expected Js type is not present. */
  type JsF[T] = JsValue => T
  /** Add operators to Symbol. */
  implicit def sym_add_operators[T](sym: Symbol) = new SymOp(sym)
  /** For ! and ? operators on Symbol. */
  case class SymOp(sym: Symbol) {
    /** @return an extractor */
    def ? [T](cst: Extract[T])(implicit parent: Option[Obj]) = 
      new Rel[T, Member[T]](parent, Member(sym, cst))
    /** @return an assertion extracting function (JsF) */
    def ! [T](cst: Extract[T]): JsF[T] = 
      new Member(sym, cst).unapply _ andThen { _.get }
  }
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  implicit def %[A,B](a: JsF[A], b: JsF[B])(js: JsValue) = (a(js), b(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  implicit def %[A,B,C](a: JsF[A], b: JsF[B], c: JsF[C])(js: JsValue) = (a(js), b(js), c(js))
  /** Combines assertion extracting functions into a single function returning a tuple, when curried. */
  implicit def %[A,B,C,D](a: JsF[A], b: JsF[B], c: JsF[C], d: JsF[D])(js: JsValue) = (a(js), b(js), c(js), d(js))
 }

object Js extends Js {
  def apply(): JsValue = JsValue()
  def apply(stream: java.io.InputStream): JsValue = JsValue.fromStream(stream)
  def apply(string: String): JsValue = JsValue.fromString(string)
  implicit def ext2fun[T](ext: Extract[T]): JsF[T] = ext.unapply _ andThen { _.get }
}
