(function($){ // I'm wrapping all day

/**
 * RichResultField is a text field that can be overlaid with a similar-looking
 * box containing arbitrary text. Useful if you want to save a particular value
 * from a picker but display something more friendly from the user.
 *
 * Requires Bootstrap.
 */
var RichResultField = function (input) {
  var self = this;
  this.$input = $(input);
  this.$uneditable = $('<span><span class=val></span>'+
             '<a href=# class="clear-field" title="Clear">&times;</a></span>');
  this.$uneditable.attr('class', 'uneditable-input rich-result-field ' + this.$input.attr('class'));
  this.$input.after(this.$uneditable);
  this.$uneditable.find('a').click(function () {
    self.edit();
    return false;
  });
  this.$uneditable.hide();
};

/** Clear field, focus for typing */
RichResultField.prototype.edit = function () {
  this.$input.val('').show().trigger('change').focus();
  this.$uneditable.hide().find('.val').text('');
};

/** Hide input field and show the rich `text` instead */
RichResultField.prototype.storeText = function (text) {
  this.$input.hide();
  this.$uneditable.show().find('.val').text(text);
};

/** Set value of input field, hide it and show the rich `text` instead */
RichResultField.prototype.store = function (value, text) {
  this.$input.val(value).trigger('change').hide();
  this.$uneditable.show().find('.val').text(text);
};

/**
 * Creates a Bootstrap Typeahead but with added functionality needed by FlexiPicker and ModulePicker
 */
var TabulaTypeahead = function(options) {
	var $typeahead = options.element.typeahead({
		source: options.source,
		item: options.item
	}).data('typeahead');

	// Overridden lookup() method uses this to delay the AJAX call
	$typeahead.delay = 100;

	// Provides delayed AJAX lookup - called by lookup() method.
	$typeahead._delaySource = function(query, source, process) {
		if(this.calledOnce === undefined)  {
			this.calledOnce = true;
			var items = source(query, process);
			if (items) {
				this.delay = 0;
				return items;
			}
		} else {
			if (this.delay === 0) {
				return source(query, process);
			} else {
				if(this.timeout) {
					clearTimeout(this.timeout);
				}
				this.timeout = setTimeout(function() {
					return source(query, process);
				}, this.delay);
			}
		}
	};

	// Disable clientside sorting - server will sort where appropriate.
	$typeahead.sorter = function(items) { return items; };

	// The server will filter - don't do clientside filtering
	$typeahead.matcher = function() { return true; };

	// Mostly the same as the original but delays the lookup if a delay is set.
	$typeahead.lookup = function () {
		var items;
		this.query = this.$element.val();
		if (!this.query || this.query.length < this.options.minLength) {
			return this.shown ? this.hide() : this
		}
		items =  ($.isFunction(this.source)) ? this._delaySource(this.query, this.source, $.proxy(this.process, this)) : this.source;
		return items ? this.process(items) : this
	};

	return $typeahead;

};

/**
 * An AJAX autocomplete-style picker that can return a variety of different
 * result types, such as users, webgroups, and typed-in email addresses.
 *
 * The actual searching logic is handled by the corresponding controller -
 * this plugin just passes it option flags to tell it what to search.
 *
 * Requires Bootstrap with the Typeahead plugin.
 */
var FlexiPicker = function (options) {
	var self = this;
	var $element = $(options.input);

	// Might have manually wired this element up with FlexiPicker,
	// but add the class for CSS style purposes.
	if (!$element.hasClass('flexi-picker')) {
		$element.addClass('flexi-picker');
	}

	// Disable browser autocomplete dropdowns, it gets in the way.
	$element.attr('autocomplete', 'off');

	var $spinner = $('<div />').addClass('spinner-container');
	$element.before($spinner);

	this.includeUsers = options.includeUsers !== false;
	this.includeGroups = options.includeGroups || false;
	this.includeEmail = options.includeEmail || false;
	this.tabulaMembersOnly = options.tabulaMembersOnly || false;
	this.prefixGroups = options.prefixGroups || '';
	this.universityId = options.universityId || false;

	this.richResultField = new RichResultField($element[0]);

	this.iconMappings = {
		user: 'user',
		group: 'globe',
		email: 'envelope'
	};

	var $typeahead = new TabulaTypeahead({
		element: $element,
		source: function (query, process) {
			$spinner.spin('small');
			self.search(query).done(function (results) {
				$spinner.spin(false);
				process(results);
			});
		},
		item: '<li class=flexi-picker-result><a href="#"><i></i><span class=title></span><span class=type></span><div class=description></div></a></li>'
	});

	// Renders each result item with icon and description.
	$typeahead.render = function (items) {
		var that = this;

		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.attr('data-value', item.value);
				i.attr('data-type', item.type);
				i.attr('data-fullname', item.name);
				i.find('span.title').html(that.highlighter(item.title));
				i.find('span.type').html(item.type);
				i.find('i').addClass('icon-' + self.iconMappings[item.type]);
				var desc = i.find('.description');
				if (desc && desc != '') {
					  desc.html(item.description).show();
				} else {
					desc.hide();
				}
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});

		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};

	// On selecting a value, we can transform it here before it's stored in the field.
	$typeahead.updater = function(value) {
		var type = this.$menu.find('.active').attr('data-type');
		return self.getValue(value, type);
	};

	// Override select item to place both the value in the field
	// and a more userfriendly text in the "rich result".
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		var text = this.$menu.find('.active .title').text();
		var desc = this.$menu.find('.active .description').text();
		if (desc) { text = text + ' (' + desc + ')'; }
		self.richResultField.storeText(text);
		this.$element.data('fullname', this.$menu.find('.active').data('fullname'));
		return oldSelect.call($typeahead);
	};

	// On load, look up the existing value and give it human-friendly text if possible
	// NOTE: this relies on the fact that the saved value is itself a valid search term
	// (excluding the prefix on webgroup, which is handled by getQuery() method).
	var currentValue = $element.val();
	if (currentValue && currentValue.trim().length > 0) {
		var searchQuery = this.getQuery(currentValue);
		this.search(searchQuery, { exact: true }).done(function(results){
			if (results.length > 0) {
				self.richResultField.storeText(results[0].title + ' (' + results[0].description + ')');
			}
		});
	}

	// The Bootstrap Typeahead always appends the drop-down to directly after the input
	// Replace the show method so that the drop-down is added to the body
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};
};

