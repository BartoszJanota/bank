package com.bj.bank.models

import com.bj.bank.exceptions.{AccountNotFoundEx, CustomerNotFoundEx}

case class TransferReq(toAccNumber: String, toAccName: String, fromAccNumber: String, amount: BigDecimal)

case class TransferRes(fromBalance: BigDecimal, toBalance: BigDecimal)

case class Transfer(transferType: String, status: String, from: String, to: String, balance: Option[BigDecimal], amount: Option[BigDecimal], customerId: String)

object Transfer {
  def transfer(transferType: String, customerId: String, req: TransferReq, ex: Exception): Transfer = {
    ex match {
      case AccountNotFoundEx => Transfer(transferType, "REJECTED_CUSTOMER_ACC_NOT_FOUND", req.fromAccNumber, req.toAccNumber, None, None, customerId)
      case CustomerNotFoundEx => Transfer(transferType, "REJECTED_CUSTOMER_NOT_FOUND", req.fromAccNumber, req.toAccNumber, None, None, customerId)
    }
  }

  def transfer(_type: String, status: String, customerId: String, req: TransferReq, balance: BigDecimal): Transfer =
    Transfer(_type, status, req.fromAccNumber, req.toAccNumber, Some(balance), Some(req.amount), customerId)
}