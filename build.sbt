import Dependencies._

ThisBuild / scalaVersion := "3.3.7"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / exportPipelining := false

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

lazy val zioInterop =
  project
    .in(file("interop-zio"))
    .dependsOn(core)
    .settings(name := "union-derivation-zio")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      libraryDependencies += Dependencies.dev.zio.`zio-prelude`
    )

lazy val tests =
  project
    .in(file("tests"))
    .dependsOn(core, catsInterop, circeInterop, zioInterop)
    .settings(name := "union-derivation-tests")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .settings(
      Compile / scalacOptions ~= { opts =>
        opts.filterNot(_.contains("wartremover"))
      },
      Test / scalacOptions ~= { opts =>
        opts.filterNot(_.contains("wartremover"))
      }
    )
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

lazy val bench =
  project
    .in(file("bench"))
    .dependsOn(core, catsInterop, circeInterop)
    .settings(name := "union-derivation-bench")
    .settings(commonSettings)
    .settings(
      Compile / scalacOptions ~= { opts =>
        opts.filterNot(_.contains("wartremover"))
      }
    )
    .enablePlugins(JmhPlugin)
    .settings(
      libraryDependencies ++= Seq(
        Dependencies.io.circe.`circe-core`,
        Dependencies.org.typelevel.`cats-core`,
      )
    )

lazy val `unionmirror` =
  project
    .in(file("."))
    .settings(name := "unionmirror")
    .settings(commonSettings)
    .settings(autoImportSettings)
    .aggregate(core, catsInterop, circeInterop, zioInterop, tests)
    .dependsOn(core, catsInterop, circeInterop, zioInterop)

def ltsScalacOptions(opts: Seq[String]): Seq[String] =
  opts.filterNot { opt =>
    opt == "-Ypickle-java" ||
    opt == "-Ypickle-write" ||
    opt == "-Xkind-projector" ||
    opt == "-Wsafe-init" ||
    opt.contains("/early/") ||
    opt.contains("/test-early/") ||
    opt.endsWith("/early.jar")
  }

lazy val commonSettings = {
  lazy val commonScalacOptions =
    Seq(
      Compile / scalacOptions ~= ltsScalacOptions,
      Test / scalacOptions ~= ltsScalacOptions,
      scalacOptions ++= Seq(
        "-Wconf:msg=pattern selector should be an instance of Matchable:silent",
      ),
      Test / scalacOptions ++= Seq(
        "-Wconf:msg=the type test for .* cannot be checked at runtime.*:silent",
        "-Wconf:msg=unused local definition:silent",
      ),
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

lazy val autoImportSettings = {
  val baseImports =
    Seq(
      "java.lang",
      "scala",
      "scala.Predef",
      "scala.annotation",
      "scala.util.chaining",
    )

  Seq(
    scalacOptions += baseImports.mkString(start = "-Yimports:", sep = ",", end = ""),
    Test / scalacOptions ~= (_.filterNot(_.startsWith("-Yimports:"))),
    Test / scalacOptions +=
      (baseImports ++ Seq(
        "org.scalacheck",
        "org.scalacheck.Prop",
      )).mkString(start = "-Yimports:", sep = ",", end = ""),
  )
}

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
