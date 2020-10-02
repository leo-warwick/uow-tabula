package uk.ac.warwick.tabula.data.model.mitcircs

import enumeratum.{EnumEntry, _}
import uk.ac.warwick.tabula.data.model.{EnumSeqUserType, StudentMember}
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable

sealed abstract class IssueType(val description: String, val helpText: String, val evidenceGuidance: String) extends EnumEntry

sealed abstract class CoronavirusIssueType(description: String, helpText: String, override val evidenceGuidance: String = "") extends IssueType(description, helpText, evidenceGuidance)

object IssueType extends Enum[IssueType] {

  val values: immutable.IndexedSeq[IssueType] = findValues

  case object SeriousAccident extends IssueType(
    description = "Serious accident",
    helpText = "An accident which had a significant effect on your ability to complete an assessment. Normally the accident would have required you to receive medical treatment and would be supported by a doctor's (or other healthcare professional) note.",
    evidenceGuidance = "The evidence that is helpful, if available: (i) Official letter or email from an appropriately qualified professional (e.g. medical doctor, nurse, counsellor) confirming dates affected. Normally this should have been recorded around the date of the serious illness, accident or trauma and should attest to evidenced impact on the student. (ii) Evidence of impact on your ability to undertake the assessment obtained from sources such as a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services and other external support services."
  )
  case object SeriousPhysicalIllness extends IssueType(
    description = "Serious physical illness",
    helpText = "An illness that might require medication prescribed by a GP, or a referral to a specialist, normally supported by a doctor's (or other healthcare professional) note. Minor illnesses such as coughs and colds not requiring treatment would not normally be eligible.",
    evidenceGuidance = "The evidence that is helpful, if available: (i) Official letter or email from an appropriately qualified professional (e.g. medical doctor, nurse, counsellor) confirming dates affected. Normally this should have been recorded around the date of the serious illness, accident or trauma and should attest to evidenced impact on the student. (ii) Evidence of impact on your ability to undertake the assessment obtained from sources such as a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services and other external support services."
  )
  case object MentalHealth extends IssueType(
    description = "Mental health issue",
    helpText = "A mental health issue for which you’re receiving or are waiting for support from university or other mental health services or your GP, normally supported by a note from your support service or GP / healthcare professional. Issues arising from short-term assessment stress and anxiety are not normally eligible unless it is a flare-up of a pre-diagnosed illness / condition.",
    evidenceGuidance = "The evidence that is helpful, if available: (i) Official letter or email from an appropriately qualified professional (e.g. medical doctor, nurse, counsellor) confirming dates affected. Normally this should have been recorded around the date of the serious illness, accident or trauma and should attest to evidenced impact on the student. (ii) Evidence of impact on your ability to undertake the assessment obtained from sources such as a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services and other external support services."
  )
  case object SeriousMedicalOther extends IssueType(
    description = "Serious accident or illness of someone close",
    helpText = "This would normally be a close family member, and would normally be supported by a doctor's note. Conditions which require you to undertake new and significant caring responsibilities are particularly relevant.",
    evidenceGuidance = "The evidence that is helpful, if available: EITHER Official letter or email from a health professional confirming the circumstances with the dates OR letter from Personal or Senior Tutor, health professional, or Student Support explaining how the circumstances are affecting your ability to do the assessment. OR FOR CARERS: Official letter from health professional confirming the circumstances with the dates AND a statement written yourself or by a Personal or Senior Tutor, health professional, or Student Support to confirm that you have substantial care and support responsibilities for the person."
  )
  case object Employment extends IssueType(
    description = "Significant changes in employment circumstances",
    helpText = "As a part-time student, if you’re also in employment and your employer makes changes beyond your control, e.g. to your working hours or your place of employment.",
    evidenceGuidance = "A letter from from your employer confirming new working hours and/or a statement from your personal tutor or similar indicating the impact on you."
  )
  case object Deterioration extends IssueType(
    description = "Deterioration of a permanent condition",
    helpText = "A condition which you have already reported and is already covered by reasonable adjustments, but which has become significantly worse.",
    evidenceGuidance = "If this permanent condition has already been adequately adjusted through Special Examination arrangements or other reasonable adjustments, it is only the deterioration or significant change of circumstance that can be considered as a Mitigating Circumstance. An official letter or email from a health professional, or Disability Services or Counsellor or Senior Tutor or an email confirming deterioration with dates. This letter/email should be written around the time of the deterioration and should attest to evidenced impact on the student"
  )
  case object Bereavement extends IssueType(
    description = "Bereavement",
    helpText = "If there has been a death of someone in your family or close to you, tick this option",
    evidenceGuidance = "If you do have documentation and feel able to share it, you can submit it within the portal. You do not need to submit evidence immediately.  Alternatively, you can share information about your circumstances with your personal or senior tutor who can acknowledge your claim on your behalf, without the provision of evidence.  Please tell us how the bereavement has affected your ability to study. If you require an extension for assessed work then contact your department. The evidence that is helpful, if available: Evidence of impact on your ability to undertake the assessment either through a short statement written yourself or obtained from sources such as a Personal or Senior Tutor doctor, counsellor or Students’ Union Advice Centre or Student Services OR a copy of the death certificate or order of funeral service or death announcement in a newspaper or on the web."
  )
  case object AbruptChange extends IssueType(
    description = "Sudden change in personal circumstances",
    helpText = "Changes of this sort may include a divorce or separation, a sudden change in financial circumstances, a court appearance, or an acute accommodation crisis. If this is related to the coronavirus crisis either claim it here or separately in the coronavirus part of the portal.",
    evidenceGuidance = "Employment or finances: Evidence of unforeseen financial hardship, e.g. bank statements showing current financial circumstances, loss of job or redundancy AND/OR Statement from Personal tutor or Senior Tutor or Student Support, Student Funding or Students’ Union Advice Centre attesting to impact on student.\nSerious Family Problems: Letter from a doctor, solicitor or other professional person confirming the circumstances and dates AND/OR Evidence from a doctor, nurse or relevant professional, Personal Tutor, Senior Tutor, Student Support or Students’ Union Advice Centre attesting to the impact on your ability to carry out the assessment.\nCourt Appearance/jury duty: Letter from court with date student is expected to appear.\nOther issues not listed above: Evidence provided should prove the circumstance exists (must be from independent source) with dates AND evidence from a doctor, nurse or relevant professional, Personal tutor or Senior Tutor or Student Support or Students’ Union Advice Centre attesting to the impact on your ability to carry out the assessment. "
  )
  case object LateDiagnosis extends IssueType(
    description = "Late diagnosis of a specific learning difference",
    helpText = "If you have not previously been diagnosed with a disability (including a specific learning difference), but receive such a diagnosis close to an assessment.",
    evidenceGuidance = "A diagnosis letter and confirmation from your department or from Disability Services that the diagnosis was submitted too late and missed the University deadline."
  )
  case object VictimOfCrime extends IssueType(
    description = "Victim of crime",
    helpText = "If you are the victim of a crime (normally supported by a crime number provided by the police) which has caused you significant distress and/or practical difficulties. Involvement in a criminal case as a witness may also be eligible.",
    evidenceGuidance = "Official police report giving the date of the crime OR a statement from a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services or other external support service outlining nature of circumstance with dates affected and the impact on your ability to undertake the assessment."
  )
  case object Harassment extends IssueType(
    description = "Suffered bullying, harassment, victimisation or threatening behaviour",
    helpText = "If you have suffered behaviour which has caused you significant distress and which you have reported to an appropriate body.",
    evidenceGuidance = "Statement from a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services or other external support service outlining nature of circumstance with dates affected and the impact on your ability to undertake the assessment."
  )
  case object IndustrialAction extends IssueType(
    description = "Industrial action",
    helpText = "If your studies are affected by industrial action (e.g. your lectures or seminars get cancelled or rearranged) then this may be eligible as mitigating circumstances. A statement of the disruption that has occurred should be provided by your department, and you should say how this has affected your ability to complete your assessments.",
    evidenceGuidance = "A statement from your department of the disruption that has affected your studies."
  )
  case object Trauma extends IssueType(
    description = "Trauma",
    helpText = "We are aware that trauma can take many forms and caused by a wide range of pressures arising from circumstances which may be specific to an individual. If something has occurred that has significantly impacted on your ability to engage with your academic studies please discuss what has occurred with your Personal Tutor, Departmental Senior Tutor, or the Wellbeing Support Service",
    evidenceGuidance = "We are aware that trauma can take many forms and caused by a wide range of pressures arising from circumstances which may be specific to an individual. If something has occurred that has significantly impacted on your ability to engage with your academic studies please discuss what has occurred with your Personal Tutor, Departmental Senior Tutor, or the Wellbeing Support Service.  Please provide a Statement from a Personal or Senior Tutor, Doctor, Counsellor, Students’ Union Advice Centre, Report and Support, Student Services or other external support service outlining nature of circumstance with dates affected and the impact on your ability to undertake assessment."
  )
  case object Other extends IssueType(
    description = "Other",
    helpText = "This may include: gender transition or gender reassignment; maternity, paternity or adoption leave; caring responsibilities. However, this list is not exhaustive. If you want to report a claim for something which you believe represents a mitigating circumstance, but which is not shown on this form, you should enter it here.",
    evidenceGuidance = "Where appropriate please supply any independent evidence that you may have from a relevant professional person or body (including dates) and the effect it had on you."
  ) {
    val covidHelpText = "If coronavirus has affected your circumstances in some other way, please tick this option and tell us something about what has happened"
  }

