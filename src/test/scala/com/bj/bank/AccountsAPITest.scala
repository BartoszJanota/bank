package com.bj.bank

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import com.bj.bank.models.{Account, AccountReq, CustomerReq}
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec, _}

import scala.concurrent.ExecutionContextExecutor


class AccountsAPITest extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with MockitoSugar with Routes {

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

  val customerReq = CustomerReq("Tom Hanks", "tom@hanks.xxx", "USD", 1000)

  val accountReq1 = AccountReq("SEK", 1234)
  val accountReq2 = AccountReq("GBP", 4321)

  "The accounts API" should {

    "create an account for an existing customer" in {

      val customerId = customersService.create(customerReq)._1.id

      Post("/bank/customers/" + customerId + "/accounts", accountReq1) ~> routes ~> check {
        //status shouldEqual StatusCodes.Created

        val account = responseAs[Account]

        account.currency shouldBe "SEK"
        account.balance shouldEqual 1234
      }
    }

    "lookup for an account for a newly created customer" in {

      val customerId = customersService.create(customerReq)._1.id

      Get("/bank/customers/" + customerId + "/accounts") ~> routes ~> check {
        status shouldEqual StatusCodes.OK

        val accounts = responseAs[List[Account]]

        accounts.size shouldEqual 1
        accounts.head.balance shouldEqual 1000
        accounts.head.currency shouldBe "USD"
      }
    }

    "lookup for a given account" in {

      val customerId = customersService.create(customerReq)._1.id

      val account = accountsService.add(customerId, accountReq1).get

      Get("/bank/customers/" + customerId + "/accounts/" + account.number) ~> routes ~> check {
        status shouldEqual StatusCodes.OK

        val fetchedAcc = responseAs[Account]

        fetchedAcc.number shouldEqual account.number
        fetchedAcc.balance shouldEqual accountReq1.initialBalance
      }
    }

    "not lookup for a non-existing account" in {

      val customerId = customersService.create(customerReq)._1.id

      val nonExistingAccNum: String = "123403-21312-32223-99"

      Get("/bank/customers/" + customerId + "/accounts/" + nonExistingAccNum) ~> routes ~> check {

        status shouldEqual StatusCodes.NotFound

      }
    }

    "lookup for all customer's accounts" in {

      val customerId = customersService.create(customerReq)._1.id

      val account1 = accountsService.add(customerId, accountReq1).get

      val account2 = accountsService.add(customerId, accountReq2).get

      Get("/bank/customers/" + customerId + "/accounts") ~> routes ~> check {
        status shouldEqual StatusCodes.OK

        val accounts = responseAs[List[Account]]

        accounts.size shouldEqual 3

        val accountsNums = accounts.map(acc => acc.number).toSet

        accountsNums.should(Matchers.contain(account1.number))
        accountsNums.should(Matchers.contain(account2.number))
      }
    }
  }

}
