package com.bj.bank

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import com.bj.bank.models.{AccountReq, CustomerReq, TransferSendReq}
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}

import scala.concurrent.ExecutionContext

trait Routes extends Directives with Json4sSupport {

  implicit def executionContext: ExecutionContext

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  val accountsService = new AccountsService()

  val customersService = new CustomersService(accountsService)

  val transfersService = new TransfersService(accountsService)

  val routes = pathPrefix("bank") {
    pathPrefix("customers") {
      pathEnd {
        post {
          entity(as[CustomerReq]) { req =>
            complete(ToResponseMarshallable(customersService.create(req)))
          }
        } ~
          get {
            complete(ToResponseMarshallable(customersService.getAll()))
          }
      } ~
        path(Segment) { customerId =>
          pathEnd {
            get {
              complete(ToResponseMarshallable {
                fetchCustomer(customerId)
              })
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
        pathEnd {
          get {
            complete(ToResponseMarshallable(""))
          }
        } ~
          path(Segment) { accNumber =>
            pathPrefix("send") {
              pathEnd {
                post {
                  entity(as[TransferSendReq]) { req =>
                    complete(ToResponseMarshallable(transfersService.send(accNumber)))
                  }
                }
              }
            } ~
              pathPrefix("receive") {
                pathEnd {
                  post {
                    entity(as[TransferReceiveReq]) { req =>
                      complete("")
                    }
                  }
                }
              }
          }
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
      case Some(accounts) => accounts
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
