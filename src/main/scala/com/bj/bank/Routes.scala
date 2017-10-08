package com.bj.bank

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives
import com.bj.bank.exceptions.{InternalException, NotEnoughMoneyEx}
import com.bj.bank.models.{AccountReq, Balance, CustomerReq, TransferReq}
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
              complete(fetchCustomer(customerId))
            }
          }
        } ~
        path(Segment / "accounts") { customerId =>
          pathEnd {
            get {
              complete(fetchAllCustomerAccounts(customerId))
            } ~
              post {
                entity(as[AccountReq]) { req =>
                  complete(ToResponseMarshallable(addAccount(customerId, req)))
                }
              }
          }
        } ~
        path(Segment / "accounts" / Segment) { (customerId, accNumber) =>
          pathEnd {
            get {
              complete(fetchCustomerAccount(customerId, accNumber))
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
            path(Segment / "send") { customerId =>
              pathEnd {
                post {
                  entity(as[TransferReq]) { req =>
                    complete(sendMoney(customerId, req))
                  }
                }
              }
            } ~
            pathPrefix("receive") {
              pathEnd {
                post {
                  entity(as[TransferReq]) { req =>
                    complete(receiveMoney(req))
                  }
                }
              }
            }
        } ~
          pathPrefix("internal") {
            pathEnd {
              get {
                complete(ToResponseMarshallable(transfersService.getAll(internal = true)))
              }
            }~
            path(Segment) { customerId =>
              pathEnd {
                post {
                  entity(as[TransferReq]) { req =>
                    complete(internal(customerId, req))
                  }
                }
              }
            }
          }
      }
  }

  def internal(customerId: String, req: TransferReq): ToResponseMarshallable = {
    transfersService.internal(customerId, req) match {
      case Right(transferRes) => Created -> transferRes
      case Left(ex: NotEnoughMoneyEx) => BadRequest -> Balance(ex.message)
      case Left(ex) => BadRequest -> ex.message
    }
  }


  private def receiveMoney(req: TransferReq): ToResponseMarshallable = {
    transfersService.receive(req) match {
      case Right(balance) => Created -> Balance(balance)
      case Left(ex: InternalException) =>
        print(ex)
        BadRequest -> ex.message
    }
  }

  private def sendMoney(customerId: String, req: TransferReq): ToResponseMarshallable = {
    transfersService.send(customerId, req) match {
      case Right(balance: BigDecimal) => Created -> Balance(balance)
      case Left(ex: NotEnoughMoneyEx) => BadRequest -> Balance(ex.message)
      case Left(ex: InternalException) => BadRequest -> ex.message
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
      case Some(account) => OK -> account
      case None => NotFound -> "Couldn't find a given account"
    }
  }

  private def fetchCustomer(customerId: String): ToResponseMarshallable = {
    customersService.get(customerId) match {
      case Some(customer) => OK -> customer
      case None => NotFound -> "Couldn't find a given customer"
    }
  }

  def fetchAllCustomerAccounts(customerId: String): ToResponseMarshallable = {
    accountsService.getAllFor(customerId) match {
      case Some(accounts) => OK -> accounts.values()
      case None => NotFound -> "Couldn't find any customers"
    }
  }
}