package com.bj.bank

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives
import com.bj.bank.models.{Account, AccountReq}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}

import scala.concurrent.ExecutionContext

trait Routes extends Directives with Json4sSupport{

  implicit def executionContext: ExecutionContext

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  val routes = pathPrefix("bank") {
    pathPrefix("accounts"){
      pathEnd {
        post {
          entity(as[AccountReq]) { req =>
            complete(ToResponseMarshallable(Account(req)))
          }
        }
      }
    }
  }

}