// Extract the value from a chosen value with type.
FlexiPicker.prototype.getValue = function(value, type) {
	if (type == 'group') {
		value = this.prefixGroups + value;
	}
	return value;
};

// Turn value into something we can query on.
FlexiPicker.prototype.getQuery = function(value) {
	// If we prefixed something onto a group name, remove it first
	if (this.prefixGroups && value.indexOf(this.prefixGroups) == 0) {
		value = value.substring(this.prefixGroups.length);
	}
	return value;
};

FlexiPicker.prototype.transformItem = function(item) {
	if (item.type == 'user') {
		item.title = item.name;
		item.description = item.value + ', ' + ((item.isStaff === 'true')?'Staff':'Student') + ', ' + item.department; // usercode, staff/student, department
	} else if (item.type == 'group') {
		item.description = item.title;
		item.title = item.value;
	} else if (item.type == 'email') {
		item.title = item.name;
		item.description = item.address;
	}
};

/** Runs the search */
FlexiPicker.prototype.search = function (query, options) {
	// We'll return a Deferred result, that will get resolved by the AJAX call
	options = options || {};
	var d = $.Deferred();
	var self = this;
	// Abort any existing search
	if (this.currentSearch) {
		this.currentSearch.abort();
		this.currentSearch = null;
	}
	this.currentSearch = $.ajax({
		url: '/ajax/flexipicker/query.json',
		dataType: 'json',
		data: {
			includeUsers: this.includeUsers,
			includeGroups: this.includeGroups,
			includeEmail: this.includeEmail,
			tabulaMembersOnly: this.tabulaMembersOnly,
			universityId: this.universityId,
			query: query,
			exact: options.exact // if true, only returns 100% matches.
		},
		success: function(json) {
			if (json.data && json.data.results) {
				var results = json.data.results;
				// Massage incoming JSON a bit to fill in title and description.
				$.each(results, function(i, item){
					self.transformItem(item);
				});
				// Resolve the deferred result, triggering any handlers
				// that may have been registered against it.
				d.resolve(results);
			}
		}
	});
	// unset the search when it's done
	this.currentSearch.always(function(){
		self.currentSearch = null;
	});
	return d;
};

// The jQuery plugin
$.fn.flexiPicker = function (options) {
	this.each(function () {
		var $this = $(this);
		if ($this.data('flexi-picker')) {
			throw new Error("FlexiPicker has already been added to this element.");
		}
		var allOptions = {
			input: this,
			includeGroups: $this.data('include-groups'),
			includeEmail: $this.data('include-email'),
			includeUsers: $this.data('include-users') !== false,
			tabulaMembersOnly: $this.data('members-only'),
			prefixGroups: $this.data('prefix-groups') || '',
			universityId: $this.data('universityid')
		};
		$.extend(allOptions, options || {});
		$this.data('flexi-picker', new FlexiPicker(allOptions));
	});
	return this;
};

