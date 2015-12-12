package com.mooneyserver.dublinpubs.geoimg

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.Http

/**
 * Simple web service to get
 * images for the pubs location from some
 * third party img service
 */
object GeoImgService extends App with GeoImgApi {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}