package com.bj.bank.models

import java.util.UUID.randomUUID

case class CustomerReq(name: String, email: String, accCurrency: String, initialBalance: BigDecimal)

case class Customer(name: String, email: String, id: String)

object Customer{
  def apply(req: CustomerReq) = new Customer(req.name, req.email, randomUUID.toString.substring(0, 12))
}