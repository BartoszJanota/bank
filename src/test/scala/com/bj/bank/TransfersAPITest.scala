package com.bj.bank

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import com.bj.bank.models._
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec, _}

import scala.concurrent.ExecutionContextExecutor


class TransfersAPITest extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with MockitoSugar with Routes {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  override implicit val serialization = native.Serialization
  override implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  var accountsService: AccountsService = _
  var customersService: CustomersService = _
  var transfersService: TransfersService = _

  before {
    accountsService = new AccountsService()
    customersService = new CustomersService(accountsService)
    transfersService = new TransfersService(accountsService)
  }

  val customerReq1 = CustomerReq("Tom Hanks", "tom@hanks.xxx", "USD", 1000)

  "The transfers API" should {

    "send an external transfer" in {

      val customer = customersService.create(customerReq1)

      val customerId = customer._1.id

      val customerAcc = customer._2.number

      val transferReq1 = TransferReq("1234-5436-3938-22", "Penelope Cruz", customerAcc, 300)


      Post("/bank/transfers/external/" + customerId + "/send", transferReq1) ~> routes ~> check {
        status shouldEqual StatusCodes.Created

        val response = responseAs[(Balance)]

        response.balance shouldEqual (1000 - 300)
      }
    }
  }

}
