package dispatch.classic

/** Trust all TLS certs */
trait HttpsLeniency { self: BlockingHttp =>
  import java.net.Socket
  import javax.net.ssl.{X509TrustManager, SSLContext}
  import java.security.KeyStore
  import java.security.cert.X509Certificate
  import org.apache.http.conn.scheme.Scheme
  import org.apache.http.conn.ssl.SSLSocketFactory

  private def socket_factory = {
    val truststore = KeyStore.getInstance(KeyStore.getDefaultType())
    truststore.load(null, null)
    val context = SSLContext.getInstance(SSLSocketFactory.TLS)
    val manager = new X509TrustManager {
      def checkClientTrusted(xcs: Array[X509Certificate], string: String) {}
      def checkServerTrusted(xcs: Array[X509Certificate], string: String) {}
      def getAcceptedIssuers = null
    }
    context.init(null, Array(manager), null)
    new SSLSocketFactory(context,
                         SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
  }

  client.getConnectionManager.getSchemeRegistry.register(
    new Scheme("https", 443, socket_factory)
  )
}
