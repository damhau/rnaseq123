/*
 * Copyright (C) 2020 LLAAMA <https://www.llaama.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.llaama.palaamon.workers.rnaseq

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path}

import akka.actor.{Actor, ActorLogging, Props}
import com.llaama.palaamon.core.hashes.CalculateDigest
import com.llaama.palaamon.core.transforms._
import com.llaama.palaamon.core.transforms.cluster.TransformWorker.{WorkerJobFailed, WorkerJobSuccessFul}
import com.llaama.palaamon.core.transforms.cluster.{GetTransfDefId, TransformType, TransformWorker, WorkerErrorMsg, WorkerOutputMsg}
import com.llaama.palaamon.core.utils

import scala.sys.process.ProcessLogger

class Rnaseq123 extends Actor with ActorLogging {

  import Rnaseq123._

  def receive: Receive = {
    case t: TransformFull =>
      log.info(s"transformDef: ${t.transform.definition} defLight=$transfDefId")
      require(t.transform.definition == transfDefId.fullName)

      val transfVisF = TransformFolderVisit(t.transform)

      val countFiles = transfVisF.inputPath.toFile.listFiles()
        .filter(_.getName.endsWith("txt"))

      val startFileContent = buildRStartFile(countFiles, transfVisF.transformLocation)
      log.info(s"R start script= $startFileContent")

      val startFile = transfVisF.transformLocation resolve "start-script"

      (transfVisF.transformLocation resolve "tmp").toFile.mkdir()

      Files.write(startFile, startFileContent.getBytes(StandardCharsets.UTF_8), CREATE)

      startFile.toFile.getAbsolutePath

      val rCmd = Seq("Rscript", startFile.toFile.getAbsolutePath)

      val process = scala.sys.process.Process(rCmd, transfVisF.transformLocation.toFile)

      log.info(s"starting process: $process")

      val output = new StringBuilder
      val error = new StringBuilder

      val status = process.!(ProcessLogger(
        s => {
          output append s
          sender ! WorkerOutputMsg(t.uid, s)
        },
        s => {
          error append s
          sender ! WorkerErrorMsg(t.uid, s)
        }))

      if (status == 0) {
        log.info("r process completed successfully. ")
        sender() ! WorkerJobSuccessFul(s"The RnaSeq123 workflow has been processed correctly...",
          Map("RNASeq123" -> targetHtml))
      } else {
        log.error("r process failed. ")
        sender() ! WorkerJobFailed("The RNASeq123 analysis workflow has failed ",
          (output.toString takeRight 500) ++ (error.toString takeRight 1000))
      }

      log.info(output.toString)
      log.error(error.toString)

      Files.write(transfVisF.logPath resolve "output.log",
        output.toString.getBytes(StandardCharsets.UTF_8), CREATE)

      Files.write(transfVisF.logPath resolve "error.log",
        error.toString.getBytes(StandardCharsets.UTF_8), CREATE)

    case GetTransfDefId(wi) ⇒
      log.debug(s"asking worker type for $wi")
      sender() ! TransformType(wi, transfDefId)

    case msg: Any ⇒ log.error(s"unable to deal with message: $msg")
  }
}


object Rnaseq123 {
  val fullName: FullName = FullName("com.llaama.palaamon.workers.rnaseq", "RNASeq-1-2-3", "rnaseq123",
    "1.0.0",
    CalculateDigest.getDigestAsHex("com.llaama.palaamon.workers.rnaseq@RNASeq-1-2-3@rnaseq123@1.0.0"))

  val transfDefId = TransformDefinitionIdentity(fullName,
    TransformDescription("Implements the whole RNA Seq 1-2-3 workflow (including EdgeR, Glimma, etc.)"
      , "A list of count files (txt extension) as input",
      "It produces a R markdown report including all the steps described in the RNASeq 123 paper",
      properties = Map("url.web" -> "f1000research.com/articles/5-1408/")),
    releaseDate = utils.getAsDate("2019/04/02-09:00:00"))

  val definition = TransformDefinition(transfDefId, props)
  val targetHtml = "rnaseq123.html"

  def props(): Props = Props(classOf[Rnaseq123])

  def buildRStartFile(files: Seq[File], workDir: Path): String = {

    val outputFolder = workDir resolve "output"
    val inputFolder = workDir resolve "input"

    val filesAsString = files.map(_.getAbsolutePath).map(f ⇒ s""" \"$f\" """).mkString("c(", " ,", ")")
    val fileNamesAsString = files.map(_.getName).map(f ⇒ s""" \"$f\" """).mkString("c(", " ,", ")")

    s"""
       |setwd("${workDir}")
       |library(rmarkdown)
       |files <- ${filesAsString}
       |fileNames <- ${fileNamesAsString}
       |OutputPath <- "${outputFolder.toString}"
       |inputPath <- "${inputFolder.toString}"
       |render("/opt/docker/R/rnaseq123md.Rmd", run_pandoc=T, intermediates_dir="${workDir.toString}/temp", output_dir="${outputFolder.toString}", output_file="$targetHtml", quiet = F, encoding = getOption("encoding"))
    """.stripMargin
  }

}
