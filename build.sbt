import sbt.librarymanagement.ArtifactFilter

import scala.annotation.tailrec

publishMavenStyle in ThisBuild := false
name := "simple-" + baseDirectory.value.getName
val workaround = {
    sys.props += "packaging.type" -> "jar"
    ()
}
version := "1.0.0"

scalaVersion := "2.13.4"
val camelVersion = "2.25.2"
val akkaVersion = "2.5.32"

val env = sys.props.getOrElse("env", "dev")
Compile / resourceDirectory := baseDirectory.value / "resources" / env

scalacOptions ++= Seq("-deprecation")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

unmanagedBase := baseDirectory.value / "lib"

// https://mvnrepository.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api
libraryDependencies += "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.3"
// https://mvnrepository.com/artifact/org.apache.kafka/kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.6.0"
// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"


libraryDependencies += "junit" % "junit" % "4.12" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-camel" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "org.apache.camel" % "camel-core" % camelVersion
libraryDependencies += "org.apache.camel" % "camel-jetty" % camelVersion
libraryDependencies += "org.apache.camel" % "camel-http" % camelVersion
libraryDependencies += "org.apache.camel" % "camel-stream" % camelVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.10"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.10"
libraryDependencies += "org.jasypt" % "jasypt" % "1.9.3"
// https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-util
libraryDependencies += "org.eclipse.jetty" % "jetty-util" % "9.4.32.v20200930" % Test


def processTemplate(f: File, props: Map[String, String]) {
    @tailrec
    def processTemplate0(content: String, props: List[(String, String)]): String = {
        props match {
            case Nil => content
            case h::tail => processTemplate0(content.replace(h._1, h._2), tail)
        }
    }
    IO.write(f, processTemplate0(IO.read(f), props.toList))
}

val pack = taskKey[Unit]("pack")
pack := {
    (clean.value, update.value, crossTarget.value, (packageBin in Compile).value) match {
        case (cl, updateReport, out, jar) =>
            val dist = out / "dist"
            val af: ArtifactFilter = (a: Artifact) => a.`type` != "source" && a.`type` != "javadoc" && a.`type` != "javadocs"
            //updateReport.select(configuration = Set("runtime"), configurationFilter(name = "runtime"), artifact = af)  foreach {
            updateReport.select(configuration = configurationFilter(name = "runtime"), module = moduleFilter(name = "*"), artifact = af) foreach {
                srcPath =>
                    val destPath = dist / "lib" / srcPath.getName
                    IO.copyFile(srcPath, destPath, preserveLastModified = true)
            }
            IO.copyDirectory(file(".") / "src/bin", dist / "bin")
            IO.copyDirectory(file(".") / "src/main/resources" / env, dist / "conf")
            IO.copyDirectory(file(".") / "src/web/", dist / "web/kmon")
            IO.copyDirectory(file(".") / "src/plugins/", dist / "plugins")
            IO.createDirectory(dist / "lib/ext")
            IO.createDirectory(dist / "logs")
            IO.copyFile(jar, dist / "lib" / jar.getName)
            processTemplate(dist / "web/kmon" / "index.html", Map("{env}" -> env.toUpperCase()))
    }
}