// Lazily loaded user picker object.
if (!window.UserPicker) {
	window.UserPicker = {};
}
(function ($) { "use strict";

var trim = $.trim;
var _userPicker = null;
var exports = window.UserPicker;
function getUserPicker() {
    var targetWidth = 500, targetHeight=400;
    if (_userPicker == null) {

        // A popup box enhanced with a few methods for user picker
        _userPicker = new WPopupBox();

        _userPicker.isUniId = false;

        _userPicker.showPicker = function (element, targetInput) {
            this.setContent("Loading&hellip;");
            this.targetInput = targetInput;
            this.setSize(targetWidth,targetHeight);
            $.get('/ajax/userpicker/form', function (data) {
                _userPicker.setContent(data);
                _userPicker.setSize(targetWidth,targetHeight);
                _userPicker.show();
                _userPicker.positionRight(element);
                _userPicker.decorateForm();
            });
        };

        /* Give behaviour to user lookup form */
        _userPicker.decorateForm = function () {

            var $contents = $(this.contentElement),
                $firstname = $contents.find('.userpicker-firstname'),
                $lastname  = $contents.find('.userpicker-lastname'),
                $results = $contents.find('.userpicker-results'),
                $xhr,
                onResultsLoaded;

            $firstname.focus();
            $contents.find('input').delayedObserver(function () {
                // WSOS will search with at least 2 chars, but let's
                // enforce at least 3 to avoid overly broad searches.
                if (trim($firstname.val()).length > 2 ||
                        trim($lastname.val()).length > 2) {
                    $results.html('Loading&hellip;');
                    if ($xhr) $xhr.abort();
                    $xhr = jQuery.get('/ajax/userpicker/query',  {
                        firstName: $firstname.val(),
                        lastName: $lastname.val(),
                        isUniId: _userPicker.isUniId
                    }, onResultsLoaded);
                }
            }, 0.5);

            // wire up each user Id to be clickable
            onResultsLoaded = function(data) {
                $results.html(data);
                $results.find('td.user-id').click(function(){
                    var userId = this.innerHTML;
                    var input = $(_userPicker.targetInput);
                    input.val(userId);
                    input.trigger('change');
                    _userPicker.hide();
                });
            };

        };

    }
    return _userPicker;
}

function initUserPicker(picker, isUniId){
    var $picker = $(picker),
        $button = $('<a href="#" class="btn">Search for user</a>'),
        userPicker;
    $picker.after("&nbsp;");
    $picker.after($button);
    $button.click(function(event){
    	event.preventDefault();
        // lazy load on click
        if (userPicker === undefined){
            userPicker = getUserPicker();
        }
        userPicker.isUniId = isUniId;
        userPicker.showPicker(picker, picker);
        return false;
    });
}

// decorate all inputs with class user-code-picker.
$(function(){
    $('input.user-code-picker').each(function (i, picker) {
        initUserPicker(picker, false);
    });

    var emptyValue = function() {
    	return (this.value||"").trim() == "";
    };

    /*
     * Handle the multiple-user picker, by dynamically expanding to always
     * have at least one empty picker field.
     */
    // Each set of pickers will be in a .user-picker-collection
    var $collections = $('.user-picker-collection');
    $collections.each(function(i, collection){
    	var $collection = $(collection),
    	    $blankInput = $collection.find('.user-picker-container').first().clone()
    	    				.find('input').val('').end();
    	$blankInput.find('a.btn').remove(); // this button is added by initUserPicker, so remove it now or we'll see double

		// check whenever field is changed or focused
    	$collection.on('change focus', 'input', function(ev) {

    		// remove empty pickers
    		var $inputs = $collection.find('input');
    		if ($inputs.length > 1) {
    			var toRemove = $inputs.not(':focus').not(':last').filter(emptyValue).closest('.user-picker-container');
    			toRemove.remove();
    		}

    		// if last picker is nonempty OR focused, append an blank picker.
    		var $last = $inputs.last();
            var lastFocused = (ev.type == 'focusin' && ev.target == $last[0]);
    		if (lastFocused || $last.val().trim() != '') {
	    		var input = $blankInput.clone();
	    		$collection.append(input);
	    		initUserPicker(input.find('input')[0], false);
    		}
    	});
    });

});

exports.initUserPicker = initUserPicker;

}(jQuery));