package com.bj.bank.services

import com.bj.bank.models.{Customer, CustomerReq}

import scala.collection.mutable

class CustomersService(accountsService: AccountsService) {

  val customers: mutable.LinkedHashMap[String, Customer] = mutable.LinkedHashMap.empty

  def create(req: CustomerReq) = {
    val customer = Customer(req)
    customers += (customer.id -> customer)
    val acc = accountsService.create(customer.id, req)
    (customer, acc)
  }

  def getAll() = customers.values

  def get(id: String) = {
    customers.get(id)
  }

}
