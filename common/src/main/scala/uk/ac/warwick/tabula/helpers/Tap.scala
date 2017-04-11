package uk.ac.warwick.tabula.helpers

import language.implicitConversions

/**
 * K-Combinator. (AKA, a copy of ruby's Object.tap() method)
 *
 * Super-handy if you want to temporarily add some logging
 * to the return value of a function, without putting temporary
 * variables all over the place
 *
 * e.g.
 *
 *  def getStuff(keys):Seq[Stuff] =  dao.getTheStuffs(keys) // what was stuff?
 *
 *  Add a tap  ===>
 *
 *  def getStuff(keys) = dao.getTheStuffs(keys).tap(stuffs=> logger.info(stuffs.map(_.stuffCode).mkString(",")) )
 *
 */
class Tap[A](val any: A) extends AnyVal {
  def tap(f: (A) => Unit): A = {
    f(any)
    any
  }
}

object Tap {
  implicit def tap[A](toTap: A): Tap[A] = new Tap(toTap)
}
