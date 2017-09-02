package com.bj.bank.services

import java.util.concurrent.ConcurrentHashMap

import com.bj.bank.exceptions.{AccountNotFoundEx, NotEnoughMoneyEx}
import com.bj.bank.models.{Account, AccountReq, CustomerReq}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class AccountsService(implicit val executionContext: ExecutionContext) {
  val accounts: mutable.Map[String, ConcurrentHashMap[String, Account]] = mutable.Map.empty

  def add(customerId: String, req: AccountReq): Option[Account] = {
    val acc = Account(req)
    accounts.get(customerId) match {
      case Some(customerAccounts) => {
        customerAccounts.put(acc.number, acc)
        Some(acc)
      }
      case _ => None


  }

  def getAllFor(customerId: String) = {
    accounts get customerId
  }

  def create(customerId: String, req: CustomerReq) = {
    val customerAccounts = new ConcurrentHashMap[String, Account]()
    val acc = Account(req)
    customerAccounts.put(acc.number, acc)
    accounts += (customerId -> customerAccounts)
    acc
  }

  def update(acc: Account, saldo: BigDecimal) = {
    acc.copy(acc.userName, acc.number, acc.balance)
  }

  def deductMoney(accNumber: String, amount: BigDecimal) = {
    accounts.get(accNumber) match {
      case Some(acc) => {
        val newBalance = acc.balance - amount
        if (newBalance > 0L) {
          accounts.put(accNumber, update(acc, newBalance))
          Right(newBalance)
        }
        else {
          Left(NotEnoughMoneyEx(newBalance))
        }
      }
      case None => Left(AccountNotFoundEx)
    }
  }

  def create(req: AccountReq) = {
    val acc = Account(req)
    accounts += (acc.number -> acc)
    acc
  }

  def getAll() = accounts.values

  def get(customerId: String, accNumber: String) = {
    accounts.get(customerId) match {
      case Some(customerAccounts) =>
        if (customerAccounts.containsKey(accNumber)) {
          customerAccounts.get(accNumber)
        }

      case _ => None
    }
  }

}
