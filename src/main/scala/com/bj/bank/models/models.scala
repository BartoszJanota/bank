package com.bj.bank.models

import java.util.UUID.randomUUID

import com.bj.bank.exceptions.AccountNotFoundEx

case class CustomerReq(userName: String, email: String, accCurrency: String, initialBalance: BigDecimal)

case class Customer(userName: String, email: String, id: String)

object Customer{
  def apply(req: CustomerReq) = new Customer(req.userName, req.email, randomUUID.toString.substring(0, 12))
}

case class AccountReq(accCurrency: String, initialBalance: BigDecimal)

case class Account(number: String, currency: String, balance: BigDecimal)

object Account {
  def apply(req: AccountReq): Account = Account(randomUUID.toString, req.accCurrency, req.initialBalance)

  def apply(req: CustomerReq): Account = Account(randomUUID.toString, req.accCurrency, req.initialBalance)
}

case class TransferSendReq(toAccNumber: String, toAccName: String, amount: BigDecimal)

case class Transfer(status: String, from: String, to: String, balance: Option[BigDecimal], amount: Option[BigDecimal], transferType: String)

object Transfer {
  def outgoing(status: String, from: String, req: TransferSendReq, ex: Exception): Transfer = {
    ex match {
      case AccountNotFoundEx => Transfer("REJECTED_SENDER_ACC_NOT_FOUND", from, req.toAccNumber, None, None, "OUTGOING")
    }
  }

  def outgoing(status: String, from: String, req: TransferSendReq, balance: BigDecimal): Transfer =
    Transfer(status, from, req.toAccNumber, Some(balance), Some(req.amount), "OUTGOING")
}