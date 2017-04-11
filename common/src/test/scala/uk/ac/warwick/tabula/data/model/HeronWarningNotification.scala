package uk.ac.warwick.tabula.data.model

import javax.persistence.{ManyToOne, DiscriminatorValue, Entity}
import uk.ac.warwick.userlookup.User
import org.hibernate.annotations.Type

object HeronWarningNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl"
	val heronRant = "They are after your delicious eye jelly. Throw rocks at them!"
}

@Entity
@DiscriminatorValue(value="HeronWarning")
class HeronWarningNotification extends Notification[Heron, Unit]
	with SingleItemNotification[Heron] with SingleRecipientNotification {

	import HeronWarningNotification._

	val verb: String = "Heron"

	def title: String = "You all need to know. Herons would love to kill you in your sleep"
	def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))
	def url: String = "/beware/herons"
	def urlTitle = "see how evil herons really are"
	def recipient: User = item.entity.victim

}

@Entity
@DiscriminatorValue(value="HeronDefeat")
class HeronDefeatedNotification extends Notification[Heron, Unit]
with SingleItemNotification[Heron] with SingleRecipientNotification {

	import HeronWarningNotification._

	val verb: String = "Heron"

	def title: String = "A heron has been defeated. Rejoice"
	def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))
	def url: String = "/beware/herons"
	def urlTitle = "wallow in glory"
	def recipient: User = item.entity.victim

}

@Entity
class Heron extends GeneratedId with ToEntityReference {

	def this(v: User) = {
		this()
		victim = v
	}

	type Entity = Heron
	def toEntityReference: HeronEntityReference = new HeronEntityReference().put(this)

	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	var victim: User = null
}

@Entity @DiscriminatorValue(value="heron")
class HeronEntityReference extends EntityReference[Heron] {
	@ManyToOne()
	var entity: Entity = null
}
/*
                                       _____
                                 _.::::::::::::-.
                             _.-:::::='=::=. _   `.
                         _.-'.:'          ""(@)`"- `-----....__
                     _.-'-'.:::....            `' -----------__:=
                  .-'-''.::::::::::._    .       .'""""""""""
                   _.-::::::::::::::')    `-- - :
                .-:::::::::::::'     /  ._.'   )
             .-::::::::::::'    _   .'  .__.:' :
           .::::::::::'      -=' _.'--.  .   ..'
         .:::::::'           _.-'      )  :-'.'
       .::::'  ___..==''          .-   : .  .'
     .::'" _.           _.- _.-        ' .-'
    :'         __.'     _=-'    _.' __' :/
   ,' _.=  _.-==_.  -==        __.. .-'.'
  .'.-_.        _      __..--__..-"_.-''
 .' \ \_.=   -'  __..-'__.--==. .'"
.'\ \_.-= __... __.--'\\     / /
\\__...__.--''"'       \\   ("(
 /_.--'                 \\_.'`.`.
                       .:._.'  `.`.,.     _
                     =''   `:,   `.`.`..-::'
                                   `.` :"
                                   .:.-.`.
                                 .:'    `.:
                                          '
 */