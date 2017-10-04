package com.bj.bank.services

import com.bj.bank.models.Transfer._
import com.bj.bank.models.{Transfer, TransferReq, TransferRes}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class TransfersService(accountsService: AccountsService)(implicit val executionContext: ExecutionContext) {
  val transfers: mutable.MutableList[Transfer] = mutable.MutableList.empty

  def getAll(internal: Boolean) = transfers.filter(t => t.transferType.equals("INTERNAL") == internal)

  def internal(customerId: String, req: TransferReq) = {
    accountsService.lookupInternalSender(customerId, req.fromAccNumber) match {
      case Some(fromCustomerId) =>
        accountsService.lookupAccount(req.toAccNumber) match {
          case Some(toCustomerId) =>
            accountsService.transfer(fromCustomerId, toCustomerId, req) match {
              case Right(balance: (BigDecimal, BigDecimal)) =>
                transfers += transfer("INTERNAL", "SENT", fromCustomerId, req, balance._1)
                transfers += transfer("INTERNAL", "RECEIVED", toCustomerId, req, balance._2)
                TransferRes(balance._1, balance._2)
              case Left(ex) =>
                transfers += transfer("INTERNAL", customerId, req, ex)
                None
            }
          case None => None
        }
      case None => None
    }
  }

  def send(customerId: String, req: TransferReq) = {
    accountsService.deductMoney(customerId, req.fromAccNumber, req.amount) match {
      case Right(balance) =>
        transfers += transfer("OUTGOING", "COMPLETED", customerId, req, balance)
        Some(balance)
      case Left(ex: Exception) =>
        transfers += transfer("OUTGOING", customerId, req, ex)
        None
    }
  }

  def receive(req: TransferReq) = {
    accountsService.lookupAccount(req.toAccNumber) match {
      case Some(customerId) =>
        //receiver name validation also possible here
        accountsService.addMoney(customerId, req.toAccNumber, req.amount) match {
          case Right(balance) =>
            transfers += transfer("INCOMING", "COMPLETED", customerId, req, balance)
            Some(balance)
          case Left(ex: Exception) =>
            transfers += transfer("INCOMING", customerId, req, ex)
          None
        }
      case None => None
    }
  }

}