package com.mooneyserver.dublinpubs.geoimg

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{Future, ExecutionContextExecutor}

import com.typesafe.config.Config


/**
 * The Geolocation Pub Image Service
 */
trait GeoImgApi extends GeoImgProtocol {
  
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  
  def config: Config
  
  lazy val geoImgConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnectionTls(config.getString("services.nasaApiIpOrHost"), config.getInt("services.nasaApiPort"))
   
  lazy val nasaImagePath = s"/planetary/earth/imagery?lat={1}&lon={2}&api_key=${config.getString("services.nasaApiKey")}"
    
  def nasaImageApiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(geoImgConnectionFlow).runWith(Sink.head)
  
  def getImageFrom3rdPartyProvider(lat: String, long: String): Future[Either[RequestFailed, PubImage]] = {
    nasaImageApiRequest(RequestBuilding.Get(nasaImagePath.replace("{1}", lat).replace("{2}", long))).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[PubImage].map(Right(_))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          Future.successful(Left(RequestFailed(s"Nasa Image request failed with status code ${response.status} and body $entity")))
        }
      }
    }
  }
  
  /**
   * REST API definition:
   *  GET /pubimg?lat=50.00&long=-6.00
   */
  val routes = {
    logRequestResult("pub-img-rest-svc") {
      pathPrefix("pubimg") {
        (get & parameters('lat, 'long)) { 
          (lat, long) => {
            complete {
              getImageFrom3rdPartyProvider(lat, long).map[ToResponseMarshallable] {
                case Right(pubImage) => pubImage
                case Left(reqFailed) => ServiceUnavailable -> reqFailed.reason
              }
            }
          }
        }
      }
    }
  }
}