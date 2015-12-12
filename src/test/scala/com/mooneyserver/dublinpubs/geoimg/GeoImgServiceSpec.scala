package com.mooneyserver.dublinpubs.geoimg

import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.MissingQueryParamRejection
import akka.stream.scaladsl.Flow
import org.scalatest._

class GeoImgServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with GeoImgApi {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig

  val successfulResponse = PubImage(53.344158, -6.556247, "Base64EncodedImgData", false)

  override lazy val geoImgConnectionFlow = Flow[HttpRequest].map { request =>
    if (request.uri.toString().contains("lat=53.000") && request.uri.toString().contains("lon=-6.123456"))
      HttpResponse(status = OK, entity = marshal(successfulResponse))
    else
      HttpResponse(status = BadRequest, entity = marshal("Shizzle be fucked!"))      
  }

  "GeoImgApi" should "respond with a JSON representation of a Pub Image" in {
    Get(s"/pubimg?lat=53.000&long=-6.123456") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PubImage] shouldBe successfulResponse
    }
  }

  it should "throw a Missing Query Param Rejection if lat query param is missing" in {
    Get(s"/pubimg?long=12.234") ~> routes ~> check {
      handled shouldBe false
      rejection shouldBe MissingQueryParamRejection("lat")
    }
  }
  
  it should "throw a Missing Query Param Rejection if long query param is missing" in {
    Get(s"/pubimg?lat=12.234") ~> routes ~> check {
      handled shouldBe false
      rejection shouldBe MissingQueryParamRejection("long")
    }
  }

  it should "respond with a Service Unavailable for any external service errors" in {
    Get(s"/pubimg?lat=53.123&long=-6.0000") ~> routes ~> check {
      status shouldBe ServiceUnavailable
    }
  }
}
