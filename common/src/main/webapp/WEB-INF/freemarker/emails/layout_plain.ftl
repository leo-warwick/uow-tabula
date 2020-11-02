Dear ${recipient.firstName!},

${content?trim}

<#if actionRequired!false>You need to ${urlTitle!"visit the page for futher info"}. Please visit ${url}<#else>To ${urlTitle!"see futher info"}, please visit ${url}</#if>

Thank you,
Tabula

This email was sent from an automated system and replies to it will not reach a real person.
