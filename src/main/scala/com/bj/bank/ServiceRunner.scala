package com.bj.bank

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object ServiceRunner extends App with Routes {

  val config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem("quiz-management-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(30 seconds)

  private val host: String = config.getString("http.host")
  private val port: Int = config.getInt("http.port")

  implicit val accountsService: AccountsService = new AccountsService()

  implicit val customersService: CustomersService = new CustomersService(accountsService)

  implicit val transfersService: TransfersService = new TransfersService(accountsService)

  Http().bindAndHandle(handler = routes, interface = host, port = port) map { binding =>
    println(s"Service bound to ${binding.localAddress}")
  } recover { case ex =>
    println(s"Service could not bind to ${host}:${port}", ex.getMessage)
  }

}
