package uk.ac.warwick.tabula.services.turnitinlti

import uk.ac.warwick.tabula.TestBase

class TurnitinLtiResponseTest extends TestBase {

	@Test def submissionsList() = {
		val response = TurnitinLtiResponse.fromJson( SubmittedPaperInfo )
		val submissionInfo = response.submissionInfo()
		submissionInfo.overlap should be (Some(67))
		submissionInfo.similarity should be (Some(3))
		submissionInfo.publication_overlap should be (Some(35))
		submissionInfo.student_overlap should be (Some(67))
		submissionInfo.web_overlap should be (Some(0))
	}

	val SubmittedPaperInfo = """
	{
		"outcome_originalfile":{
			"roles":[
			"Learner",
			"Instructor"
			],
			"text":null,
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/orig/200503253?lang=en_us",
			"label":"Download File in Original Format"
		},
		"outcome_grademark":{
			"roles":[
			"Instructor"
			],
			"numeric":{
			"score":null,
			"max":null
		},
			"text":"--",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/grademark/200503253?lang=en_us",
			"label":"Open GradeMark"
		},
		"outcome_pdffile":{
			"roles":[
			"Learner",
			"Instructor"
			],
			"text":null,
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/download/pdf/200503253?lang=en_us",
			"label":"Download File in PDF Format"
		},
		"outcome_originalityreport":{
			"text":"67%",
			"roles":[
			"Instructor"
			],
			"numeric":{
			"score":67,
			"max":100
		},
			"label":"Open Originality Report",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/dv/report/200503253?lang=en_us",
			"breakdown":{
			"publications_score":35,
			"internet_score":0,
			"submitted_works_score":67
		}
		},
		"outcome_resubmit":{
			"text":null,
			"roles":[
			"Learner"
			],
			"label":"Resubmit File",
			"launch_url":"https://sandbox.turnitin.com/api/lti/1p0/upload/resubmit/200503253?lang=en_us"
		}
	}

	"""
}