  case object SelfIsolate extends CoronavirusIssueType(
    description = "Self-isolating due to somebody in household having symptoms",
    helpText = "If you’re self-isolating because someone in your household has symptoms that suggest coronavirus (even though you may not be showing symptoms yourself), tick this option",
    evidenceGuidance = "If you remain symptom-free, then you do not need to provide any formal evidence other than explaining how this has affected your ability to study. If you require an extension for assessed work within this period please contact your department. If you are on campus or elsewhere and receive a test and trace notification, then this could be submitted as evidence."
  )

  case object SelfIsolate7Days extends CoronavirusIssueType(
    description = "Required to self-isolate for 7 days by test and trace or medically qualified person",
    helpText = "If you have been advised by your GP or a doctor or test and trace that you need to self-isolate, tick this option",
    evidenceGuidance = "Please provide evidence of the need to quarantine or the instruction from a medically qualified person or test and trace.\nIf your illness is mild, then you do not need to provide any other formal evidence other than explaining how this has affected your ability to study. If you require an extension for assessed work within this period please contact your department. If you are on campus or elsewhere and receive a test and trace notification then this could be submitted as evidence."
  )

  case object SelfIsolate14Days extends CoronavirusIssueType(
    description = "Required to self-isolate for 14 days by test and trace or medically qualified person",
    helpText = "If you have been advised by your GP or a doctor or test and trace that you need to self-isolate, tick this option",
    evidenceGuidance = "Please provide evidence of the need to quarantine or the instruction from a medically qualified person or test and trace.\nIf your illness is mild, then you do not need to provide any other formal evidence other than explaining how this has affected your ability to study. If you require an extension for assessed work within this period please contact your department. If you are on campus or elsewhere and receive a test and trace notification then this could be submitted as evidence."
  )