/**
 * Any input with the flexi-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 *
 * More likely you'd use the flexi-picker tag.
 */
jQuery(function($){
	$('.flexi-picker').flexiPicker({});

	var emptyValue = function() {
		return (this.value||"").trim() == "";
	};

	/*
	* Handle the multiple-flexi picker, by dynamically expanding to always
	* have at least one empty picker field.
	*/
	// Each set of pickers will be in a .flexi-picker-collection
	var $collections = $('.flexi-picker-collection');
	$collections.each(function(i, collection){
		var $collection = $(collection),
			$blankInput = $collection.find('.flexi-picker-container').first().clone()
				.find('input').val('').end();
		$blankInput.find('a.btn').remove(); // this button is added by initFlexiPicker, so remove it now or we'll see double

		// check whenever field is changed or focused
		if (!!($collection.data('automatic'))) {
			$collection.on('change focus', 'input', function(ev) {
				// remove empty pickers
				var $inputs = $collection.find('input');
				if ($inputs.length > 1) {
					var toRemove = $inputs.not(':focus').not(':last').filter(emptyValue).closest('.flexi-picker-container');
					toRemove.remove();
				}

				// if last picker is nonempty OR focused, append an blank picker.
				var $last = $inputs.last();
				var lastFocused = (ev.type == 'focusin' && ev.target == $last[0]);
				if (lastFocused || $last.val().trim() != '') {
					var input = $blankInput.clone();
					$collection.append(input);
					input.find('input').first().flexiPicker({});
				}
			});
		} else {
			$collection.append(
				$('<button />')
					.attr({'type':'button'})
					.addClass('btn').addClass('btn-mini btn-xs btn-default')
					.html('<i class="icon-plus"></i> Add another')
					.on('click', function() {
						var input = $blankInput.clone();
						$(this).before(input);
						input.find('input').first().flexiPicker({});
					})
			);
		}
	});

});


/**
 * Like the FlexiPicker, but for Modules
 */
var ModulePicker = function (options) {
	var self = this;
	var $element = $(options.input);

	// Might have manually wired this element up with an existing picker,
	// but add the class for CSS style purposes.
	if (!$element.hasClass('module-picker')) {
		$element.addClass('module-picker');
	}

	// Disable browser autocomplete dropdowns, it gets in the way.
	$element.attr('autocomplete', 'off');

	var $typeahead = new TabulaTypeahead({
		element: $element,
		source: function(query, process){
			// Abort any existing search
			if (self.currentSearch) {
				self.currentSearch.abort();
				self.currentSearch = null;
			}
			self.currentSearch = $.ajax({
				url: '/ajax/modulepicker/query',
				dataType: 'json',
				data: {
					query: query,
					checkGroups: options.checkGroups,
					checkAssignments: options.checkAssignments
				},
				success: function(data) {
					process(data)
				}
			});
		},
		item: '<li class="flexi-picker-result module"><a href="#"><div class="name"></div><div class=department></div><div class="no-groups" style="display:none;"><i class="icon-exclamation-sign"></i> This module has no small groups set up in Tabula</div><div class="no-assignments" style="display:none;"><i class="icon-exclamation-sign"></i> This module has no assignments set up in Tabula</div></a></li>'
	});

	// Renders each result item with icon and description.
	$typeahead.render = function (items) {
		var that = this;

		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.attr('data-moduleid', item.id);
				i.attr('data-modulecode', item.code);
				i.find('div.name').html(that.highlighter(item.code.toUpperCase() + ' ' + item.name));
				i.find('div.department').html(item.department);
				if (options.checkGroups) {
					if(!item.hasSmallGroups) {
						i.find('div.no-groups').show();
						i.attr('data-hasgroups', false);
					} else {
						i.find('div.no-groups').hide();
						i.attr('data-hasgroups', true);
					}
				}
				if (options.checkAssignments) {
					if(!item.hasAssignments) {
						i.find('div.no-assignments').show();
						i.attr('data-hasassignments', false);
					} else {
						i.find('div.no-assignments').hide();
						i.attr('data-hasassignments', true);
					}
				}
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});

		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};

	// The Bootstrap Typeahead always appends the drop-down to directly after the input
	// Replace the show method so that the drop-down is added to the body
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};

	// Override select item to store the relevant attributes
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		this.$element.data('moduleid', this.$menu.find('.active').data('moduleid'));
		this.$element.data('modulecode', this.$menu.find('.active').data('modulecode'));
		this.$element.data('hasgroups', this.$menu.find('.active').data('hasgroups'));
		this.$element.data('hasassignments', this.$menu.find('.active').data('hasassignments'));
		return oldSelect.call($typeahead);
	};

	$typeahead.updater = function() {
		return this.$menu.find('.active .name').text();
	};
};

