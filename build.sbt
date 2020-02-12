import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.docker.Cmd
import sbt.Resolver

organization := "com.llaama.palaamon.rnaseq"

name := "rnaseq123"

version := "1.1.2"

scalaVersion := "2.12.10"

crossScalaVersions := Seq(scalaVersion.value, "2.13.1")

scmInfo := Some(
  ScmInfo(
    url("https://llaama@bitbucket.org/llaamco/llaama-worker-example/browse"),
    "scm:ssh://git@bitbucket.org:llaamco/llaama-worker-example.git",
    Some("scm:git:git@bitbucket.org:llaamco/llaama-worker-example.git")
  )
)

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.llaama.com", "llaa-sbt", "hdaz74Eritu")

publishMavenStyle := true

publishTo := {
  val nexus = "https://nexus.llaama.com/repository/"
  if (version.value.toString.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "maven-snapshots")
  else
    Some("releases" at nexus + "maven-releases")
}

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.bintrayRepo("typesafe", "maven-releases"),
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("public"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  MavenRepository("mvn-repository", "https://mvnrepository.com/artifact/"),
  MavenRepository("nexus-releases", "https://nexus.llaama.com/repository/maven-releases/"),
  MavenRepository("nexus-snapshots", "https://nexus.llaama.com/repository/maven-snapshots/"),
  Resolver.bintrayRepo("tanukkii007", "maven")) //todo just to make sure it can retrieve lib but it should be removed!

// These options will be used for *all* versions.
scalacOptions ++= Seq(
  "-deprecation"
  , "-unchecked"
  , "-encoding", "UTF-8"
  , "-Xlint"
  //  , "-Yclosure-elim"
  //  , "-Yinline"
  , "-Xverify"
  , "-feature"
  , "-language:postfixOps"
)

libraryDependencies ++= {
  val akkaVersion = "2.6.0"
  val akkaHttpVersion = "10.1.10"
  val akkaManagementVersion = "1.0.4"

  Seq(
    "com.llaama.palaamon" %% "palaamon-core" % "2.1.6",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "commons-io" % "commons-io" % "2.5",
    "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
    "org.specs2" %% "specs2-core" % "3.8.9" % "test",
    "com.twitter" %% "chill" % "0.9.4" % "runtime",
    "com.twitter" %% "chill-akka" % "0.9.4" % "runtime",
    "org.scalactic" %% "scalactic" % "3.0.1",
    "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion, //is a transitive dep from previous, but better to fix version
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
    "io.kamon" %% "kamon-bundle" % "2.0.4",
    "io.kamon" %% "kamon-apm-reporter" % "2.0.0",
    "org.scalatest" %% "scalatest" % "3.0.7" % "test")
}

// which one is really needed?
enablePlugins(JavaServerAppPackaging)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(DockerSpotifyClientPlugin)

mainClass in Compile := Some("com.llaama.palaamon.workers.rnaseq.Main")

mappings in Universal ++= {
  directory("scripts") ++ // copy configuration files to config directory
    contentOf("src/main/R").toMap.mapValues("R/" + _)
}

