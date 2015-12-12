package com.mooneyserver.dublinpubs.geoimg

import spray.json.DefaultJsonProtocol

/**
 * Response Types for the Geolocation Pub Image API
 */
case class RequestFailed(reason: String)
case class PubImage(latitude: Double, logitude: Double, data: String, cloudCovered: Boolean)

/**
 * Provide JSON serialisation for response types
 */
trait GeoImgProtocol extends DefaultJsonProtocol {
  implicit val pubImageFormat = jsonFormat4(PubImage)
}