  // this option can't be selected anymore but must remain to support existing claims of this type
  case object VulnerableGroup extends CoronavirusIssueType(
    description = "I am in a highly vulnerable group and am being shielded for 12 weeks isolation",
    helpText = "For example, people undergoing cancer treatment, people with severe respiratory conditions",
    evidenceGuidance = "Please provide a copy of your official letter directing you to be shielded for 12 weeks isolation."
  )

  case object InsufficientITProvision extends CoronavirusIssueType(
    description = "My internet connection or IT provision failed/is not sufficient to undertake on-line teaching and assessment",
    helpText = "If you have a problem with IT provision, tick this option",
    evidenceGuidance = "Provide date and time of failure of internet connection or IT equipment. If your internet connection is not appropriate to participate in assessments or if you are not able to obtain certain teaching or examination materials due to your location, please contact your Department as soon as possible to discuss appropriate support."
  )

  case object Diagnosed extends CoronavirusIssueType(
    description = "Diagnosed with coronavirus (10 day self-isolation) and/or a coronavirus hospital inpatient",
    helpText = "If you have been diagnosed with coronavirus, tick this option",
    evidenceGuidance = "Please provide the date you were diagnosed and your notification and how this has affected your ability to study. If you require an extension for assessed work within this affected period, please contact your department if you are able to. If you are admitted to hospital, please provide the length of time you were ill or hospitalised, and the name of the hospital where you were treated. We recognise that there will be times when it isn’t possible to submit this information at the time. This can be provided at a later date but if it is possible to inform your department of your circumstances in the meantime please do so."
  )