//we wipe out all previous settings of docker commands
dockerCommands := Seq(
  Cmd("FROM", "bioconductor/release_core2:R3.5.3_Bioc3.8"),
  Cmd("MAINTAINER", "Bernard Deffarges bernard@llaama.com"),
  Cmd("ENV", "LC_ALL en_US.UTF-8"),
  Cmd("ENV", "LANG en_US.UTF-8"),
  Cmd("RUN", "echo Europe/Berlin > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata"),
  Cmd("RUN", "addgroup -gid 987654 palaamon"),
  Cmd("RUN", "useradd --home-dir /home/palaamon --uid 987654 --gid 987654 palaamon"),
  Cmd("RUN", "mkdir /home/palaamon && chown palaamon:palaamon /home/palaamon"),
  Cmd("RUN", "uname -or"), //some info about the distro
  Cmd("RUN", "cat /etc/*-release"),
  Cmd("RUN", """apt-get update && apt-get install -y default-jdk"""),
  Cmd("RUN", "/var/lib/dpkg/info/ca-certificates-java.postinst configure"),
  Cmd("RUN",
    """apt-get update && apt-get install -y --no-install-recommends \
      |ed less locales vim-tiny wget ca-certificates fonts-texgyre apt-utils  \
      |&& rm -rf /var/lib/apt/lists/*""".stripMargin),
  Cmd("RUN",
    """echo "en_US.UTF-8 UTF-8" >> \
      |/etc/locale.gen && locale-gen en_US.utf8 \
      |&& /usr/sbin/update-locale LANG=en_US.UTF-8""".stripMargin),
  Cmd("ARG", "DEBIAN_FRONTEND=noninteractive"),
  Cmd("RUN", """apt-get update && apt-get install -y libudunits2-dev"""),
  Cmd("RUN", """apt-get update && apt-get install -y haskell-platform"""),
  Cmd("RUN", """apt-get update && apt-get install -y texlive"""),
  Cmd("RUN", """apt-get update && apt-get install -y pandoc"""),
  Cmd("RUN", """apt-get update && apt-get install -y littler"""),
  Cmd("RUN", """apt-get update && apt-get install -y libcurl4-openssl-dev"""),
  Cmd("RUN", """apt-get update && apt-get install -y libcairo2-dev"""),
  Cmd("RUN", """apt-get update && apt-get install -y libxml2 libxml2-dev"""),
  Cmd("RUN", """apt-get update && apt-get install -y libmariadbclient-dev libmariadb-client-lgpl-dev"""),
  //  Cmd("RUN", """apt-get update && apt-get install libssl-dev"""), // does not seem to be needed !
  Cmd("RUN",
    """R -e "install.packages(c('MASS', 'Matrix', 'lattice', 'foreign', \
      |'maptools', 'regeos', 'KernSmooth'), dependencies=TRUE)"
      |""".stripMargin),
  Cmd("RUN",
    """R -e "install.packages(c('readxl', 'DT', 'RColorBrewer', \
      |'gplots', 'locfit', 'survival'), dependencies=TRUE)"
      |""".stripMargin),
  Cmd("RUN",
    """R -e "install.packages(c('class', 'biomartr', 'parallel','rmarkdown', \
      |'prettydoc', 'plyr', 'ggplot2', 'reshape2', \
      |'corrplot', 'grodExtra'), dependencies=TRUE)"
      |""".stripMargin),
  Cmd("RUN",
    """R -e "if (!requireNamespace('BiocManager')) install.packages('BiocManager')"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install()"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install('limma', version = '3.8')"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install('Glimma', version = '3.8')"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install('edgeR', version = '3.8')"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install('Mus.musculus', version = '3.8')"
    """.stripMargin),
  Cmd("RUN",
    """R -e "BiocManager::install('RNAseq123', version = '3.8')"
    """.stripMargin),
  Cmd("RUN",//todo needed ??
    """R -e "BiocManager::install()"
    """.stripMargin),
  Cmd("WORKDIR", "/opt/docker"),
  Cmd("COPY", "opt /opt"),
  Cmd("RUN", "chown -R palaamon:palaamon ."),
  Cmd("USER", "palaamon"),
  Cmd("ENTRYPOINT", "bin/rnaseq123"))

//dockerRepository := Some("docker.llaama.com")
dockerRepository := Some("docker.k8stest.llaama.com")

dockerAlias := DockerAlias(dockerRepository.value, Some("palaamon"), packageName.value, Some(version.value))

dockerExposedPorts := Seq(2600)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

// bashScriptExtraDefines += """addJava "-Dconfig.resource=$CONF""""
bashScriptExtraDefines += """addJava "-Dconfig.file=/opt/docker/config/cluster-client.conf""""


