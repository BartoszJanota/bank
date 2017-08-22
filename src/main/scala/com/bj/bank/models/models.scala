package com.bj.bank.models

import java.util.UUID.randomUUID

case class AccountReq(userName: String)

case class Account(userName: String, number: String, balance: BigDecimal)

object Account{
  def apply(req: AccountReq): Account = Account(req.userName, randomUUID.toString, 0)
}
