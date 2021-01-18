package event.security

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.HandlerWrapper

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class WebSSOHandler(enabled: Boolean) extends HandlerWrapper {
  override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    if (enabled && !check(baseRequest)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN)
      baseRequest.setHandled(true)
    }
    super.handle(target, baseRequest, request, response)
  }

  private def check(request: Request): Boolean = {
    val cookies = request.getCookies
    cookies != null && cookies.exists(_.getName == "PS_TOKEN")
  }
}
