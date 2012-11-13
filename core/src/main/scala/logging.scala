package dispatch

/** Mix in to Http if you want JDK logging */
trait JdkLogging extends HttpExecutor {
  override def make_logger = new dispatch.Logger {
    val jdklog = java.util.logging.Logger.getLogger("dispatch")
    def info(msg: String, items: Any*) { 
      jdklog.info(msg.format(items: _*)) 
    }
    def warn(msg: String, items: Any*) { 
      jdklog.warning(msg.format(items: _*)) 
    }
  }
}

/**
 * Mix in to Http if you want no logging from Dispatch.
 * Note that HttpClient logs separately:
 * http://hc.apache.org/httpcomponents-client/logging.html
 */
trait NoLogging extends HttpExecutor {
  override def make_logger = new dispatch.Logger {
    def info(msg: String, items: Any*) { }
    def warn(msg: String, items: Any*) { }
  }
}

/** Mix in to Http if you want SLF4J logging */
trait Slf4jLogging extends HttpExecutor {
  override def make_logger = new Logger {
    val logger = org.slf4j.LoggerFactory.getLogger("dispatch")
    def info(msg: String, items: Any*) { logger.info(msg.format(items: _*)) }
    def warn(msg: String, items: Any*) { logger.warn(msg.format(items: _*)) }
  }
}