  case object AwaitingResults extends CoronavirusIssueType(
    description = "Awaiting the result of a coronavirus test",
    helpText = "If you have been tested for coronavirus but have not yet received the results of your test, tick this option",
    evidenceGuidance = "Please provide evidence of the result when known and tell us how this has affected your ability to study. If you require an extension for assessed work within this period please contact your department."
  )

  case object CoronavirusBereavement extends CoronavirusIssueType(
    description = "Bereavement due to coronavirus",
    helpText = "If there has been a death of someone in your family or close to you as a result of coronavirus, tick this option",
    evidenceGuidance = "If you do have documentation and feel able to share it, you can submit it within the portal. You do not need to submit evidence immediately.  Alternatively, you can share information about your circumstances with your personal or senior tutor who can acknowledge your claim on your behalf, without the provision of evidence.  Please tell us how the bereavement has affected your ability to study. If you require an extension for assessed work then contact your department. The evidence that is helpful, if available: Evidence of impact on your ability to undertake the assessment either through a short statement written yourself or obtained from sources such as a Personal or Senior Tutor doctor, counsellor or Students’ Union Advice Centre or Student Services OR a copy of the death certificate or order of funeral service or death announcement in a newspaper or on the web."
  )

  case object Carer extends CoronavirusIssueType(
    description = "Carer for a coronavirus patient ",
    helpText = "If you are acting as the carer for someone (other than yourself) who is suffering from coronavirus, tick this option",
    evidenceGuidance = "Please provide the date the patient was diagnosed and/or entered hospital, the length of time they were ill or hospitalised, and the name of the hospital where they were treated. We recognise that there will be times when it isn’t possible to submit this information at the time. This can be provided at a later date but if it is possible to inform your department of your circumstances in the meantime please do so.  Please tell us how this has affected your ability to study. If you require an extension for assessed work then contact your department. "
  )

  case object CarerSelfIsolate extends CoronavirusIssueType(
    description = "Carer for a family/household member required to self-isolate",
    helpText = "Please tell us how this has affected your ability to study and the name of the family or household member required to self-isolate. If you require an extension for assessed work then contact your department.  If you require more support or any other reasonable adjustment, please contact your department.",
  )

  case object CarerChildcareClosure extends CoronavirusIssueType(
    description = "Carer of children due to school closure",
    helpText = "If you are experiencing difficulties due to childcare (eg difficulty fully participating in on-line teaching or assessment), tick this box",
    evidenceGuidance = "Please tell us how this has affected your ability to study and the name of the school(s) closed. If you require an extension for assessed work then contact your department.  If you require more support or any other reasonable adjustment, please contact your department."
  )

  case object NoVisa extends CoronavirusIssueType(
    description = "Not able to obtain a Visa",
    helpText = "If you have not been able to obtain a visa to come to the UK because of travel or other restrictions put in place by your country or by the UK, tick this option",
    evidenceGuidance = "Evidence is not required for students affected until 11th January 2021. From 12th January 2021 please provide us with any visa rejection letters."
  )

  case object CannotTravel extends CoronavirusIssueType(
    description = "Unable to travel to the UK due to a travel ban resulting from coronavirus",
    helpText = "If there are travel restrictions in place which make it impossible for you to travel to the UK, tick this option",
    evidenceGuidance = "Evidence is not required for students affected until 11th January 2021. From 12th January 2021 please provide us with any links to government advice/official travel restrictions or cancelled flight tickets"
  )

  def coronavirusIssueTypes: Seq[IssueType] = IssueType.values.collect { case i: CoronavirusIssueType => i } ++ Seq(Other)
  def generalIssueTypes: Seq[IssueType] =  IssueType.values.diff(coronavirusIssueTypes) ++ Seq(Other)

  def validIssueTypes(student: StudentMember): Seq[IssueType] = {
    // TODO - Make it possible for TQ to enable this (we could also just manage this in code)
    val invalidTypes =
      if (Option(student.mostSignificantCourse).flatMap(scd => Option(scd.latestStudentCourseYearDetails)).flatMap(scyd => Option(scyd.modeOfAttendance)).map(_.code).contains("P")) Seq(IndustrialAction, VulnerableGroup)
      else Seq(Employment, IndustrialAction, VulnerableGroup)

    generalIssueTypes.filterNot(invalidTypes.contains)
  }
}

class IssueTypeUserType extends EnumSeqUserType(IssueType)
class IssueTypeConverter extends EnumTwoWayConverter(IssueType)
