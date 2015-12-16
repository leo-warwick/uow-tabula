package uk.ac.warwick.tabula.services.jobs

import java.util.UUID

import uk.ac.warwick.tabula.data.JobDao

import scala.collection.mutable.ArrayBuffer

class MockJobDao extends JobDao {

	val instances = ArrayBuffer[JobInstance]()

	def findOutstandingInstances(max: Int) = {
		instances.iterator.filterNot( _.started ).take(max).toSeq
	}

	def findOutstandingInstance(example: JobInstance) = {
		instances.filterNot( _.started ).find { inst =>
			inst.jobType == example.jobType &&
			inst.json == example.json
		}
	}

	def saveJob(instance: JobInstance) = {
		instance.id = UUID.randomUUID.toString
		instances += instance
		instance
	}

	def unfinishedInstances = instances.toSeq.filterNot( _.finished )

	def listRecent(start: Int, count: Int) = instances.toSeq.filter( _.finished ).slice(start, start+count)

	def getById(id:String) = instances.find( id == _.id )

	def clear = instances.clear

	def update(instance: JobInstance) {}

	override def listRunningJobs = instances.toSeq.filterNot(_.finished).filter(_.started)
}