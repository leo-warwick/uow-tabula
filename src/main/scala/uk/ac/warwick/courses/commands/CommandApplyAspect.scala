package uk.ac.warwick.courses.commands

import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.events.EventHandling
import uk.ac.warwick.courses.services.MaintenanceModeService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.services.MaintenanceModeEnabledException
import uk.ac.warwick.courses.services.MaintenanceModeEnabledException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Configurable 
@Aspect
class CommandApplyAspect extends EventHandling {
	
	@BeanProperty @Autowired var maintenanceMode:MaintenanceModeService =_
	
	@Pointcut("execution(* uk.ac.warwick.courses.commands.Command.apply(..)) && target(callee)")
	def applyCommand(callee:Describable[_]): Unit = {}
	
	@Around("applyCommand(callee)")
	def aroundApplyCommand[T](jp:ProceedingJoinPoint, callee:Describable[T]):Any = 
		if (!maintenanceMode.enabled || callee.isInstanceOf[ReadOnly]) 
			recordEvent(callee) { jp.proceed.asInstanceOf[T] }
		else 
			throw maintenanceMode.exception()
	
}
