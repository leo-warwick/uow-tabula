package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions.GodMode

class ComposableCommandTest extends TestBase with Mockito{

	/**
	 * A trait describing the interface to our command which clients (controllers) or mixed-in components want to see
	 */
	trait HelloCommand{
		 val name:String
	}
	/**
	 * The implementation of applyInternal
	 */
	class HelloCommandInternals(val name:String) extends HelloCommand with CommandInternal[String]{
		def applyInternal(): String = "hello " + name

	}

	/**
	 * The permissions for our command
	 */
	trait HelloCommandPermissions extends RequiresPermissionsChecking{
		this:HelloCommand=>

		def permissionsCheck(p: PermissionsChecking) {
			if (name!= "noperms"){
				p.PermissionCheck(GodMode)
			}
		}
	}

	/**
	 * The notification implementation for our command
	 */
	trait HelloCommandNotification extends Describable[String]{
		this:HelloCommand=>
		def describe(d: Description) {d.property("name",name)}
	}

	/**
	 * Fixture data
	 */
	val mockDescribable: Describable[String] = mock[Describable[String]]
	val mockPerms: RequiresPermissionsChecking = mock[RequiresPermissionsChecking]
	val sampleName="Reverend Arthur Belling"


	@Test def composedCommandIsAValidCommand(){
		val realCommand = new HelloCommandInternals(sampleName) with ComposableCommand[String] with HelloCommandPermissions with HelloCommandNotification

		realCommand.isInstanceOf[Command[String]] should be(true)
	}

	@Test def canComposeCommandWithMocksForTesting(){

		val stubbedCommand = new HelloCommandInternals(sampleName) with RequiresPermissionsChecking with Describable[String]{
			def describe(d: Description) {
				mockDescribable.describe(d)
			}
			val eventName: String = name
			def permissionsCheck(p: PermissionsChecking) {mockPerms.permissionsCheck(p)}
		}

		stubbedCommand.applyInternal() should  be ("hello " + sampleName)
	}

	@Test def canSeeInternalCommandsInterfaceAfterComposition(){
		val realCommand = new HelloCommandInternals(sampleName) with ComposableCommand[String] with HelloCommandPermissions with HelloCommandNotification
		realCommand.name should be (sampleName)
	}

	@Test def UnauditedTraitIsVisibleOnCommand(){
		val realCommand = new HelloCommandInternals(sampleName) with ComposableCommand[String] with HelloCommandPermissions with Unaudited

		realCommand.isInstanceOf[Unaudited] should be(true)
	}

	@Test def CanTestPermissionsIndependently(){
		val perms = new HelloCommandPermissions with HelloCommand {
			val name: String = sampleName
		}
		val mockChecking = mock[PermissionsChecking]
		perms.permissionsCheck(mockChecking)

		verify(mockChecking, times(1)).PermissionCheck(GodMode)

		// now try again with the magic name, to show that state in HelloCommand is visible to HelloCommandPermisions
		val noPerms = new HelloCommandPermissions with HelloCommand {
			val name = "noperms"
		}
		val mockChecking2 = mock[PermissionsChecking]
		noPerms.permissionsCheck(mockChecking2)

		verify(mockChecking2, times(0)).PermissionCheck(GodMode)
	}

	@Test def CanTestNotificationIndependently(){
		val desc = new HelloCommandNotification with HelloCommand {
			val name: String = sampleName
			val eventName = "test"
		}
		val description = new DescriptionImpl
		desc.describe(description)
		description.allProperties("name") should be(sampleName)
	}


}
