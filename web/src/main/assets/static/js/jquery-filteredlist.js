/**

jQuery.filteredList

A configurable plugin to define a set of items that can be filtered
by a number of options. Works well in conjunction with jQuery.dragAndDrop
for filtering items in the return list.

Run the plugin on a list that contains all the items:

    $('#tutees-list').filteredList();

This must contain at least one or more .filter elements that have form
controls containing the filter type. These form controls must each have
a data-filter-attr attribute set to the camel-cased data attribute that
each item has, and a data-filter-value with the filtered value.

For example (using both checkboxes and a dropdown):

		<div class="item-list tabula-filtered-list">
			<div class="filters">
				<div class="filter">
					<label>
						Male
						<input type="checkbox" data-filter-attr="fGender" data-filter-value="M" checked="checked">
					</label>
					<label>
						Female
						<input type="checkbox" data-filter-attr="fGender" data-filter-value="F" checked="checked">
					</label>
				</div>
				<div class="filter">
					<select data-filter-attr="fYear">
						<option data-filter-value="1" selected="selected">Year 1</option>
						<option data-filter-value="2">Year 2</option>
						<option data-filter-value="3">Year 3</option>
					</select>
				</div>
			</div>

			<ul class="items">
				<li class="student" data-f-gender="M" data-f-year="1">Student 1</li>
				<li class="student" data-f-gender="M" data-f-year="2">Student 2</li>
				<li class="student" data-f-gender="F" data-f-year="3">Student 3</li>
			</ul>
		</div>

Adding the class tabula-filtered-list to the container allows for automatic
configuration without being invoked via Javascript.

A special-case (for <select> filters) is data-filter-value="*", which effectively
disables that filter by allowing any value to be displayed.

Options: (all of these options can be set as data- attributes)
 - itemSelector (default: 'li')
 - filterSelector (default: '.filter')
 - filterControls (default: 'input[type="checkbox"],select')

*/
(function($){ "use strict";

	var DataName = "tabula-filtered-list";

  var FilteredList = function(element, options) {
  	var self = this; // take a selfie
  	var $el = $(element);

    if (options && typeof(options) === 'object') this.options = options;
    else this.options = {};

    // Extract some options out to vars with defaults
	var itemSelector = this.options.itemSelector || $el.data('item-selector')  || 'li';
	var filterSelector = this.options.filterSelector || $el.data('filter-selector') || '.filter';
	var filterControls = this.options.filterControls || $el.data('filter-controls') || 'input[type="checkbox"],select';

		this.filter = function() {
			var items = $el.find(itemSelector);
			var controls = $el.find(filterSelector).find(filterControls);

			// get a list of (json-stringed) name/value pairs for attributes to hide
    	var hidden = controls.map(function(e,control) {
        var $control = $(control);

        switch ($control.prop('tagName').toLowerCase()) {
        	case 'input':
        		switch ($control.attr('type').toLowerCase()) {
        			case 'checkbox':
								if ( ! $control.is(":checked")) { // unchecked means we will hide any items with this attribute
        					var hideThis = {};
           				hideThis[$control.data("filter-attr")] = $control.data("filter-value");
           				return JSON.stringify(hideThis);
        				}
        				break;
        			default:
        				console.error('Unsupported filter control: input[type=' + $control.attr('type') + ']');
        		}
        		break;
        	case 'select':
        		// Special case - if the selected value is * then we ignore this filter
        		if ($control.find('option:selected').data('filter-value') === '*') {
        			return;
        		}

        		return $control.find('option[data-filter-value]').map(function(i, opt) {
        			var $option = $(opt);
        			if (! $option.is(":selected")) { // unselected means we will hide any items with this attribute
        				var hideThis = {};
           			hideThis[$control.data("filter-attr")] = $option.data("filter-value");
           			return JSON.stringify(hideThis);
        			}
        		}).get();
        	default:
        		console.error('Unsupported filter control: ' + $control.prop('tagName'));
        }
    	});

    	function setVisibility(element, hiddenAttrs) {
		    var $element = $(element);
		    var data = $element.data();

		    // convert any data-f-* attributes into JSON
		    // n.b. jQuery data() camel-cases attributes;
		    // it converts data-f-Bar="foo" into data()[fBar]=foo
		    var stringData = [];
		    for (var prop in data){
		      if (prop.match("^f[A-Z]")){
		        var o = {};
		        o[prop] = data[prop];
		        stringData.push(JSON.stringify(o));
		      }
		    }

		    // if this element has any attributes on the hidden list, it
		    // should not be visible. Otherwise, show it.
		    var visible = true;
		    $(hiddenAttrs).each(function(i,attr){
		      if ($.inArray(attr, stringData) > -1){
		        visible = false;
		        return false; // break out of the loop early
		      }
		    });
		    $element.toggle(visible);
			}

      // now go through all the items and hide/show each as appropriate
	    items.each(function(i, ele) {
	    	setVisibility(ele, hidden);
	    });

	    $el.trigger('filteredList.changed');
		};

		$el.find(filterSelector).find(filterControls).on('change input keyup', this.filter);
		this.filter();
	};

  // The jQuery plugin itself is a basic adapter around FilteredList
  $.fn.filteredList = function(options) {
		var filter = this.data(DataName);

    if (options === 'filter') {
      filter.filter();
    } else {
    	this.each(function(i, element) {
      	filter = new FilteredList(element, options);
      	$(element).data(DataName, filter);
			});
		}

		return this;
	};
})(jQuery);