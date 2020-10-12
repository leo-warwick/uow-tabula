package uk.ac.warwick.tabula.data.model.forms

import java.io.StringReader
import java.sql.Types

import com.fasterxml.jackson.databind.ObjectMapper
import freemarker.core.TemplateHTMLOutputModel
import javax.persistence._
import org.hibernate.`type`.StandardBasicTypes
import org.hibernate.annotations.{Proxy, Type}
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.UserLookupService

import scala.reflect._

/**
  * A FormField defines a field to be displayed on an Assignment
  * when a student is making a submission or a marker is entering feedback.
  *
  * Submissions are bound in the command as FormValue items,
  * and if validation passes a Submission object is saved with a
  * collection of SavedFormValue objects.
  *
  * Although there can be many types of FormField, many of them
  * can use the same FormValue class if they contain the
  * same sort of data (e.g. a string), and there is only one
  * SavedFormValue class.
  *
  */

object FormField {
  final val FormFieldMaxSize = 32768
}

@Entity
@Proxy
@Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "fieldtype")
abstract class FormField extends GeneratedId with Logging {

  @transient var json: ObjectMapper = JsonObjectMapperFactory.instance
  @transient var userLookup: UserLookupService = Wire.auto[UserLookupService]

  var name: String = _
  var label: String = _
  var instructions: String = _
  var required: Boolean = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.forms.FormFieldContextUserType")
  var context: FormFieldContext = _

  @Basic(optional = false)
  @Access(AccessType.PROPERTY)
  def getProperties(): String = {
    // TODO cache the string value.
    json.writeValueAsString(propertiesMap)
  }

  def properties: String = getProperties()

  def setProperties(props: String): Unit = {
    propertiesMap = json.readValue(new StringReader(props), classOf[Map[String, Any]])
  }

  @transient var propertiesMap: collection.Map[String, Any] = Map()

  protected def setProperty(name: String, value: Any): Unit = {
    propertiesMap += name -> value
  }

  /**
    * Fetch a property out of the property map if it matches the type.
    * Careful with types as they are generally the ones that the JSON library
    * has decided on, so integers come out as JInteger, and Int
    * won't match.
    */
  protected def getProperty[A: ClassTag](name: String, default: A): A =
    propertiesMap.get(name) match {
      case Some(null) => default
      case Some(obj) if classTag[A].runtimeClass.isInstance(obj) => obj.asInstanceOf[A]
      case Some(obj) =>
        // TAB-705 warn when we return an unexpected type
        logger.warn("Expected property %s of type %s, but was %s".format(name, classTag[A].runtimeClass.getName, obj.getClass.getName))
        default
      case _ => default
    }

  def isReadOnly = false

  final def readOnly: Boolean = isReadOnly

  @Type(`type` = "int")
  var position: JInteger = 0

  /** Determines which Freemarker template is used to render it. */
  @transient lazy val template: String = getClass.getAnnotation(classOf[DiscriminatorValue]).value

  /**
    * Return a blank FormValue that can be used to bind a submission
    * of the same type as this FormField.
    */
  def blankFormValue: FormValue

  /**
    * Return a form value of the appropriate type with its value set
    */
  def populatedFormValue(value: SavedFormValue): FormValue

  def validate(value: FormValue, errors: Errors): Unit

}

trait SimpleValue[A] {
  self: FormField =>
  def value_=(value: A): Unit = {
    propertiesMap += "value" -> value
  }

  def setValue(value: A): Unit = value_=(value)

  def value: A = propertiesMap.getOrElse("value", null).asInstanceOf[A]

  def getValue(): A = value

  override def validate(value: FormValue, errors: Errors): Unit = {
    value match {
      case s: StringFormValue if s.value != null =>
        val length = s.value.toString.length
        if (length > FormField.FormFieldMaxSize)
          errors.rejectValue("value", "textfield.tooLarge", Array[Object](length: JInteger, FormField.FormFieldMaxSize: JInteger), "")
      case _ =>
    }
  }

  def blankFormValue = new StringFormValue(this)

  def populatedFormValue(savedFormValue: SavedFormValue): StringFormValue = {
    val formValue = new StringFormValue(this)
    formValue.value = savedFormValue.value
    formValue
  }

}

