package uk.ac.warwick.tabula.services.fileserver

import org.joda.time.ReadablePeriod

case class CachePolicy(expires: Option[ReadablePeriod] = None)
