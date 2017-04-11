package uk.ac.warwick.tabula.data.model

import javax.persistence.{Column, Version}

trait HibernateVersioned {
	@Version @Column(name="hib_version") var version: Int = _
}