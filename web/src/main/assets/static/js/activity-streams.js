jQuery(function($) {
	'use strict';

	// TODO this should be run early on in a more general file,
	// so that it applies to all use of moment
	moment.locale('en-gb');

	// Maps from priority classes to icon classes.
	var icons = {
		'priority-complete' : 'icon-ok fa fa-check',
		'priority-trivial' : 'icon-info-sign fa fa-info-circle',
		'priority-info' : 'icon-info-sign fa fa-info-circle',
		'priority-warning' : 'icon-warning-sign fa fa-exclamation-triangle',
		'priority-critical' : 'icon-warning-sign fa fa-exclamation-triangle' // same but red.
	};

	var exports = window;

	function Activity(item) {
		this.item = item;
	}

	Activity.prototype.render = function() {
		var item = this.item;
		var priority = 'priority-complete';
		if (item.priority >= 0.1) priority = 'priority-trivial';
		if (item.priority >= 0.25) priority = 'priority-info';
		if (item.priority >= 0.5) priority = 'priority-warning';
		if (item.priority >= 0.75) priority = 'priority-critical';

		var now = moment();
		var time = moment(item.published);
		var fullDate = time.format('LLLL');
		var $timestamp = $('<div>', {'class':'timestamp', title: fullDate}).html(toTimestamp(now, time));
		var urlTitle = capitalise(item.urlTitle || 'further info');
		var url = (item.priority >= 0.1) ? $('<p>', {'class': 'url'}).append($('<a></a>', {'href': item.url}).html(urlTitle)) : "";

		return $('<div>', {'class': 'activity ' + priority})
			.data('notification-id', item._id)
			.append($('<button>', {'class':'close', title: 'Dismiss'}).html('&times;'))
			.append($('<div>', {'class': 'headline'})
				.append($('<i></i>', {'class': icons[priority]}))
				.append($('<h5>', {'class': 'title'})
					.append($('<a>', {'class': 'url', href: item.url}).html(item.title))
				)
				.append($timestamp)
			)
			.append($('<div>', {'class': 'content'})
				.append(item.content)
				.append(url)
			);
	};

	function capitalise(text) {
		if (!text || text.length < 1) return text;
		return text.charAt(0).toUpperCase() + text.slice(1);
	}

	function toTimestamp(now, then) {
		var yesterday = now.clone().subtract(1, 'day');
		if (now.diff(then) < 60000) { // less than a minute ago
			return then.from(now);
		} else if (now.isSame(then, 'day')) {
			return then.format('LT [Today]');
		} else if (yesterday.isSame(then, 'day')) {
			return then.format('LT [Yesterday]');
		} else if (now.isSame(then, 'year')) {
			return then.format('ddd Do MMM LT');
		} else {
			return then.format('ddd Do MMM YYYY LT');
		}
	}

	// Set up a rendered activity stream in the given container.
	// The contents will be cleared.
	function initStream($container, options) {
		var options = options || {};

		if ($container.data('activity-stream-init')) {
			return;
		}
		$container.data('activity-stream-init', true);

		var $moreLink = $('<div>', {'class':'more-link'}).append(
				$('<a>', {href:'#'}).html('More&hellip;')
		);

		$container.on('click', 'button.close', function() {
			// dismiss
			var $activity = $(this).closest('.activity');
			var notificationId = $activity.data('notification-id');

			if ($activity.data('ajaxRequest')) {
				// Double-click protection
				return;
			}

//			console.log('remove', notificationId);
			$activity.data('ajaxRequest', true);
			$.post('/activity/dismiss/' + notificationId)
				.then(function(data, textStatus, xhr) {
					$activity.data('ajaxRequest', false);

					var $undoNotice = $('<div>', {'class':'deleted-notice alert'}).html('Dismissed. ');
					$undoNotice.append($('<a>', {href:'#'}).html('Undo').on('click', function() {
						$activity.show();
						$undoNotice.remove();
						$.post('/activity/restore/' + notificationId);
						return false;
					}));
					$activity.before($undoNotice);
					$activity.hide(); // though it's deleted, just hide it for now to make restoring easier.
				}, function(xhr, textStatus, errorThrown) {
					$activity.data('ajaxRequest', false);
//					console.error(errorThrown);
				});
		});

		// apply JS behaviour to .activity elements.
		function wire($activities) {
			$activities.filter(':not(.priority-complete)').find('.content').collapsible();
		}

		function loadPage(lastCreated, first) {
			var data = jQuery.extend({}, options);
			var url = '/activity/@me';
			if (lastCreated) {
				data.lastCreated = lastCreated;
			}
			jQuery.get(url, data).then(function(data) {
				if (first) {
					$container.html('');
				}

				$(data.items).each(function(i, item) {
					var activity = new Activity(item);
					var $activity = activity.render();
					wire($activity);
					$activity.appendTo($container);
				});

				$container.append($moreLink);
				if (data.lastCreated) {
					if (data.items.length) {
						$moreLink.off('click');
						$moreLink.on('click', 'a', function() {
							loadPage(data.lastCreated);
							return false;
						})
					} else {
						// this page was empty but there's another page, so
						// get that straight away. (can happen if all notifications related
						// to obsolete objects so aren't returned)
						loadPage(data.lastCreated, first);
					}
				} else {
					var noMoreMsg = first ? '<span class="hint">No recent activity to display.</span>' : '<span class="hint">No more activity to display.</span>';
					$moreLink.after(noMoreMsg);
					$moreLink.remove(); // il n'y a pas de items
				}
			}, function (xhr, data, error) {

			});
		}

		loadPage(null, true);
	}

	function autoInit() {
		$('.activity-stream').each(function(i, container) {
			var $container = $(container);
			var options = {};
			options.max = $container.data('max');
			options.minPriority = $container.data('minpriority');
			var types = $container.data('types');
			if (types) {
				options.types = types;
			}

			initStream($container, options);
		});
	}

	exports.ActivityStreams = {
		//Activity: Activity,
		initStream: initStream,
		autoInit: autoInit
	};

	$(function(){
		autoInit();
	});

});