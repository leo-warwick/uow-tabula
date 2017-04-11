<#assign maxFiles=1 />
<#assign fileTypes=".xlsx" />
<@form.filewidget basename="allocate" types=fileTypes multiple=(maxFiles gt 1) max=maxFiles />
