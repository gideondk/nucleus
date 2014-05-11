resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers ++= Seq(
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.5.1")