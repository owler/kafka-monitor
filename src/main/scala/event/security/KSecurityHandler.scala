package event.security

import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.authentication.BasicAuthenticator
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.HashLoginService
import scala.jdk.CollectionConverters._

class KSecurityHandler extends ConstraintSecurityHandler {

  val constraint = new Constraint(Constraint.__BASIC_AUTH, "user")
  constraint.setAuthenticate(true)

  val cm = new ConstraintMapping
  cm.setPathSpec("/*")
  cm.setConstraint(constraint)
  setAuthenticator(new BasicAuthenticator())
  setConstraintMappings(List(cm).asJava)
  val loginService = new HashLoginService("MyRealm", "../conf/myRealm.properties")
  setLoginService(loginService)
}
