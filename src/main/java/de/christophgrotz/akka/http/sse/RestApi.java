package de.christophgrotz.akka.http.sse;

import akka.actor.ActorSystem;
import akka.http.model.japi.*;
import akka.http.model.japi.headers.AccessControlAllowHeaders;
import akka.http.model.japi.headers.AccessControlAllowMethods;
import akka.http.model.japi.headers.AccessControlAllowOrigin;
import akka.http.model.japi.headers.HttpOriginRange;
import akka.http.server.japi.HttpApp;
import akka.http.server.japi.PathMatcher;
import akka.http.server.japi.PathMatchers;
import akka.http.server.japi.Route;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.ByteString$;
import com.google.common.collect.Lists;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Christoph Grotz on 14.12.14.
 */
public class RestApi extends HttpApp {
  public Route withSSE(final Source<ByteString> source) {
    return handleWith(ctx -> {
      MediaType mediaType = MediaTypes.custom("text", "event-stream", false, false, Lists.newArrayList("sse"), new HashMap<>());
      akka.http.model.japi.ContentType contentType = akka.http.model.japi.ContentType.create(mediaType, HttpCharsets.UTF_8);

      akka.http.model.japi.HttpResponse response = HttpResponse.create()
        .addHeader(AccessControlAllowOrigin.create(HttpOriginRange.ALL))
        .addHeader(AccessControlAllowHeaders.create("Access-Control-Allow-Origin", "Access-Control-Allow-Method", "Content-Type"))
        .addHeader(AccessControlAllowMethods.create(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.OPTIONS, HttpMethods.DELETE))
        .withEntity(HttpEntities.createCloseDelimited(contentType, source.asScala()));

      return ctx.complete(response);
    });
  }

  @Override
  public Route createRoute() {
    PathMatcher<String> id = PathMatchers.segment();
    return route(
      path(
        "messages"
      ).route(
        get(
          withSSE(
            Source.from(Duration.create(1, TimeUnit.SECONDS), Duration.create(5, TimeUnit.SECONDS), () -> "Hello World").map((x) -> ByteString$.MODULE$.apply("data: " + x + "\r\n\r\n", "UTF-8"))
          )
        )
      )
    );
  }

  public static void main(String ... args) throws Exception {
    RestApi api = new RestApi();
    api.bindRoute("localhost",8080, ActorSystem.create());
  }
}
