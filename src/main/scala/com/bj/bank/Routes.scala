package com.bj.bank

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import com.bj.bank.models.{AccountReq, CustomerReq, TransferReq}
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}

import scala.concurrent.ExecutionContext

trait Routes extends Directives with Json4sSupport {

  implicit def executionContext: ExecutionContext

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  import akka.http.scaladsl.marshalling.Marshaller._

  implicit def accountsService: AccountsService

  implicit def customersService: CustomersService

  implicit def transfersService: TransfersService

  val routes = pathPrefix("bank") {
    pathPrefix("customers") {
      pathEnd {
        post {
          entity(as[CustomerReq]) { req =>
            complete(ToResponseMarshallable(StatusCodes.Created -> customersService.create(req)))
          }
        } ~
          get {
            complete(ToResponseMarshallable(customersService.getAll()))
          }
      } ~
        path(Segment) { customerId =>
          pathEnd {
            get {
              complete(ToResponseMarshallable(fetchCustomer(customerId)))
            }
          } ~
            pathPrefix("accounts") {
              pathEnd {
                get {
                  complete(ToResponseMarshallable {
                    fetchAllCustomerAccounts(customerId)
                  })
                } ~
                  post {
                    entity(as[AccountReq]) { req =>
                      complete(ToResponseMarshallable(addAccount(customerId, req)))
                    }
                  }
              }
            } ~
            path(Segment) { accNumber =>
              pathEnd {
                get {
                  complete(ToResponseMarshallable {
                    fetchCustomerAccount(customerId, accNumber)
                  })
                }
              }
            }
        }
    } ~
      pathPrefix("transfers") {
        pathPrefix("external") {
          pathEnd {
            get {
              complete(ToResponseMarshallable(transfersService.getAll(internal = false)))
            }
          } ~
            path(Segment) { customerId =>
              pathPrefix("send") {
                pathEnd {
                  post {
                    entity(as[TransferReq]) { req =>
                      complete(ToResponseMarshallable(sendMoney(customerId)(_)))
                    }
                  }
                }
              }
            } ~
            pathPrefix("receive") {
              pathEnd {
                post {
                  entity(as[TransferReq]) { req =>
                    complete(ToResponseMarshallable(receiveMoney _))
                  }
                }
              }
            }
        } ~
          pathPrefix("internal") {
            pathEnd {
              get {
                complete(ToResponseMarshallable(transfersService.getAll(internal = true)))
              } ~
                path(Segment) { customerId =>
                  pathEnd {
                    post {
                      entity(as[TransferReq]) { req =>
                        complete(ToResponseMarshallable(internal(customerId)(_)))
                      }
                    }
                  }
                }
            }
          }
      }
  }

  def internal(customerId: String)(req: TransferReq) = {
    transfersService.internal(customerId, req) match {
      case Some(balance) => balance
      case None => BadRequest
    }
  }


  private def receiveMoney()(req: TransferReq) = {
    transfersService.receive(req) match {
      case Some(balance) => balance
      case None => BadRequest
    }
  }

  private def sendMoney(customerId: String)(req: TransferReq) = {
    transfersService.send(customerId, req) match {
      case Some(balance) => balance
      case None => BadRequest
    }
  }

  private def addAccount(customerId: String, req: AccountReq) = {
    accountsService.add(customerId, req) match {
      case Some(acc) => acc
      case None => NotFound
    }
  }

  private def fetchCustomerAccount(customerId: String, accNumber: String) = {
    accountsService.get(customerId, accNumber) match {
      case Some(account) => account
      case None => NotFound
    }
  }

  private def fetchCustomer(customerId: String) = {
    customersService.get(customerId) match {
      case Some(customer) => customer
      case None => NotFound
    }
  }

  def fetchAllCustomerAccounts(customerId: String) = {
    accountsService.getAllFor(customerId) match {
      case Some(accounts) => accounts.values()
      case None => NotFound
    }
  }
}