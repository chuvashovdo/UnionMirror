import Dependencies._

ThisBuild / scalaVersion := "3.8.1"

lazy val `unionmirror` =
  project
    .in(file("."))
    .settings(name := "unionmirror")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(dependencies)

lazy val commonSettings = {
  lazy val commonScalacOptions =
    Seq(
      Compile / console / scalacOptions := {
        (Compile / console / scalacOptions)
          .value
          .filterNot(_.contains("wartremover"))
          .filterNot(Scalac.Lint.toSet)
          .filterNot(Scalac.FatalWarnings.toSet) :+ "-Wconf:any:silent"
      },
      Test / console / scalacOptions :=
        (Compile / console / scalacOptions).value,
    )

  lazy val otherCommonSettings =
    Seq(
      update / evictionWarningOptions := EvictionWarningOptions.empty
      // cs launch scalac:3.3.1 -- -Wconf:help
      // src is not yet available for Scala3
      // scalacOptions += s"-Wconf:src=${target.value}/.*:s",
    )

  Seq(
    commonScalacOptions,
    otherCommonSettings,
  ).reduceLeft(_ ++ _)
}

lazy val autoImportSettings =
  Seq(
    scalacOptions +=
      Seq(
        "java.lang",
        "scala",
        "scala.Predef",
        "scala.annotation",
        "scala.util.chaining",
      ).mkString(start = "-Yimports:", sep = ",", end = ""),
    Test / scalacOptions +=
      Seq(
        "org.scalacheck",
        "org.scalacheck.Prop",
      ).mkString(start = "-Yimports:", sep = ",", end = ""),
  )

lazy val dependencies =
  Seq(
    libraryDependencies ++= Seq(
      // main dependencies
      org.typelevel.`cats-core`,
      Dependencies.io.circe.`circe-core`,
    ),
    libraryDependencies ++= Seq(
      com.eed3si9n.expecty.expecty,
      org.scalacheck.scalacheck,
      org.scalameta.`munit-scalacheck`,
      org.scalameta.munit,
      org.typelevel.`discipline-munit`,
      // Typeclass libraries for testing
      dev.zio.`zio-prelude`,
    ).map(_ % Test),
  )