@Entity
@Proxy
abstract class AssignmentFormField extends FormField {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignment_id")
  var assignment: Assignment = _

  def duplicate(newAssignment: Assignment): AssignmentFormField
}

@Entity
@Proxy
@DiscriminatorValue("comment")
class CommentField extends AssignmentFormField with SimpleValue[String] {
  override def isReadOnly = true

  def formattedHtml: TemplateHTMLOutputModel = FormattedHtml(Option(value))

  override def duplicate(newAssignment: Assignment): CommentField = {
    val newField = new CommentField
    newField.assignment = newAssignment
    newField.name = Assignment.defaultCommentFieldName
    newField.label = this.label
    newField.context = this.context
    newField.value = this.value
    newField
  }
}

@Entity
@Proxy
@DiscriminatorValue("text")
class TextField extends AssignmentFormField with SimpleValue[String] {

  override def duplicate(newAssignment: Assignment): TextField = {
    val newField = new TextField
    newField.assignment = newAssignment
    newField.context = this.context
    newField.name = this.name
    newField.label = this.label
    newField
  }

}

@Entity
@Proxy
@DiscriminatorValue("wordcount")
class WordCountField extends AssignmentFormField {
  context = FormFieldContext.Submission

  def min: JInteger = getProperty[JInteger]("min", null)

  def min_=(limit: JInteger): Unit = setProperty("min", limit)

  def max: JInteger = getProperty[JInteger]("max", null)

  def max_=(limit: JInteger): Unit = setProperty("max", limit)

  def conventions: String = getProperty[String]("conventions", null)

  def conventions_=(conventions: String): Unit = setProperty("conventions", conventions)

  def blankFormValue = new IntegerFormValue(this)

  def populatedFormValue(savedFormValue: SavedFormValue): IntegerFormValue = {
    val formValue = new IntegerFormValue(this)
    formValue.value = savedFormValue.value.asInstanceOf[Integer]
    formValue
  }

  override def validate(value: FormValue, errors: Errors): Unit = {
    value match {
      case i: IntegerFormValue =>
        if (i.value == null) errors.rejectValue("value", "assignment.submit.wordCount.missing")
        else if ((min != null && i.value < min) || (max != null && i.value > max)) errors.rejectValue("value", "assignment.submit.wordCount.outOfRange")
      case _ => errors.rejectValue("value", "assignment.submit.wordCount.missing") // value was null or wrong type
    }
  }

  override def duplicate(newAssignment: Assignment): WordCountField = {
    val newField = new WordCountField
    newField.assignment = newAssignment
    newField.name = Assignment.defaultWordCountName
    newField.label = this.label
    newField.max = this.max
    newField.min = this.min
    newField.conventions = this.conventions
    newField
  }

}

@Entity
@Proxy
@DiscriminatorValue("textarea")
class TextareaField extends AssignmentFormField with SimpleValue[String] {

  override def duplicate(newAssignment: Assignment): TextareaField = {
    val newField = new TextareaField
    newField.assignment = newAssignment
    newField.context = this.context
    newField.name = this.name
    newField.label = this.label
    newField
  }

}

@Entity
@Proxy
@DiscriminatorValue("checkbox")
class CheckboxField extends FormField {
  def blankFormValue = new BooleanFormValue(this)

  def populatedFormValue(savedFormValue: SavedFormValue): BooleanFormValue = {
    val formValue = new BooleanFormValue(this)
    formValue.value = savedFormValue.value.asInstanceOf[Boolean]
    formValue
  }

  override def validate(value: FormValue, errors: Errors): Unit = {}
}

@Entity
@Proxy
@DiscriminatorValue("file")
class FileField extends AssignmentFormField {
  def blankFormValue = new FileFormValue(this)

  def populatedFormValue(savedFormValue: SavedFormValue): FileFormValue = blankFormValue

  def attachmentLimit: Int = getProperty[JInteger]("attachmentLimit", 1)

  def attachmentLimit_=(limit: Int): Unit = setProperty("attachmentLimit", limit)

  def unlimitedAttachments: JBoolean = getProperty[JBoolean]("unlimitedAttachments", false)

  def unlimitedAttachments_=(unlimited: JBoolean): Unit = setProperty("unlimitedAttachments", unlimited)

