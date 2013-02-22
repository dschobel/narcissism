import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "Narcissism"

version := "ALPHA"

scalaVersion := "2.10.0"

resolvers += "twitter-repo" at "http://maven.twttr.com"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps")

libraryDependencies ++= Seq("com.twitter" % "finagle-core" % "1.9.0",
                            "com.twitter" % "finagle-http" % "1.9.0", 
                            "com.twitter" % "ostrich" % "9.1.0", 
                            "oauth.signpost" % "signpost-core" % "1.2",
                            "oauth.signpost" % "signpost-commonshttp4" % "1.2", 
                            "org.apache.httpcomponents" % "httpclient" % "4.2",
                            "org.squeryl" %% "squeryl" % "0.9.5-6",
                            "postgresql" % "postgresql" % "8.4-701.jdbc4",
                            "org.scalatest" % "scalatest_2.10.0" % "2.0.M5" % "test")
