package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.userlookup.User

trait ExtensionServiceComponent {
  def extensionService: ExtensionService
}

trait AutowiringExtensionServiceComponent extends ExtensionServiceComponent {
  var extensionService: ExtensionService = Wire[ExtensionService]
}

object ExtensionService {
  val MaxResults = 50
}

trait ExtensionService {
  def saveOrUpdate(extension: Extension): Unit

  def getExtensionById(id: String): Option[Extension]

  def countExtensions(assignment: Assignment): Int

  def hasExtensions(assignment: Assignment): Boolean

  def countUnapprovedExtensions(assignment: Assignment): Int

  def hasUnapprovedExtensions(assignment: Assignment): Boolean

  def getUnapprovedExtensions(assignment: Assignment): Seq[Extension]

  /**
    * Get all approved extensions for this assignment, using the latest extended deadline for where a student has multiple.
    */
  def getApprovedExtensionsByUserId(assignment: Assignment): Map[String, Extension]

  /**
    * Get all extensions for this assignment, mapped by usercode
    */
  def getAllExtensionsByUserId(assignment: Assignment): Map[String, Seq[Extension]]

  def countExtensionsByState(assignment: Assignment): Map[ExtensionState, Int]

  def getAllExtensionRequests(user: User): Seq[Extension]

  def filterExtensions(restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder], maxResults: Int, startResult: Int): Seq[Extension]

  def countFilteredExtensions(restrictions: Seq[ScalaRestriction]): Int
}

abstract class AbstractExtensionService extends ExtensionService {
  self: ExtensionDaoComponent =>

  def saveOrUpdate(extension: Extension): Unit = extensionDao.saveOrUpdate(extension)

  def getExtensionById(id: String): Option[Extension] = transactional(readOnly = true) {
    extensionDao.getExtensionById(id)
  }

  def countExtensions(assignment: Assignment): Int = transactional(readOnly = true) {
    extensionDao.countExtensions(assignment)
  }

  def countUnapprovedExtensions(assignment: Assignment): Int = transactional(readOnly = true) {
    extensionDao.countUnapprovedExtensions(assignment)
  }

  def getUnapprovedExtensions(assignment: Assignment): Seq[Extension] = transactional(readOnly = true) {
    extensionDao.getUnapprovedExtensions(assignment)
  }

  def hasExtensions(assignment: Assignment): Boolean = countExtensions(assignment) > 0

  def hasUnapprovedExtensions(assignment: Assignment): Boolean = countUnapprovedExtensions(assignment) > 0

  def getApprovedExtensionsByUserId(assignment: Assignment): Map[String, Extension] = transactional(readOnly = true) {
    extensionDao.getApprovedExtensions(assignment)
      .groupBy(_.usercode)
      .mapValues(_.maxBy(_.expiryDate.map(_.getMillis).getOrElse(0L)))
  }

  def getAllExtensionsByUserId(assignment: Assignment): Map[String, Seq[Extension]] = transactional(readOnly = true) {
    extensionDao.getAllExtensions(assignment)
      .groupBy(_.usercode)
  }

  def countExtensionsByState(assignment: Assignment): Map[ExtensionState, Int] = transactional(readOnly = true) {
    extensionDao.countExtensionsByState(assignment)
  }

  def getAllExtensionRequests(user: User): Seq[Extension] = transactional(readOnly = true) {
    extensionDao.getAllExtensionRequests(user)
  }

  def filterExtensions(restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq(),
    maxResults: Int = ExtensionService.MaxResults, startResult: Int = 0): Seq[Extension] = {
    extensionDao.filterExtensions(restrictions, orders, maxResults, startResult)
  }

  def countFilteredExtensions(restrictions: Seq[ScalaRestriction]): Int =
    extensionDao.countFilteredExtensions(restrictions)

}

@Service(value = "extensionService")
class ExtensionServiceImpl
  extends AbstractExtensionService
    with AutowiringExtensionDaoComponent