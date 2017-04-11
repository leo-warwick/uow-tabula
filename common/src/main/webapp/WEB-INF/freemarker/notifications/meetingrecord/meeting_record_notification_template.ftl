${actor.fullName} has ${verbed} a record of your ${role} meeting:

${meetingRecord.title!'A meeting you no longer have permission to view'} at ${dateFormatter.print(meetingRecord.meetingDate)}
<#if reason??>

Because: "${reason}"
</#if>