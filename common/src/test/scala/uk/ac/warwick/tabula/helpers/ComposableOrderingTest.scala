package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ComposableOrderingTest extends TestBase with Mockito {

	val student1: StudentMember = Fixtures.student("1234")
	student1.firstName = "Dave"
	student1.lastName = "Lister"

	val student2: StudentMember = Fixtures.student("2345")
	student2.firstName = "Arnold"
	student2.lastName = "Rimmer"

	val student3: StudentMember = Fixtures.student("3456")
	student3.firstName = "Ace"
	student3.lastName = "Rimmer"

	val student4: StudentMember = Fixtures.student("3457")
	student4.firstName = "Ace"
	student4.lastName = "Rimmer"

	val students = Seq(student1, student2, student3)

	@Test
	def singleOrdering(): Unit = {
		val c = new ComposableOrdering[StudentMember](Ordering.by[StudentMember, String](_.firstName))
		val sorted = students.sorted(c)
		sorted.head should be (student3)
	}

	@Test
	def singleOrderingReverse(): Unit = {
		val c = new ComposableOrdering[StudentMember](Ordering.by[StudentMember, String](_.firstName)(Ordering.String.reverse))
		val sorted = students.sorted(c)
		sorted.head should be (student1)
	}

	@Test
	def doubleOrdering(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student2.lastName == student3.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName < student2.firstName
		val c = new ComposableOrdering[StudentMember](o1, o2)
		val sorted = students.sorted(c)
		sorted should be (Seq(student1, student3, student2))
	}

	@Test
	def doubleOrderingWithReverse(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student2.lastName == student3.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName)(Ordering.String.reverse) // student3.firstName < student2.firstName
		val c = new ComposableOrdering[StudentMember](o1, o2)
		val sorted = students.sorted(c)
		sorted should be (Seq(student1, student2, student3))
	}

	@Test
	def tripleOrdering(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName) // student3.lastName == student4.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName == student4.firstName
		val o3 = Ordering.by[StudentMember, String](_.universityId) // student3.universityId < student4.universityId
		val c = new ComposableOrdering[StudentMember](o1, o2, o3)
		val sorted = (students ++ Seq(student4)).sorted(c)
		sorted should be (Seq(student1, student3, student4, student2))
	}

	@Test
	def tripleOrderingReverse(): Unit = {
		val o1 = Ordering.by[StudentMember, String](_.lastName)(Ordering.String.reverse) // student3.lastName == student4.lastName
		val o2 = Ordering.by[StudentMember, String](_.firstName) // student3.firstName == student4.firstName
		val o3 = Ordering.by[StudentMember, String](_.universityId)(Ordering.String.reverse) // student3.universityId > student4.universityId
		val c = new ComposableOrdering[StudentMember](o1, o2, o3)
		val sorted = (students ++ Seq(student4)).sorted(c)
		sorted should be (Seq(student4, student3, student2, student1))
	}

	trait MockFixture {
		private def toStudentMembers(any: Any): Array[StudentMember] = any.asInstanceOf[Array[_]].map(_.asInstanceOf[StudentMember])

		val lastNameOrdering: Ordering[StudentMember] = Ordering.by[StudentMember, String](_.lastName)
		val mockLastNameOrdering: Ordering[StudentMember] = smartMock[Ordering[StudentMember]]
		mockLastNameOrdering.compare(any[StudentMember], any[StudentMember]) answers { any =>
			val students = toStudentMembers(any)
			lastNameOrdering.compare(students(0), students(1))
		}

		val firstNameOrdering: Ordering[StudentMember] = Ordering.by[StudentMember, String](_.firstName)
		val mockFirstNameOrdering: Ordering[StudentMember] = smartMock[Ordering[StudentMember]]
		mockFirstNameOrdering.compare(any[StudentMember], any[StudentMember]) answers { any =>
			val students = toStudentMembers(any)
			firstNameOrdering.compare(students(0), students(1))
		}

		val universityIdOrdering: Ordering[StudentMember] = Ordering.by[StudentMember, String](_.universityId)
		val mockUniversityIdOrdering: Ordering[StudentMember] = smartMock[Ordering[StudentMember]]
		mockUniversityIdOrdering.compare(any[StudentMember], any[StudentMember]) answers { any =>
			val students = toStudentMembers(any)
			universityIdOrdering.compare(students(0), students(1))
		}

		val c = new ComposableOrdering[StudentMember](mockLastNameOrdering, mockFirstNameOrdering, mockUniversityIdOrdering)
	}

	@Test
	def lazyCheck(): Unit = {
		// Ensure that the orderings are only called when necessary

		new MockFixture {
			// Different last names, so only first ordering called
			val sorted: Seq[StudentMember] = Seq(student1, student2).sorted(c)
			sorted should be (Seq(student1, student2))
			verify(mockLastNameOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
			verify(mockFirstNameOrdering, times(0)).compare(any[StudentMember], any[StudentMember])
			verify(mockUniversityIdOrdering, times(0)).compare(any[StudentMember], any[StudentMember])
		}

		new MockFixture {
			// Same last name, different first name, so only 2 orderings called
			val sorted: Seq[StudentMember] = Seq(student2, student3).sorted(c)
			sorted should be (Seq(student3, student2))
			verify(mockLastNameOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
			verify(mockFirstNameOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
			verify(mockUniversityIdOrdering, times(0)).compare(any[StudentMember], any[StudentMember])
		}

		new MockFixture {
			// Same furst and last name, so all 3 orderings called
			val sorted: Seq[StudentMember] = Seq(student4, student3).sorted(c)
			sorted should be (Seq(student3, student4))
			verify(mockLastNameOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
			verify(mockFirstNameOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
			verify(mockUniversityIdOrdering, atLeast(1)).compare(any[StudentMember], any[StudentMember])
		}
	}

}
