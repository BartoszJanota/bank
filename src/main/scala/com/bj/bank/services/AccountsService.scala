package com.bj.bank.services

import java.util.concurrent.ConcurrentHashMap

import com.bj.bank.exceptions.{AccountNotFoundEx, CustomerNotFoundEx, InternalException, NotEnoughMoneyEx}
import com.bj.bank.models.Account._
import com.bj.bank.models.{Account, AccountReq, CustomerReq, TransferReq}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class AccountsService(implicit val executionContext: ExecutionContext) {
  val accounts: mutable.Map[String, ConcurrentHashMap[String, Account]] = mutable.Map.empty

  def lookupInternalSender(customerId: String, fromAccNumber: String) = {
    accounts.get(customerId).flatMap {customerAccounts =>
      if (customerAccounts.contains(fromAccNumber)){
        Some(customerId)
      } else {
        None
      }
    }
  }


  def lookupAccount(accNumber: String) = {
    accounts.toStream
      .find { customerAccounts =>
        customerAccounts._2.containsKey(accNumber)
      }
      .map(customerAccounts => customerAccounts._1)

  }

  def add(customerId: String, req: AccountReq) = {
    val acc = Account(req)
    accounts.get(customerId) match {
      case Some(customerAccounts) => {
        customerAccounts.put(acc.number, acc)
        Some(acc)
      }
      case _ => None
    }
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

  def addMoney(customerId: String, accNumber: String, amount: BigDecimal) = {
    accounts.get(customerId) match {
      case Some(customerAccounts) =>
        if (customerAccounts.containsKey(accNumber)) {
          val acc = customerAccounts.get(accNumber)
          val newBalance = acc.balance + amount
          customerAccounts.put(accNumber, updateBalance(acc, newBalance))
          Right(newBalance)
        } else {
          Left(AccountNotFoundEx)
        }
      case None => Left(CustomerNotFoundEx)
    }
  }


  def deductMoney(customerId: String, accNumber: String, amount: BigDecimal): Either[InternalException, BigDecimal] = {
    accounts.get(customerId) match {
      case Some(customerAccounts) =>
        if (customerAccounts.containsKey(accNumber)) {
          val acc = customerAccounts.get(accNumber)
          val newBalance = acc.balance - amount
          if (newBalance > 0L) {
            customerAccounts.put(accNumber, updateBalance(acc, newBalance))
            Right(newBalance)
          }
          else {
            Left(NotEnoughMoneyEx(newBalance))
          }
        } else {
          Left(AccountNotFoundEx(accNumber))
        }
      case None => Left(CustomerNotFoundEx(customerId))
    }
  }

  def transfer(fromCustomerId: String, toCustomerId: String, req: TransferReq): Either[InternalException, (BigDecimal, BigDecimal)] = {
    accounts.get(fromCustomerId) match {
      case Some(fromCustomerAccounts) =>
        val fromAccount = fromCustomerAccounts.get(req.fromAccNumber)
        val toCustomerAccounts = accounts(toCustomerId)
        val toAccount = toCustomerAccounts.get(req.toAccNumber)
        val fromNewBalance = fromAccount.balance - req.amount
        if(fromNewBalance > 0){
          fromCustomerAccounts.put(req.fromAccNumber, updateBalance(fromAccount, fromNewBalance))
          val toNewBalance = toAccount.balance + req.amount
          toCustomerAccounts.put(req.toAccNumber, updateBalance(toAccount, toNewBalance))
          Right(fromNewBalance, toNewBalance)
        } else {
          Left(NotEnoughMoneyEx(fromNewBalance))
        }
      case None => Left(CustomerNotFoundEx(fromCustomerId))
    }
  }

  def getAll() = accounts.values

  def get(customerId: String, accNumber: String) = {
    accounts.get(customerId) match {
      case Some(customerAccounts) =>
        if (customerAccounts.containsKey(accNumber)) {
          Some(customerAccounts.get(accNumber))
        } else {
          None
        }
      case _ => None
    }
  }

}
