package blueeyes.core.service

import blueeyes.util.RichThrowableImplicits._
import blueeyes.core.http._
import blueeyes.core.http.HttpStatusCodes._
import blueeyes.core.http.HttpVersions._
import blueeyes.concurrent.Future

trait HttpResponseHelpers{
  /** Shorthand function to create a future of an HttpResponse from the given parameters.
   * {{{
   * respond(content = Some(<html></html>))
   * }}}
   */
  def respond[T](status: HttpStatus = HttpStatus(OK), headers: Map[String, String] = Map(), content: Option[T] = None): Future[HttpResponse[T]] = {
    Future.sync(HttpResponse[T](status, headers, content))
  }
  
  /** Shorthand function to create a simple response based on a future and 
   * headers. If the future is delivered, the OK status code will be returned,
   * but if the future is canceled, an InternalServerError status code will
   * be returned.
   * {{{
   * respond(
   *    <html>
   *    </html>
   * )
   * }}}
   */
  def respondLater[T](content: Future[T], headers: Map[String, String] = Map()): Future[HttpResponse[T]] = {
    content map { 
      c => HttpResponse[T](HttpStatus(OK), headers, Some(c))
    } orElse { why =>
      HttpResponse[T](
        status = HttpStatus(
          InternalServerError, 
          why.map(_.fullStackTrace).getOrElse("The response was unexpectedly canceled")
        )
      )
    }
  }
}
object HttpResponseHelpers extends HttpResponseHelpers
