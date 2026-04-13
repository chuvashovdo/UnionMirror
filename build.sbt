import Dependencies._

ThisBuild / scalaVersion := "3.8.1"

lazy val core =
  project
    .in(file("core"))
    .settings(name := "union-derivation-core")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      libraryDependencies += "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "provided"
    )

lazy val catsInterop =
  project
    .in(file("interop-cats"))
    .dependsOn(core)
    .settings(name := "union-derivation-cats")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      libraryDependencies += Dependencies.org.typelevel.`cats-core`
    )

lazy val circeInterop =
  project
    .in(file("interop-circe"))
    .dependsOn(core)
    .settings(name := "union-derivation-circe")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      libraryDependencies += Dependencies.io.circe.`circe-core`
    )

lazy val tests =
  project
    .in(file("tests"))
    .dependsOn(core, catsInterop, circeInterop)
    .settings(name := "union-derivation-tests")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.com.eed3si9n.expecty.expecty,
        Dependencies.org.scalacheck.scalacheck,
        Dependencies.org.scalameta.`munit-scalacheck`,
        Dependencies.org.scalameta.munit,
        Dependencies.org.typelevel.`discipline-munit`,
        Dependencies.dev.zio.`zio-prelude`,
      )
    )

lazy val `unionmirror` =
  project
    .in(file("."))
    .settings(name := "unionmirror")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .aggregate(core, catsInterop, circeInterop, tests)
    .dependsOn(core, catsInterop, circeInterop)

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
      org.typelevel.`cats-core`,
      Dependencies.io.circe.`circe-core`,
    ),
    libraryDependencies ++= Seq(
      com.eed3si9n.expecty.expecty,
      org.scalacheck.scalacheck,
      org.scalameta.`munit-scalacheck`,
      org.scalameta.munit,
      org.typelevel.`discipline-munit`,
      dev.zio.`zio-prelude`,
    ).map(_ % Test),
  )
