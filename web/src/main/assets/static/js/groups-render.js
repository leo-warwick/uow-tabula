/**
* Scripts used only by the SGT render/admin section.
*/
(function ($) { "use strict";

var exports = {};

// take anything we've attached to "exports" and add it to the global "Groups"
// we use extend() to add to any existing variable rather than clobber it
window.Groups = jQuery.extend(window.Groups, exports);

}(jQuery));

