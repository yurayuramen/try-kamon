name := """try-kamon"""
organization := "com.example"

version := "latest"


lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
jdbc
,guice
,filters
,ws

,"com.typesafe.slick" %% "slick-codegen" % "3.2.0"
,"com.typesafe.play" %% "play-slick" % "3.0.0"
,"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test
,"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.9"
//kamon
,"io.kamon" %% "kamon-core" % "0.6.7"
,"io.kamon" %% "kamon-play-2.6" % "0.6.8"
,"io.kamon" %% "kamon-akka-2.5" % "0.6.8"
,"io.kamon" %% "kamon-fluentd" % "0.6.7"
,"io.kamon" %% "kamon-system-metrics" % "0.6.7"
,"io.kamon" %% "kamon-akka-http" % "0.6.8"

// 標準出力にmetricsを出力したいときにコメント解除
// デバッグ,開発目的で使用すべきものでプロダクションでの運用は避けたほうがよい
// https://github.com/kamon-io/kamon-log-reporter
//,"io.kamon" %% "kamon-log-reporter" % "0.6.8"
// jmxでmetricsをみたいときにコメント解除
//,"io.kamon" %% "kamon-jmx" % "0.6.7"
)

//aspectjSettings



/*
windows固有の問題(環境変数に割り当てた文字数が長すぎるとエラー「入力文字数が長すぎる」)に対応
参考
http://qiita.com/qr_taka/items/bf3cbfbb70a7de968be9
*/
lazy val isDev = false

import com.typesafe.sbt.packager.Keys.scriptClasspath

scriptClasspath := {
  val originalClasspath = scriptClasspath.value
  val manifest = new java.util.jar.Manifest()
  manifest.getMainAttributes().putValue("Class-Path", originalClasspath.mkString(" "))
  val classpathJar = (target in Universal).value / "lib" / "classpath.jar"
  IO.jar(Seq.empty, classpathJar, manifest)
  //フォルダbinの内容を丸ごとjar化したものをclasspathの先頭に持ってくる
  if(isDev)
  "bin.jar" +: List(classpathJar.getName)
  else
  Seq(classpathJar.getName)
}
mappings in Universal += (((target in Universal).value / "lib" / "classpath.jar") -> "lib/classpath.jar")

pipelineStages := Seq(digest, gzip)

/*
aspectjSettings
*/
// Here we are effectively adding the `-javaagent` JVM startup
// option with the location of the AspectJ Weaver provided by
// the sbt-aspectj plugin.
//javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj

// We need to ensure that the JVM is forked for the
// AspectJ Weaver to kick in properly and do it's magic.
fork in run := true

