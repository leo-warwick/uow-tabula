Hello ${admin.firstName}

A user (${student.fullName} in ${student.departmentName}) has requested access to the assignment "${assignment.name}" in module ${assignment.module.code?upper_case}, because they believe that they should have access to this assignment.

If you agree that they should have access, you can update the assignment membership here:

<@url page=path context="/coursework" />


In any case, you may wish to contact the user at ${student.email}. This email has been sent to all department administrators.


This email was sent from an automated system. Your email address was not revealed to the message sender, but it will be if you reply to them.