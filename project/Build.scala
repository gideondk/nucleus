import sbt._
import Keys._

import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {
  override lazy val settings = super.settings ++
    Seq(
      name := "nucleus",
      version := "0.1.0",
      organization := "nl.gideondk",
      scalaVersion := "2.10.2",
      parallelExecution in Test := false,
      resolvers ++= Seq(Resolver.mavenLocal,
        "gideondk-repo" at "https://raw.github.com/gideondk/gideondk-mvn-repo/master",
        "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
        "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
        "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      ensimeConfig := sexp(
        key(":compiler-args"), sexp("-Ywarn-dead-code", "-Ywarn-shadowing"),
        key(":formatting-prefs"), sexp(
          key(":alignParameters"), true,
          key(":AlignSingleLineCaseStatements"), true,
          key(":CompactControlReadability"), true,
          key(":CompactStringConcatenation"), true,
          key(":DoubleIndentClassDeclaration"), true,
          key(":IndentLocalDefs"), true,
          key(":IndentPackageBlocks"), true,
          key(":IndentSpaces"), 2,
          key(":MultilineScaladocCommentsStartOnFirstLine"), true,
          key(":PreserveSpaceBeforeArguments"), false,
          key(":PreserveDanglingCloseParenthesis"), false,
          key(":RewriteArrowSymbols"), true,
          key(":SpaceBeforeColon"), false,
          key(":SpaceInsideBrackets"), false,
          key("SpacesWithinPatternBinders"), true
        )
      ),
      publishTo := Some(Resolver.file("file", new File("/Users/gideondk/Development/gideondk-mvn-repo")))
    )

  val appDependencies = Seq(
  "org.scalaz" %% "scalaz-core" % "7.0.0",
    "org.scalaz" %% "scalaz-effect" % "7.0.0",
    "org.specs2" %% "specs2" % "1.13",

    "com.chuusai" % "shapeless_2.10.0" % "1.2.4",

    "com.typesafe.play" %% "play-iteratees" % "2.2-akka22-SNAPSHOT",

    "nl.gideondk" %% "sentinel" % "0.6.0-M2"
  )

  lazy val root = Project(id = "nucleus",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      libraryDependencies ++= appDependencies,
      mainClass := Some("Main")
    ) ++ Format.settings
  )
}

object Format {

  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences
  )

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, true).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(IndentLocalDefs, true).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, false).
      setPreference(RewriteArrowSymbols, true).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