// The jQuery plugin
$.fn.modulePicker = function (options) {
	this.each(function () {
		var $this = $(this);
		if ($this.data('module-picker')) {
			throw new Error("FlexiPicker has already been added to this element.");
		}
		var allOptions = {
			input: this,
			checkGroups: false,
			checkAssignments: false
		};
		$.extend(allOptions, options || {});
		$this.data('module-picker', new ModulePicker(allOptions));
	});
	return this;
};

/**
 * Any input with the module-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 */
jQuery(function($){
	$('.module-picker').modulePicker({});
});


/**
 * Like the ModulePicker, but for Assignments
 */
var AssignmentPicker = function (options) {
	var self = this;
	var $element = $(options.input);

	// Might have manually wired this element up with an existing picker,
	// but add the class for CSS style purposes.
	if (!$element.hasClass('assignment-picker')) {
		$element.addClass('assignment-picker');
	}

	// Disable browser autocomplete dropdowns, it gets in the way.
	$element.attr('autocomplete', 'off');

	var $typeahead = new TabulaTypeahead({
		element: $element,
		source: function(query, process){
			// Abort any existing search
			if (self.currentSearch) {
				self.currentSearch.abort();
				self.currentSearch = null;
			}
			self.currentSearch = $.ajax({
				url: '/ajax/assignmentpicker/query',
				dataType: 'json',
				data: {
					query: query
				},
				success: function(data) {
					process(data)
				}
			});
		},
		item: '<li class="flexi-picker-result assignment"><a href="#"><div class="name"></div><div class=department></div></a></li>'
	});

	// Renders each result item with icon and description.
	$typeahead.render = function (items) {
		var that = this;

		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.attr('data-assignmentid', item.id);
				i.find('div.name').html(that.highlighter(item.module.toUpperCase() + ' ' + item.name));
				i.find('div.department').html(item.department);
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});

		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};

	// The Bootstrap Typeahead always appends the drop-down to directly after the input
	// Replace the show method so that the drop-down is added to the body
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};

	// Override select item to store the relevant attributes
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		this.$element.data('assignmentid', this.$menu.find('.active').data('assignmentid'));
		return oldSelect.call($typeahead);
	};

	$typeahead.updater = function() {
		return this.$menu.find('.active .name').text();
	};
};

// The jQuery plugin
$.fn.assignmentPicker = function (options) {
	this.each(function () {
		var $this = $(this);
		if ($this.data('assignment-picker')) {
			throw new Error("FlexiPicker has already been added to this element.");
		}
		var allOptions = {
			input: this
		};
		$.extend(allOptions, options || {});
		$this.data('assignment-picker', new AssignmentPicker(allOptions));
	});
	return this;
};

/**
 * Any input with the assignment-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 */
jQuery(function($){
	$('.assignment-picker').assignmentPicker({});
});


/**
 * Like the ModulePicker, but for Routes
 */
var RoutePicker = function (options) {
	var self = this;
	var $element = $(options.input);

	// Might have manually wired this element up with an existing picker,
	// but add the class for CSS style purposes.
	if (!$element.hasClass('route-picker')) {
		$element.addClass('route-picker');
	}

	// Disable browser autocomplete dropdowns, it gets in the way.
	$element.attr('autocomplete', 'off');

	var $typeahead = new TabulaTypeahead({
		element: $element,
		source: function(query, process){
			// Abort any existing search
			if (self.currentSearch) {
				self.currentSearch.abort();
				self.currentSearch = null;
			}
			self.currentSearch = $.ajax({
				url: '/ajax/routepicker/query',
				dataType: 'json',
				data: {
					query: query
				},
				success: function(data) {
					process(data)
				}
			});
		},
		item: '<li class="flexi-picker-result route"><a href="#"><div class="name"></div><div class=department></div></a></li>'
	});

	// Renders each result item with icon and description.
	$typeahead.render = function (items) {
		var that = this;

		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.attr('data-routecode', item.code);
				i.find('div.name').html(that.highlighter(item.code.toUpperCase() + ' ' + item.name));
				i.find('div.department').html(item.department);
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});

		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};

	// The Bootstrap Typeahead always appends the drop-down to directly after the input
	// Replace the show method so that the drop-down is added to the body
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};

	// Override select item to store the relevant attributes
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		this.$element.data('routecode', this.$menu.find('.active').data('routecode'));
		return oldSelect.call($typeahead);
	};

	$typeahead.updater = function() {
		return this.$menu.find('.active .name').text();
	};
};

