/**
 * Copyright (C) 2011 SINTEF <franck.fleurey@sintef.no>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * @author Brice MORIN, SINTEF IKT
 */
package org.thingml.utils.comm

import ch.eth.coap.endpoint.LocalResource
import org.thingml.utils.log.Logger
import ch.eth.coap.coap.{PUTRequest, Response, CodeRegistry, POSTRequest, GETRequest}

class ThingMLCoAPResource(val resourceIdentifier : String = "ThingML", val code : Byte = 0x00, val server : CoAP) extends LocalResource(resourceIdentifier) {

  setResourceTitle("Generic ThingML Resource")
  setResourceType("ThingMLResource")

  def isThingML(payload : Array[Byte]) : Boolean = {
    payload.size == 18 && payload(0) == 0x12 && payload(17) == 0x13 && payload(4) == code
  }

  def parse(payload : String) : Option[Array[Byte]] = {
    try {
      var params = Map[String,  String]()
      payload.split(",").collect{case p => p.trim()}.foreach{p => params += (p.split(":")(0).trim() -> p.split(":")(1).trim())}
      if (checkParams(params)) {
         return doParse(params)
        return None
      } else {
        return None
      }
    } catch {
      case _ => return None
    }
  }
  def doParse(params : Map[String, String]) : Option[Array[Byte]] = None //This should be overridden in message resource generated by ThingML
  def checkParams(params : Map[String, String]) = true//This should be overridden in message resource generated by ThingML to check the number and types of params

  def addSubResource(resource : ThingMLCoAPResource) {
    super.addSubResource(resource)
    server.resourceMap += (resource.code -> resource.getResourcePath)
  }

  //PUT is the only method supported (currently) by ThingML resources
  override def performPUT(request: PUTRequest) {
    Logger.debug("performPUT: " + request.getPayload.mkString ("[", ", ", "]"))
    //Send the payload to the ThingML side
    if (isThingML(request.getPayload)) {
      server.coapThingML.receive(request.getPayload)
      Logger.info("PUT request can be handled: " + request)
    } else {
      parse(request.getPayloadString) match {
        case Some(p) =>
          server.coapThingML.receive(p)
          Logger.info("PUT request can be handled: " + request)
        case None => Logger.warning("PUT request cannot be handled: " + request)
      }
    }

    //Default response, whatever we do with the request
    val response = new Response(CodeRegistry.RESP_CONTENT)
    response.setPayload("OK!")
    request.respond(response)
  }

  override def performPOST(request: POSTRequest) {
    Logger.debug("performPOST: " + request.getPayload.mkString ("[", ", ", "]"))
    Logger.warning("POST not supported")

    //Default response, whatever we do with the request
    val response = new Response(CodeRegistry.RESP_METHOD_NOT_ALLOWED)
    response.setPayload("POST not supported by ThingML resources. Please use PUT")
    request.respond(response)
  }

  override def performGET(request: GETRequest) {
    Logger.debug("performGET: " + request.getPayload.mkString ("[", ", ", "]"))
    Logger.warning("GET not supported")

    //Default response, whatever we do with the request
    val response = new Response(CodeRegistry.RESP_METHOD_NOT_ALLOWED)
    response.setPayload("GET not supported by ThingML resources. Please use PUT")
    request.respond(response)
  }
}