package uk.ac.warwick.tabula.services.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import javax.persistence.{Column, Entity}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.system.CurrentUserInterceptor
import uk.ac.warwick.tabula.{CurrentUser, ToString}
import uk.ac.warwick.userlookup.{AnonymousUser, UserLookupInterface}

object JobInstanceImpl {
  def fromPrototype(prototype: JobPrototype): JobInstanceImpl = {
    val instance = new JobInstanceImpl
    instance.jobType = prototype.identifier
    instance.json = prototype.map
    instance
  }
}

/**
  * JobDefinition is the database entity that stores
  * data about the job request, its status and progress.
  * There can be many Job subclasses but JobInstance
  * does not need subclassing.
  */
@Entity(name = "Job")
@Proxy
class JobInstanceImpl() extends JobInstance with GeneratedId with PostLoadBehaviour with Logging with ToString {

  @transient var jsonMapper: ObjectMapper = Wire.auto[ObjectMapper]
  @transient var userLookup: UserLookupInterface = Wire.auto[UserLookupInterface]
  @transient var currentUserFinder: CurrentUserInterceptor = Wire.auto[CurrentUserInterceptor]

  /** Human-readable status of the job */
  override var status: String = _

  var jobType: String = _

  override var started = false
  override var finished = false
  override var succeeded = false

  var realUser: String = _
  var apparentUser: String = _

  override def userId: String = apparentUser

  @transient var user: CurrentUser = _

  override var createdDate: DateTime = new DateTime

  override var updatedDate: DateTime = new DateTime

  @Column(name = "progress") var _progress: Int = 0

  override def progress: Int = _progress

  override def progress_=(p: Int): Unit = {
    _progress = p
  }

  @Column(name = "instance")
  override var schedulerInstance: String = _

  // CLOB
  var data: String = "{}"

  @transient private var _json: JsonMap = Map()

  override def json: JsonMap = _json

  def json_=(map: JsonMap): Unit = {
    _json = map
    if (jsonMapper != null) {
      data = jsonMapper.writeValueAsString(json)
    } else {
      logger.warn("JSON mapper not set on JobInstanceImpl")
    }
  }

  override def propsMap: JsonMap = json

  override def propsMap_=(map: JsonMap): Unit = {
    json = map
  }

  override def postLoad(): Unit = {
    val map = jsonMapper.readValue(data, classOf[Map[String, Any]])
    json = map

    updatedDate = new DateTime

    def u(id: String) = id match {
      case id: String => userLookup.getUserByUserId(id)
      case _ => new AnonymousUser
    }

    val realUser = u(this.realUser)
    val apparentUser = u(this.apparentUser)

    user = currentUserFinder.resolveCurrentUser(realUser, { (u, s) => apparentUser }, false)
  }

  override def toStringProps = Seq(
    "id" -> id,
    "status" -> status,
    "jobType" -> jobType)

}
