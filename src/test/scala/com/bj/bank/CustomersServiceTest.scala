package com.bj.bank

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.bj.bank.models.{Account, Customer, CustomerReq}
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest._
import org.mockito.Matchers.any

import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.unmarshalling.Unmarshaller._


class CustomersServiceTest extends WordSpec with BeforeAndAfter with Matchers with ScalatestRouteTest with MockitoSugar with Routes {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  override implicit val serialization = native.Serialization
  override implicit def json4sFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  var accountsService: AccountsService = _
  var customersService: CustomersService = _
  var transfersService: TransfersService = _

  before {
    accountsService = mock[AccountsService]
    customersService = new CustomersService(accountsService)
    transfersService = new TransfersService(accountsService)
  }

  val customerReq1 = CustomerReq("Tom Hanks", "tom@hanks.xxx", "USD", 1000)
  val customerReq2 = CustomerReq("Penelope Cruze", "Penelope@Cruze.xxx", "USD", 3000)

  private val account1 = Account(customerReq1)
  private val account2 = Account(customerReq2)


  "The customer service" should {

    "create a customer" in {

      when(accountsService.create(any[String], any[CustomerReq])).thenReturn(account1)

      Post("/bank/customers", customerReq1) ~> routes ~> check {
        status shouldEqual StatusCodes.Created

        val response = responseAs[(Customer, Account)]
        response._1.name shouldBe "Tom Hanks"
        response._2.balance shouldEqual 1000
      }
    }

    "lookup for a given customer" in {

      when(accountsService.create(any[String], any[CustomerReq])).thenReturn(account1)

      val customerId = customersService.create(customerReq1)._1.id

      Get("/bank/customers/" + customerId) ~> routes ~> check {
        status shouldEqual StatusCodes.OK

        val customer = responseAs[Customer]

        customer.name shouldEqual "Tom Hanks"
        customer.id shouldEqual customerId
      }
    }

    "return all bank customers" in {

      when(accountsService.create(any[String], any[CustomerReq])).thenReturn(account1).thenReturn(account2)

      customersService.create(customerReq1)
      customersService.create(customerReq2)

      Get("/bank/customers") ~> routes ~> check {
        status shouldEqual StatusCodes.OK

        val customers = responseAs[List[Customer]]

        customers.size shouldEqual 2
        customers.head.name shouldEqual "Tom Hanks"
        customers.last.name shouldEqual "Penelope Cruze"
      }

    }
  }

}
