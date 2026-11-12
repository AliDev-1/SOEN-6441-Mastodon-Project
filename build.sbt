import scala.sys.process.Process

// Generate Java documentation for our production and test sources together.
lazy val javadoc = taskKey[File]("Generate Javadoc including private methods and tests")

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name := "mawa",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.18",
    libraryDependencies ++= Seq(
      guice,
      ws,
      "org.jsoup" % "jsoup" % "1.21.2",
      "junit" % "junit" % "4.13.2" % Test,
      "org.mockito" % "mockito-core" % "5.24.0" % Test
    ),
    // Keep Java compilation consistent between developer machines.
    javacOptions ++= Seq("--release", "21", "-encoding", "UTF-8", "-parameters", "-Xlint:unchecked"),
    Test / fork := true,
    Test / parallelExecution := false,
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    // Only compiler-generated routes and templates are excluded from coverage.
    Test / jacocoExcludes := Seq("router.*", "controllers.routes*", "controllers.Reverse*", "controllers.javascript.*", "views.*"),
    Test / jacocoReportSettings := JacocoReportSettings()
      .withTitle("MAWA coverage")
      .withFormats(JacocoReportFormats.HTML, JacocoReportFormats.XML)
      .withThresholds(JacocoThresholds(line = 100, method = 100)),
    Test / jacoco := {
      (Test / jacoco).value
      // Zero missed branches also handles classes with no branches (reported N/A).
      val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      val report = factory.newDocumentBuilder().parse(((Test / jacocoReportDirectory).value / "jacoco.xml"))
      val counters = report.getElementsByTagName("counter")
      val missedBranches = (0 until counters.getLength).map(counters.item).filter { node =>
        node.getAttributes.getNamedItem("type").getNodeValue == "BRANCH"
      }.exists(_.getAttributes.getNamedItem("missed").getNodeValue.toInt > 0)
      if (missedBranches) sys.error("Branch coverage failed: every handwritten branch must be covered")
      streams.value.log.info("Branch coverage passed: zero missed branches")
    },
    javadoc := {
      // Compile first so Play-generated types referenced by Java are available.
      (Test / compile).value
      val sources = ((Compile / unmanagedSources).value ++ (Test / unmanagedSources).value)
        .filter(_.getName.endsWith(".java"))
      val output = target.value / "site" / "javadoc"
      val executable = file(sys.props("java.home")) / "bin" / "javadoc"
      val classpath = (Test / fullClasspath).value.files.mkString(java.io.File.pathSeparator)
      IO.delete(output)
      IO.createDirectory(output)
      // Standard Javadoc does not render @author on methods. Translate only
      // temporary copies to a custom tag; keep the required tags in source code.
      val staging = target.value / "javadoc-sources"
      IO.delete(staging)
      val documentedSources = sources.map { source =>
        val copy = staging / IO.relativize(baseDirectory.value, source).get
        IO.write(copy, IO.read(source).replaceAll("(?m)^(\\s*\\*\\s*)@author\\b", "$1@mawa.author"))
        copy
      }
      val arguments = Seq(executable.getAbsolutePath, "-quiet", "-private", "-author",
        "-tag", "mawa.author:a:Author:", "-encoding", "UTF-8", "-docencoding", "UTF-8",
        "-Xdoclint:all", "-Werror", "-d", output.getAbsolutePath, "-classpath", classpath)
      val exitCode = Process(arguments ++ documentedSources.map(_.getAbsolutePath), baseDirectory.value).!
      if (exitCode != 0) sys.error("Javadoc generation failed")
      streams.value.log.info("Javadoc: " + (output / "index.html"))
      output
    }
  )