// The jQuery plugin
$.fn.routePicker = function (options) {
	this.each(function () {
		var $this = $(this);
		if ($this.data('route-picker')) {
			throw new Error("FlexiPicker has already been added to this element.");
		}
		var allOptions = {
			input: this
		};
		$.extend(allOptions, options || {});
		$this.data('route-picker', new RoutePicker(allOptions));
	});
	return this;
};

/**
 * Any input with the route-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 */
jQuery(function($){
	$('.route-picker').routePicker({});
});


/**
 * Like the FlexiPicker, but for Locations
 */
var LocationPicker = function (options) {
	var self = this;
	var $element = $(options.input);

	// Might have manually wired this element up with an existing picker,
	// but add the class for CSS style purposes.
	if (!$element.hasClass('location-picker')) {
		$element.addClass('location-picker');
	}

	// Disable browser autocomplete dropdowns, it gets in the way.
	$element.attr('autocomplete', 'off');

	var $typeahead = new TabulaTypeahead({
		element: $element,
		source: function(query, process){
			// Abort any existing search
			if (self.currentSearch) {
				self.currentSearch.abort();
				self.currentSearch = null;
			}
			self.currentSearch = $.ajax({
				url: 'https://campus.warwick.ac.uk/Services/api.php?callback=?',
				dataType: 'json',
				data: {
					config: 'warwick',
					act: 'ac',
					mlim: 2,
					limit: 1000,
					type: 'lb',
					key: '$2a$11$ra7c/DofvF6yZuQhl.SBEuLrA8k2fvyt.WlJ8bbI.Asd1OyT.N2JS',
					_ts: new Date().getTime(),
					q: query
				},
				success: function(data) {
					process(data)
				}
			});
		},
		item: '<li class="flexi-picker-result location"><a href="#"><div class="name"></div><div class="department"></div></a></li>'
	});

	// Renders each result item with icon and description.
	$typeahead.render = function (items) {
		var that = this;

		items = $(items).map(function (i, item) {
			if (item != undefined) {
				i = $(that.options.item);
				i.attr('data-lid', item.lid);
				i.find('div.name').html(that.highlighter(item.name));
				i.find('div.department').html(item.sub || '&nbsp;');
				return i[0];
			} else {
				// no idea what's happened here. Return an empty item.
				return $(that.options.item)[0];
			}
		});

		items.first().addClass('active');
		this.$menu.html(items);
		return this;
	};

	// The Bootstrap Typeahead always appends the drop-down to directly after the input
	// Replace the show method so that the drop-down is added to the body
	$typeahead.show = function () {
		var pos = $.extend({}, this.$element.offset(), {
			height: this.$element[0].offsetHeight
		});

		this.$menu.appendTo($('body')).show().css({
			top: pos.top + pos.height, left: pos.left
		});

		this.shown = true;
		return this;
	};

	// Override select item to store the relevant attributes
	var oldSelect = $typeahead.select;
	$typeahead.select = function () {
		this.$element.data('lid', this.$menu.find('.active').data('lid'));
		return oldSelect.call($typeahead);
	};

	$typeahead.updater = function() {
		var roomName = this.$menu.find('.active .name').text();
		var buildingName = this.$menu.find('.active .department').text();

		return buildingName ? roomName + ' (' + buildingName + ')' : roomName;
	};
};

// The jQuery plugin
$.fn.locationPicker = function (options) {
	this.each(function () {
		var $this = $(this);
		if ($this.data('location-picker')) {
			throw new Error("FlexiPicker has already been added to this element.");
		}
		var allOptions = {
			input: this
		};
		$.extend(allOptions, options || {});
		$this.data('location-picker', new LocationPicker(allOptions));
	});
	return this;
};

/**
 * Any input with the location-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 */
jQuery(function($){
	$('.location-picker').locationPicker({});
});

// End of wrapping
})(jQuery);

