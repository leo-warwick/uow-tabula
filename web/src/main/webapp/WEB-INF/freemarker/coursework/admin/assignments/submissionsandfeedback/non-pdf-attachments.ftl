<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body {
			font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
		}
	</style>
	<title></title>
</head>
<body>
<p>This submission contains files that are not PDFs. These files have been ignored:</p>
<ul>
	<#list attachments as attachment>
		<li>${attachment}</li>
	</#list>
</ul>
</body>
</html>
</#escape>