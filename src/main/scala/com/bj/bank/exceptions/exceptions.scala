package com.bj.bank.exceptions

case class NotEnoughMoneyEx(saldo: BigDecimal) extends Exception

object AccountNotFoundEx extends Exception
