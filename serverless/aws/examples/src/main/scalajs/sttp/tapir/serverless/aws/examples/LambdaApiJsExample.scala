package sttp.tapir.serverless.aws.examples

import cats.syntax.all._
import sttp.monad.{FutureMonad, MonadError}
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.serverless.aws.lambda._
import sttp.tapir.serverless.aws.lambda.js._

import scala.scalajs.js.annotation._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object LambdaApiJsExample {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val helloEndpoint: ServerEndpoint[Unit, Unit, String, Any, Future] = endpoint.get
    .in("api" / "hello")
    .out(stringBody)
    .serverLogic { _ => Future(s"Hello!".asRight[Unit]) }

  val options: AwsServerOptions[Future] = AwsFutureServerOptions.default.copy(encodeResponseBody = false)

  val route: JsRoute[Future] = AwsFutureServerInterpreter(options).toRoute(helloEndpoint).toJsRoute

  @JSExportTopLevel(name="handler")
  def handler(event: AwsJsRequest, context: Any): scala.scalajs.js.Promise[AwsJsResponse] = {
    import scala.scalajs.js.JSConverters._
    route(event).toJSPromise
  }
}
