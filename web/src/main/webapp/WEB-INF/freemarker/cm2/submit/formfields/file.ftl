<#assign maxFiles=field.attachmentLimit />
<#assign fileTypes=field.attachmentTypes />
<@bs3form.filewidget basename="fields[${field.id}].file" types=fileTypes multiple=(maxFiles gt 1) max=maxFiles maxFileSize=field.individualFileSizeLimit! labelText="Assignment file(s)"/>