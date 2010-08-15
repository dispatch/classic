package dispatch

/** Mix in to Http if you want JDK logging */
trait JdkLogging extends Http {
  override def get_logger = new dispatch.Logger {
    val jdklog = java.util.logging.Logger.getLogger("dispatch")
    def info(msg: String, items: Any*) { 
      jdklog.info(msg.format(items: _*)) 
    }
  }
}
