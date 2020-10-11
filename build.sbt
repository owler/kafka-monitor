import sbt.librarymanagement.ArtifactFilter

publishMavenStyle in ThisBuild := false
name := "simple-" + baseDirectory.value.getName
val workaround = {
    sys.props += "packaging.type" -> "jar"
    ()
}
version := "1.0.0"

scalaVersion := "2.12.8"
val camelVersion = "2.23.0"
val akkaVersion = "2.5.19"

scalacOptions ++= Seq("-deprecation")

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

unmanagedBase := baseDirectory.value / "lib"

// https://mvnrepository.com/artifact/jakarta.xml.bind/jakarta.xml.bind-api
libraryDependencies += "jakarta.xml.bind" % "jakarta.xml.bind-api" % "2.3.3"
// https://mvnrepository.com/artifact/org.apache.kafka/kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.2.0"
// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"


libraryDependencies += "junit" % "junit" % "4.12" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.4"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-camel" % akkaVersion
libraryDependencies += "org.apache.camel" % "camel-jetty" % camelVersion
libraryDependencies += "org.apache.camel" % "camel-stream" % camelVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.10"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.10"
libraryDependencies += "org.jasypt" % "jasypt" % "1.9.1"


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
            IO.copyDirectory(file(".") / "src/main/resources/", dist / "conf")
            IO.copyFile(jar, dist / "lib" / jar.getName)
    }
}

