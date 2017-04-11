<#escape x as x?html>
<h1>Evaluator</h1>
<div class="row">

	<div class="col-md-9">

		<@f.form method="post" action="${url('/sysadmin/repl')}">
			<input name="query" style="width:100%" value="${query!""}" class="form-control" />
			<br>
			<input type="submit" class="btn btn-default" value="Evaluate">
		</@f.form>

		<#if response.value??>
			<#assign stringValue=response.stringValue/>
		<#elseif response.none>
			<#assign stringValue=None/>
		</#if>

		<#if stringValue??>
			<pre class="well"><code>
			${stringValue}: ${response.valueType}
			</code></pre>
		</#if>
		<#if response.exception??>
			<div class="alert alert-danger">
				<h3>${response.exception.class.simpleName}</h3>
				${response.exception.message!''}
				<pre>
					${response.stackTrace!''}
				</pre>
			</div>
		</#if>

	</div>

	<div class="col-md-3">

		<p>Available variables: <code>session</code>, <code>beanFactory</code>.</p>
		<p>Available maps: <code>assignments</code>, <code>departmentCodes</code>.</p>

		<p>
			<a href="http://static.springsource.org/spring/docs/current/spring-framework-reference/html/expressions.html#expressions-intro">REPL reference</a>
		</p>

	</div>

</div>

</#escape>