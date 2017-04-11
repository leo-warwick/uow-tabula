package uk.ac.warwick.tabula

import javax.sql.DataSource

import org.hibernate.{Session, SessionFactory}
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.{ActiveProfiles, ContextConfiguration}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction._
import org.springframework.transaction.support._
import uk.ac.warwick.tabula.data.Transactions

import scala.language.implicitConversions

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/applicationContext-lazyinit.xml"))
@DirtiesContext
@ActiveProfiles(Array("test"))
abstract class AppContextTestBase extends TestBase with ContextSetup with TransactionalTesting

trait FieldAccessByReflection{
	// see http://stackoverflow.com/questions/1589603/scala-set-a-field-value-reflectively-from-field-name
	implicit class FieldReflector(ref: AnyRef) {
		def getV(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
		def setV(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
	}
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations=Array("/WEB-INF/properties-context.xml","/WEB-INF/persistence-context.xml"))
@DirtiesContext
@ActiveProfiles(Array("test"))
abstract class PersistenceTestBase extends TestBase with ContextSetup with TransactionalTesting {



}

trait ContextSetup {
	@Autowired var beans: AbstractAutowireCapableBeanFactory =_

	@Before def setupCtx() {

	}
}

trait TransactionalTesting {
	@Autowired var sessionFactory:SessionFactory =_
	@Autowired var dataSource:DataSource =_
	@Autowired var transactionManager:PlatformTransactionManager =_

	def session: Session = sessionFactory.getCurrentSession

	Transactions.enabled = true

	def transactional[A](f : TransactionStatus=>A) : A = {
		val template = new TransactionTemplate(transactionManager)

		template.execute(new TransactionCallback[A] {
			override def doInTransaction(status:TransactionStatus): A = {
				status.setRollbackOnly()
				f(status)
			}
		})
	}

	def flushing[A](s:Session)(f: =>A):A= {
		val a = f
		s.flush()
		a
	}
}