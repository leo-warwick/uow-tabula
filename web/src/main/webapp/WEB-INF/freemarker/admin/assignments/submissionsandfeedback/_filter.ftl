<#-- FIXME: implemented as part of CM2 migration but will require further reworking due to CM2 workflow changes -->
<@f.form method="post" action="${info.requestedUri.path}" cssClass="form-inline filter-form" commandName="submissionAndFeedbackCommand">
	<div class="filter">
		<label for="filter">Show all</label>
		&nbsp;
		<@f.select path="filter" cssClass="span4">
			<@f.options items=allFilters itemValue="name" itemLabel="description" />
		</@f.select>

		<#list allFilters as filter>
			<#if filter.parameters?size gt 0>
				<fieldset data-filter="${filter.name}" class="form-horizontal filter-options"<#if filter.name != submissionAndFeedbackCommand.filter.name> style="display: none;"</#if>>
					<#list filter.parameters as param>
						<@form.labelled_row "filterParameters[${param._1()}]" param._2()>
							<#if param._3() == 'datetime'>
								<@f.input path="filterParameters[${param._1()}]" cssClass="date-time-picker" />
							<#elseif param._3() == 'percentage'>
								<div class="input-append">
									<@f.input path="filterParameters[${param._1()}]" type="number" min="0" max="100" cssClass="input-small" />
									<span class="add-on">%</span>
								</div>
							<#else>
								<@f.input path="filterParameters[${param._1()}]" type=param._3() />
							</#if>
						</@form.labelled_row>
					</#list>

					<@form.row>
						<@form.field>
							<button class="btn btn-primary" type="submit">Filter</button>
						</@form.field>
					</@form.row>
				</fieldset>
			</#if>
		</#list>
	</div>

	<script type="text/javascript">
		jQuery(function($) {
			$('.filter-form select[name=filter]').on('keyup change', function() {
				var $select = $(this);
				var val = $select.val();

				var $options = $select.closest('form').find('.filter fieldset[data-filter="' + val + '"]');
				var $openOptions = $select.closest('form').find('.filter .filter-options:visible');

				var cb = function() {
					if ($options.length) {
						$options.slideDown();
					} else {
						$select.closest('form').submit();
					}
				};

				if ($openOptions.length) {
					$openOptions.slideUp('fast', cb);
				} else {
					cb();
				}
			});
		});
	</script>
</@f.form>