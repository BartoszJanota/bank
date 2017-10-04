package com.bj.bank.models

import java.util.UUID.randomUUID

case class AccountReq(accCurrency: String, initialBalance: BigDecimal)

case class Account(number: String, currency: String, balance: BigDecimal)

object Account {
  def apply(req: AccountReq): Account = Account(randomUUID.toString, req.accCurrency, req.initialBalance)

  def apply(req: CustomerReq): Account = Account(randomUUID.toString, req.accCurrency, req.initialBalance)

  def updateBalance(acc: Account, newBalance: BigDecimal) = {
    acc.copy(acc.number, acc.currency, newBalance)
  }

  def addAmount(acc: Account, amount: BigDecimal) = {
    acc.copy(acc.number, acc.currency, acc.balance + amount)
  }
}
