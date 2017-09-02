package com.bj.bank.services

import com.bj.bank.models.{Transfer, TransferSendReq}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class TransfersService(accountsService: AccountsService)(implicit val executionContext: ExecutionContext) {

  val transfers: mutable.MutableList[Transfer] = mutable.MutableList.empty

  def send(accNumber: String)(req: TransferSendReq) = {
    accountsService.deductMoney(accNumber, req.amount) match {
      case Right(balance) =>
        transfers += Transfer.outgoing("COMPLETED", accNumber, req, balance)
      case Left(ex: Exception) =>
        transfers += Transfer.outgoing("REJECTED", accNumber, req, ex)
    }
  }

}