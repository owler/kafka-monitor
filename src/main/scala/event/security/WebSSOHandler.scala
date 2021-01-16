package event.security

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class WebSSOHandler(enabled: Boolean) extends AbstractHandler{
  override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    println(request.getHeaderNames)
    if(!check(baseRequest)){
      if(!baseRequest.isHandled) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN)
        baseRequest.setHandled(true)
      }
    }
  }

  private def check(request: Request): Boolean = {
    request.getHeader("PS_TOKEN") != null
  }
}
