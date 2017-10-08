package com.bj.bank

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import com.bj.bank.models._
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}
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

  val INITIAL_BALANCE_1 = 1000

  val INITIAL_BALANCE_2 = 3000

  val TRANSFER_AMOUNT = 300

  val customerReq1 = CustomerReq("Tom Hanks", "tom@hanks.xxx", "USD", INITIAL_BALANCE_1)
  val customerReq2 = CustomerReq("Penelope Cruz", "penelope@Cruz.xxx", "USD", INITIAL_BALANCE_2)

  "The transfers API" should {

    "send an external transfer" in {

      val customer = customersService.create(customerReq1)
      val customerId = customer._1.id
      val customerAcc = customer._2.number

      val transferReq1 = TransferReq("1234-5436-3938-22", "Penelope Cruz", customerAcc, TRANSFER_AMOUNT)

      Post("/bank/transfers/external/" + customerId + "/send", transferReq1) ~> routes ~> check {
        status shouldEqual StatusCodes.Created

        val response = responseAs[Balance]

        response.balance shouldEqual (INITIAL_BALANCE_1 - TRANSFER_AMOUNT)
      }
    }

    "receive an external transfer" in {

      val customer = customersService.create(customerReq1)
      val customerAcc = customer._2.number
      val transferReq1 = TransferReq(customerAcc, "Penelope Cruz", "1234-5436-3938-22", TRANSFER_AMOUNT)

      Post("/bank/transfers/external/receive", transferReq1) ~> routes ~> check {
        status shouldEqual StatusCodes.Created

        val response = responseAs[Balance]

        response.balance shouldEqual (INITIAL_BALANCE_1 + TRANSFER_AMOUNT)
      }
    }

    "reject receiving an external transfer if account does not exist" in {

      val fakeAccNum = "535436-345-234-534"

      val transferReq1 = TransferReq(fakeAccNum, "Penelope Cruz", "1234-5436-3938-22", TRANSFER_AMOUNT)

      Post("/bank/transfers/external/receive", transferReq1) ~> routes ~> check {

        status shouldEqual StatusCodes.BadRequest

      }
    }

    "reject a transfer if customer has not enough money" in {

      val customer = customersService.create(customerReq1)
      val customerId = customer._1.id
      val customerAcc = customer._2.number

      val TOO_BIG_TRANSFER = INITIAL_BALANCE_1 + 500

      val transferReq1 = TransferReq("1234-5436-3938-22", "Penelope Cruz", customerAcc, TOO_BIG_TRANSFER)

      Post("/bank/transfers/external/" + customerId + "/send", transferReq1) ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        val response = responseAs[Balance]

        response.balance shouldEqual (INITIAL_BALANCE_1 - TOO_BIG_TRANSFER)
        assert(response.balance < 0)
      }
    }
  }

  "record all transfers" in {

    val customer = customersService.create(customerReq1)
    val customerId = customer._1.id
    val customerAcc = customer._2.number

    val TOO_BIG_TRANSFER = INITIAL_BALANCE_1 + 500

    val correctRequest = TransferReq("1234-5436-3938-22", "Penelope Cruz", customerAcc, TRANSFER_AMOUNT)
    val tooBigTransferRequest = TransferReq("1234-5436-3938-22", "Penelope Cruz", customerAcc, TOO_BIG_TRANSFER)

    transfersService.send(customerId, correctRequest)
    transfersService.send(customerId, tooBigTransferRequest)

    Get("/bank/transfers/external") ~> routes ~> check {
      status shouldEqual StatusCodes.OK

      val response = responseAs[List[Transfer]]

      response.size shouldBe 2
      response.head.status shouldBe "COMPLETED"
      response.last.status shouldBe "REJECTED_NOT_ENOUGH_MONEY"
    }
  }

  "send an internal transfer" in {

    val customer1 = customersService.create(customerReq1)
    val customer2 = customersService.create(customerReq2)

    val customerId1 = customer1._1.id
    val customerAcc1 = customer1._2.number

    val customerAcc2 = customer2._2.number

    val transferReq1 = TransferReq(customerAcc2, "Penelope Cruz", customerAcc1, TRANSFER_AMOUNT)

    Post("/bank/transfers/internal/" + customerId1, transferReq1) ~> routes ~> check {
      status shouldEqual StatusCodes.Created

      val response = responseAs[TransferRes]

      response.fromBalance shouldEqual (INITIAL_BALANCE_1 - TRANSFER_AMOUNT)
      response.toBalance shouldEqual (INITIAL_BALANCE_2 + TRANSFER_AMOUNT)
    }
  }

  "reject an internal transfer if no enough money" in {

    val customer1 = customersService.create(customerReq1)
    val customer2 = customersService.create(customerReq2)

    val customerId1 = customer1._1.id
    val customerAcc1 = customer1._2.number

    val customerAcc2 = customer2._2.number

    val TOO_BIG_TRANSFER = INITIAL_BALANCE_1 + 500

    val transferReq1 = TransferReq(customerAcc2, "Penelope Cruz", customerAcc1, TOO_BIG_TRANSFER)

    Post("/bank/transfers/internal/" + customerId1, transferReq1) ~> routes ~> check {
      status shouldEqual StatusCodes.BadRequest

      val response = responseAs[Balance]

      response.balance shouldEqual (INITIAL_BALANCE_1 - TOO_BIG_TRANSFER)
      assert(response.balance < 0)
    }
  }

  "record an internal transfer" in {

    val customer1 = customersService.create(customerReq1)
    val customer2 = customersService.create(customerReq2)

    val customerId1 = customer1._1.id
    val customerAcc1 = customer1._2.number

    val customerAcc2 = customer2._2.number

    val transferReq1 = TransferReq(customerAcc2, "Penelope Cruz", customerAcc1, TRANSFER_AMOUNT)

    transfersService.internal(customerId1, transferReq1)

    Get("/bank/transfers/internal") ~> routes ~> check {
      status shouldEqual StatusCodes.OK

      val transfers = responseAs[List[Transfer]]

      println(transfers)

      transfers.size shouldBe 2
      transfers.foreach(t => t.transferType shouldBe "INTERNAL")

      transfers.head.status shouldBe "SENT"
      transfers.head.balance.get shouldBe INITIAL_BALANCE_1 - TRANSFER_AMOUNT

      transfers.last.status shouldBe "RECEIVED"
      transfers.last.balance.get shouldBe INITIAL_BALANCE_2 + TRANSFER_AMOUNT
    }
  }

}