  def minimumAttachmentLimit: Int = getProperty[JInteger]("minimumAttachmentLimit", 1)

  def minimumAttachmentLimit_=(limit: Int): Unit = setProperty("minimumAttachmentLimit", limit)

  // List of extensions.
  def attachmentTypes: Seq[String] = getProperty[Seq[String]]("attachmentTypes", Seq())

  def attachmentTypes_=(types: Seq[String]): Unit = setProperty("attachmentTypes", types: Seq[String])

  def individualFileSizeLimit: JInteger = getProperty[JInteger]("individualFileSizeLimit", null)

  def individualFileSizeLimit_=(limit: JInteger): Unit = setProperty("individualFileSizeLimit", limit)

  private def individualFileSizeLimitInBytes: JLong = JLong(Option(individualFileSizeLimit).map(_.longValue() * 1024 * 1024))

  // This is after onBind is called, so any multipart files have been persisted as attachments
  override def validate(value: FormValue, errors: Errors): Unit = {

    /** Are there any duplicate values (ignoring case)? */
    def hasDuplicates(names: Seq[String]) = names.size != names.map(_.toLowerCase).distinct.size

    value match {
      case v: FileFormValue =>
        if (v.file.isMissing) {
          errors.rejectValue("file", "file.missing")
        } else if (v.file.size > attachmentLimit && !unlimitedAttachments) {
          if (attachmentLimit == 1) errors.rejectValue("file", "file.toomany.one")
          else errors.rejectValue("file", "file.toomany", Array(attachmentLimit: JInteger), "")
        } else if (v.file.size < minimumAttachmentLimit) {
          errors.rejectValue("file", "file.minimum", Array(minimumAttachmentLimit: JInteger), "")
        } else if (hasDuplicates(v.file.fileNames)) {
          errors.rejectValue("file", "file.duplicate")
        }

        if (attachmentTypes.nonEmpty) {
          val attachmentStrings = attachmentTypes.map(s => "." + s)
          val fileNames = v.file.fileNames map (_.toLowerCase)
          val invalidFiles = fileNames.filter(s => !attachmentStrings.exists(s.endsWith))
          if (invalidFiles.nonEmpty) {
            if (invalidFiles.size == 1) errors.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), attachmentStrings.mkString(", ")), "")
            else errors.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), attachmentStrings.mkString(", ")), "")
          }
        }

        if (Option(individualFileSizeLimit).nonEmpty) {
          val invalidFiles = v.file.individualFileSizes.filter(_._2 > individualFileSizeLimitInBytes)
          if (invalidFiles.nonEmpty) {
            if (invalidFiles.size == 1) errors.rejectValue("file", "file.toobig.one", Array(invalidFiles.map(_._1).mkString(""), individualFileSizeLimit), "")
            else errors.rejectValue("file", "file.toobig", Array(invalidFiles.map(_._1).mkString(", "), individualFileSizeLimit), "")
          }
        }
    }
  }

  override def duplicate(newAssignment: Assignment): FileField = {
    val newField = new FileField
    newField.assignment = newAssignment
    newField.name = Assignment.defaultUploadName
    newField.label = this.label
    newField.context = this.context
    newField.attachmentLimit = this.attachmentLimit
    newField.unlimitedAttachments = this.unlimitedAttachments
    newField.attachmentTypes = this.attachmentTypes
    newField.individualFileSizeLimit = this.individualFileSizeLimit
    newField
  }
}

sealed abstract class FormFieldContext(val dbValue: String, val description: String)

object FormFieldContext {

  case object Submission extends FormFieldContext("submission", "Submission")

  case object Feedback extends FormFieldContext("feedback", "Feedback")

  def fromCode(code: String): FormFieldContext = code match {
    case Submission.dbValue => Submission
    case Feedback.dbValue => Feedback
    case _ => throw new IllegalArgumentException()
  }
}

class FormFieldContextUserType extends AbstractBasicUserType[FormFieldContext, String] {

  val basicType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  val nullValue = null
  val nullObject = null

  override def convertToObject(string: String): FormFieldContext = FormFieldContext.fromCode(string)

  override def convertToValue(context: FormFieldContext): String = context.dbValue

}
