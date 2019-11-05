package uk.ac.warwick.tabula.services.timetables

import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.services.permissions.AutowiringCacheStrategyComponent
import uk.ac.warwick.tabula.services.timetables.TimetableCacheKey._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.util.cache._
import uk.ac.warwick.util.collections.Pair

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object CachedPartialTimetableFetchingService {
  final val defaultCacheExpiryTime: Int = 60 * 60 * 6 // 6 hours in seconds
}

/**
  * A wrapper around a TimetableFetchingService that stores the resulting TimetableEvents in a
  * cache, thus allowing us not to thrash Syllabus+ too badly
  *
  */
class CachedPartialTimetableFetchingService(
  val delegate: PartialTimetableFetchingService,
  val cacheName: String,
  val cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime
) extends PartialTimetableFetchingService with AutowiringCacheStrategyComponent {

  val FetchTimeout: FiniteDuration = 15.seconds

  val cacheEntryFactory = new CacheEntryFactory[TimetableCacheKey, EventList] {

    def create(key: TimetableCacheKey): EventList = {
      val result = key match {
        case StudentKey(id) => delegate match {
          case delegate: StudentTimetableFetchingService => delegate.getTimetableForStudent(id)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching student timetables")
        }
        case StaffKey(id) => delegate match {
          case delegate: StaffTimetableFetchingService => delegate.getTimetableForStaff(id)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching staff timetables")
        }
        case CourseKey(id) => delegate match {
          case delegate: CourseTimetableFetchingService => delegate.getTimetableForCourse(id)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching course timetables")
        }
        case RoomKey(id) => delegate match {
          case delegate: RoomTimetableFetchingService => delegate.getTimetableForRoom(id)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching room timetables")
        }
        case ModuleKey(id) => delegate match {
          case delegate: ModuleTimetableFetchingService => delegate.getTimetableForModule(id, includeStudents = false)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching module timetables")
        }
        case ModuleWithStudentsKey(id) => delegate match {
          case delegate: ModuleTimetableFetchingService => delegate.getTimetableForModule(id, includeStudents = true)
          case _ => throw new UnsupportedOperationException("Delegate does not support fetching module timetables with students")
        }
      }

      Try(Await.result(result, FetchTimeout)) match {
        case Success(ev) => ev
        case Failure(e) => throw new CacheEntryUpdateException(e)
      }
    }

    def create(keys: JList[TimetableCacheKey]): JMap[TimetableCacheKey, EventList] = {
      JMap(keys.asScala.toSeq.map(id => (id, create(id))): _*)
    }

    def isSupportsMultiLookups: Boolean = true

    def shouldBeCached(response: EventList): Boolean = true
  }

  // rather than using 5 little caches, use one big one with a composite key
  lazy val timetableCache: Cache[TimetableCacheKey, EventList] =
    Caches.builder(cacheName, cacheEntryFactory, cacheStrategy)
      .expireAfterWrite(java.time.Duration.ofSeconds(cacheExpiryTime * 10))
      .expiryStategy(new TTLCacheExpiryStrategy[TimetableCacheKey, EventList] {
        override def getTTL(entry: CacheEntry[TimetableCacheKey, EventList]): Pair[Number, TimeUnit] = Pair.of(cacheExpiryTime * 10, TimeUnit.SECONDS)

        override def isStale(entry: CacheEntry[TimetableCacheKey, EventList]): Boolean = (entry.getTimestamp + cacheExpiryTime * 1000) <= DateTime.now.getMillis
      })
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .asynchronous()
      .build()

  // Unwraps the CacheEntryUpdateException into its cause, for case matching
  private def toFuture(eventList: => EventList) =
    Future(eventList).recoverWith { case e: CacheEntryUpdateException => Future.failed(e.getCause) }

  def getTimetableForStudent(universityId: String): Future[EventList] = toFuture(timetableCache.get(StudentKey(universityId)))

  def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList] = {
    if (includeStudents) toFuture(timetableCache.get(ModuleWithStudentsKey(moduleCode)))
    else toFuture(timetableCache.get(ModuleKey(moduleCode)))
  }

  def getTimetableForCourse(courseCode: String): Future[EventList] = toFuture(timetableCache.get(CourseKey(courseCode)))

  def getTimetableForRoom(roomName: String): Future[EventList] = toFuture(timetableCache.get(RoomKey(roomName)))

  def getTimetableForStaff(universityId: String): Future[EventList] = toFuture(timetableCache.get(StaffKey(universityId)))

}

class CachedStudentTimetableFetchingService(delegate: StudentTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with StudentTimetableFetchingService

class CachedModuleTimetableFetchingService(delegate: ModuleTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with ModuleTimetableFetchingService

class CachedCourseTimetableFetchingService(delegate: CourseTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with CourseTimetableFetchingService

class CachedRoomTimetableFetchingService(delegate: RoomTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with RoomTimetableFetchingService

class CachedStaffTimetableFetchingService(delegate: StaffTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with StaffTimetableFetchingService

class CachedStaffAndStudentTimetableFetchingService(delegate: StudentTimetableFetchingService with StaffTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with StudentTimetableFetchingService with StaffTimetableFetchingService

class CachedCompleteTimetableFetchingService(delegate: CompleteTimetableFetchingService, cacheName: String, cacheExpiryTime: Int = CachedPartialTimetableFetchingService.defaultCacheExpiryTime)
  extends CachedPartialTimetableFetchingService(delegate, cacheName, cacheExpiryTime) with CompleteTimetableFetchingService

@SerialVersionUID(3326840601345l) sealed trait TimetableCacheKey extends Serializable {
  val id: String
}

object TimetableCacheKey {

  case class StudentKey(id: String) extends TimetableCacheKey

  case class StaffKey(id: String) extends TimetableCacheKey

  case class CourseKey(id: String) extends TimetableCacheKey

  case class RoomKey(id: String) extends TimetableCacheKey

  case class ModuleKey(id: String) extends TimetableCacheKey

  case class ModuleWithStudentsKey(id: String) extends TimetableCacheKey

}
