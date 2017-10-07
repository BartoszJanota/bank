package com.bj.bank.exceptions

trait InternalException{
  def message: Object
}

case class NotEnoughMoneyEx(message: BigDecimal) extends InternalException

case class AccountNotFoundEx(message: String) extends InternalException

case class CustomerNotFoundEx(message: String) extends InternalException
