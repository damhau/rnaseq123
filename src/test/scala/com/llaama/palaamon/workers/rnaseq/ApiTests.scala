package com.llaama.palaamon.workers.rnaseq

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.llaama.palaamon.core.api.protocols.TransformJsonProt
import com.llaama.palaamon.core.transforms.{TransformOutcome, TransformState}
import com.llaama.palaamon.core.utils.{ExperimentalDesign, FileInformation, Owner, User}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.Eventually
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.Future

/**
  * palaamon-bcl2fastq
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  * Created by Bernard Deffarges on 2017/07/26.
  *
  */
trait ApiTests extends AsyncFlatSpec with Matchers with TransformJsonProt with LazyLogging with Eventually {

  val person1 = User("NGS", "WorldGenius", "wogenius")
  val organization = "com.llaama.palaamon.research.mps.nextseq"
  val owner1 = Owner(organization, person1)

  protected var transStatus: Map[String, Boolean] = Map.empty

  protected var filesInfo: Set[FileInformation] = Set.empty

  val expDesign = ExperimentalDesign("Design for NextSeqTest", Set.empty)

  val config = ConfigFactory.parseString(
    s"""
      http {
        host = palaamon-api-edge.llaama.com
        port = 80
      }
    """).withFallback(ConfigFactory.load())


  val host: String = config.getString("http.host")
  val port: Int = config.getInt("http.port")

  val urlPrefix = "/api"

  implicit var system: ActorSystem = null //todo fix
  implicit var materializer: ActorMaterializer = null

  override def withFixture(test: NoArgAsyncTest) = {
    system = ActorSystem()
    materializer = ActorMaterializer()
    complete {
      super.withFixture(test) // Invoke the test function
    } lastly {
      system.terminate()
    }
  }

  def getAllFilesForExperiment(exp: String): Unit = {
    println(s"checking the raw files... ${filesInfo.size}")

    implicit val executionContext = system.dispatcher

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection(host, port)

    val responseFuture: Future[HttpResponse] =
      Source.single(HttpRequest(uri = s"$urlPrefix/transforms/files"))
        .via(connectionFlow).runWith(Sink.head)

    import spray.json._

    responseFuture.map { r ⇒
      if (r.status == StatusCodes.OK) {
        try {
          filesInfo = r.entity.asInstanceOf[HttpEntity.Strict].data.decodeString("UTF-8")
            .parseJson.convertTo[Set[FileInformation]]
        } catch {
          case exc: Exception ⇒ println(exc)
        }

      }
    }
  }

  def checkTransformStatus(transf: String): Unit = {
    implicit val executionContext = system.dispatcher

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection(host, port)

    val responseFuture: Future[HttpResponse] =
      Source.single(HttpRequest(uri = s"$urlPrefix/transforms/outcome/${transf}"))
        .via(connectionFlow).runWith(Sink.head)

    import spray.json._

    println(transStatus)

    responseFuture.map { r ⇒
      if (r.status == StatusCodes.OK) {
        val fb = r.entity.asInstanceOf[HttpEntity.Strict].data.decodeString("UTF-8")
          .parseJson.convertTo[TransformOutcome]
        transStatus += transf -> (fb.finalState == TransformState.SUCCEEDED)
      }
    }
  }
}
