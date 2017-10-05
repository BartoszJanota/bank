package com.bj.bank

import com.bj.bank.services.{AccountsService, CustomersService, TransfersService}

import scala.concurrent.ExecutionContext.Implicits.global

trait TestServices {

  implicit val accountsService: AccountsService = new AccountsService()

  implicit val customersService: CustomersService = new CustomersService(accountsService)

  implicit val transfersService: TransfersService = new TransfersService(accountsService)
}
