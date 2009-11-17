package dispatch;

import org.apache.http.*;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpCrutch extends DefaultHttpClient {
  public HttpResponse crutchExecute(HttpHost host, HttpUriRequest req) throws java.io.IOException {
    return super.execute(host, req);
  }
}

