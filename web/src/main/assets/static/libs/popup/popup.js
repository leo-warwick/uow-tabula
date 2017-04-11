// // // // // // // // // // // // // // // // // // //
// // // // // // //  js glass box  // // // // // // //
// // // // // // // // // // // // // // // // // // //

// don't redefine
if (typeof WPopupBox === 'undefined') {


var jsLoadImage = function(url) {
  var im = new Image();
  im.src = url;
  return im;
};

var ie_lt7 = /MSIE ((5\.5)|6)/.test(navigator.userAgent) && navigator.platform === "Win32";


var WPopupBox = function(config, options) {
  WPopupBox.jq = this.jq = (typeof jQuery != 'undefined');

  if (!config || (!config.images && config.imageroot)) {
	var imageroot = this.imageroot = (config||WPopupBox.defaultConfig).imageroot || '/static_war/popup/';
	config = {
	  images : {
	    tl : jsLoadImage(imageroot+'tl.png'),
	    tr : jsLoadImage(imageroot+'tr.png'),
	    bl : jsLoadImage(imageroot+'bl.png'),
	    br : jsLoadImage(imageroot+'br.png'),
	    t : jsLoadImage(imageroot+'t.png'),
	    r : jsLoadImage(imageroot+'r.png'),
	    b : jsLoadImage(imageroot+'b.png'),
	    l : jsLoadImage(imageroot+'l.png'),
	    c : jsLoadImage(imageroot+'c.png'),
	    topArr : jsLoadImage(imageroot+'toparr.png'),
	    bottArr : jsLoadImage(imageroot+'bottarr.png'),
	    leftArr : jsLoadImage(imageroot+'leftarr.png'),
	    rightArr : jsLoadImage(imageroot+'rightarr.png'),
	    transparent : jsLoadImage(imageroot+'shim.gif')
	  },
	   margin : [ 10,13,20,16 ], //top right bottom left
	   padding : [20,24,0,24],
	   topArr : [38,18],
	   bottArr : [37,20],
	   leftArr : [19,39],
	   rightArr : [20,39]
	};
  }

  if (this.jq) {
	  this.config = [];
	  jQuery.extend(this.config, WPopupBox.defaultConfig);
	  jQuery.extend(this.config, config);
  } else {
	  this.config = $H(WPopupBox.defaultConfig).merge(config).toObject();
  }
  options = options || {};

  var self = this;


  this.initialised = false;

  this.showing = false;

  this.glassElement = document.createElement('div');
  this.glassElement.className = 'WPopupGlass';

  this.width = null;
  this.height = null;
  this.x = null;
  this.y = null;

  this.topArrowOn = false;
  this.bottomArrowOn = false;
  this.leftArrowOn = false;
  this.rightArrowOn = false;

  this.imageElements = {};
  this.imageElements.c = document.createElement('img');
  this.imageElements.c.className = 'c';
  this._setImage(this.imageElements.c, config.images.c.src);

  var margin = config.margin;
  var dtop = margin[0];
  var dright = margin[1];
  var dbottom = margin[2];
  var dleft = margin[3];

  this.extraHeight = dtop+dbottom+config.padding[0]+config.padding[2];

  var i = this.imageElements;
  var r = this.glassElement;

  i.tl = this._makeDiv('tl alpha', config.images.tl, dleft, dtop);
  i.tr = this._makeDiv('tr alpha', config.images.tr, dright, dtop);
  i.bl = this._makeDiv('bl alpha', config.images.bl, dleft, dbottom);
  i.br = this._makeDiv('br alpha', config.images.br, dright, dbottom);
  i.t = this._makeEdge('t alpha', config.images.t, dleft, dtop);
  i.r = this._makeEdge('r alpha', config.images.r, dright, dtop);
  i.b = this._makeEdge('b alpha', config.images.b, dright, dbottom);
  i.l = this._makeEdge('l alpha', config.images.l, dleft, dbottom);
  i.ta = this._makeDiv('tArr alpha', config.images.topArr, config.topArr[0], config.topArr[1]);
  i.ba = this._makeDiv('bArr alpha', config.images.bottArr, config.bottArr[0], config.bottArr[1]);
  i.la = this._makeDiv('lArr alpha', config.images.leftArr, config.leftArr[0], config.leftArr[1]);
  i.ra = this._makeDiv('rArr alpha', config.images.rightArr, config.rightArr[0], config.rightArr[1]);

  var each = (this.jq) ? jQuery.each : function(arr, callback) {
	 $A(arr).each(function(v,i){
		 callback(i,v);
	 })
  };

  each([i.ta, i.ba, i.la, i.ra], function(index, im){
	  im.style.display = 'none';
	  im.style.position = 'relative';
  });

  each([i.tl, i.tr, i.bl, i.br, i.t, i.r, i.b, i.l, i.c, i.ta, i.ba, i.la, i.ra], function(index, im){
	  r.appendChild(im);
  });

  i.t.style.left = this.config.margin[3] + 'px';
  i.b.style.left = this.config.margin[3] + 'px';
  i.c.style.left = this.config.margin[3] + 'px';
  i.l.style.top = this.config.margin[0] + 'px';
  i.r.style.top = this.config.margin[0] + 'px';
  i.c.style.top = this.config.margin[0] + 'px';

  this.contentElement = document.createElement('div');
  this.contentElement.className = 'WPopupBoxContent';

  // Optionally provide a div containing initial content.
  // It will be used as the root element.
  var existingElement = options.element;

  if (existingElement) {
	  this.rootElement = existingElement;
	  this.setContentFromElement(existingElement);
  } else {
	  this.rootElement = document.createElement('div');
  }
  this.rootElement.className = 'WPopupBox';

  this.hide();

  this.rootElement.appendChild(this.glassElement);
  this.rootElement.appendChild(this.contentElement);

  if (existingElement) {
	  // we started off with some content so make sure it fits
	  this.setSize(450,350);
	  this.setHeightToFit();
  } else {
	  // Only append to the body if we just created it - otherwise
	  // leave it in place, because the page may depend on its
	  // placement (eg the comment popup on edit content needs to
	  // stay inside the form tag).
	  document.body.appendChild(this.rootElement);
	  this.setSize(450,350);
  }

  if (options.width) {
	  if (options.height) {
		 this.setSize(options.width, options.height);
	  } else {
		 this.setSize(options.width, 350);
		 this.setHeightToFit();
	  }
  }

  if (options.x && options.y) {
	  this.setPosition(options.x, options.y);
  }

  // monitor presses of Esc and close the popup if it's open
  if (this.jq) {
	  jQuery(document).keydown(function(event){
		  if (self.isShowing() && event.keyCode == 27) { //ESCAPE
			  event.preventDefault();
			  self.hide();
		  }
	  });
  } else {
	  Event.observe(document, 'keydown', function(e) {
		  var ev = (window.event) ? window.event : e;
		  if (ev.keyCode == Event.KEY_ESC) {
			  cancelDefaultEvents(ev);
			  this.hide();
		  }
	  }.bind(this));
  }

  this.initialised = true;
};

WPopupBox.defaultConfig = {};

/*
 * Set the top-left position of the popup.
 */
WPopupBox.prototype.setPosition = function(x,y) {
  this.x = x; this.y = y;

  if (this.topArrowOn) {
     y += this.imageElements.ta.clientHeight;
  }

  if (this.leftArrowOn) {
	 x += this.imageElements.la.clientWidth;
  }

  	/**
	 * Get the current viewport size.
	 */
	function getViewportSize()
	{
		 var size = [0, 0];

		 if (typeof window.innerWidth != 'undefined')
		 {
		   size = [
		       window.innerWidth,
		       window.innerHeight
		   ];
		 }
		 else if (typeof document.documentElement != 'undefined'
		     && typeof document.documentElement.clientWidth !=
		     'undefined' && document.documentElement.clientWidth != 0)
		 {
		   size = [
		       document.documentElement.clientWidth,
		       document.documentElement.clientHeight
		   ];
		 }
		 else
		 {
		   size = [
		       document.getElementsByTagName('body')[0].clientWidth,
		       document.getElementsByTagName('body')[0].clientHeight
		   ];
		 }

		 return size;
	}

	/**
	 * Get the current scroll position of the page.
	 */
	function getScrollingPosition()
	{
		 var position = [0, 0];
		 if (typeof window.pageYOffset != 'undefined')
		 {
		   position = [ window.pageXOffset,
		                window.pageYOffset ];
		 }
		 else if (typeof document.documentElement.scrollTop
		     != 'undefined' && document.documentElement.scrollTop > 0)
		 {
		   position = [ document.documentElement.scrollLeft,
		                document.documentElement.scrollTop ];
		 }
		 else if (typeof document.body.scrollTop != 'undefined')
		 {
		   position = [ document.body.scrollLeft,
		                document.body.scrollTop ];
		 }
		 return position;
	}

  var viewportSize = getViewportSize();
  var scrollPosition = getScrollingPosition();

  // In firefox this doesn't include scrollbars, so knock 20px off just to be safe
  var windowHeight = viewportSize[1] + scrollPosition[1] - 20;
  var windowWidth = viewportSize[0] + scrollPosition[0] - 20;

  var right = x + this.width;
  var bottom = y + this.height;

  // scrolled off the right
  if (right > windowWidth) {
	  x = windowWidth - this.width;
  }
  // scrolled off the left
  if (x < 0) {
  	  x = 0;
  }
  // scrolled off the bottom
  if ((bottom - windowHeight) > this.height) {
  	  y -= this.height;
  }
  // scrolled off the top
  if (y < 0) {
  	  y = 0;
  }

  this.rootElement.style.left = x+'px';
  this.rootElement.style.top = y+'px';
  // move content element into place
  this.contentElement.style.left = (this.config.padding[3]) + 'px';
  this.contentElement.style.top = (this.config.padding[0]) + 'px';

  var arrow = this.imageElements.ta;
  arrow.style.left = (this.width / 2 - arrow.clientWidth/2) + 'px';
  arrow.style.bottom = '10px';

  var s = this.imageElements.la.style;
  s.top = (this.height / 2 - this.imageElements.la.clientHeight/2) + 'px';
  s.marginLeft = '-5px';
  s.marginTop = '-' + Math.floor(this.config.leftArr[1]/2) + 'px';

  s = this.imageElements.ra.style;
//  s.top = (this.height / 2 - this.imageElements.ra.clientHeight/2) + 'px';
  s.left = (this.width - this.config.margin[1]) + 'px' ;
  s.marginTop = '-' + Math.floor(this.config.rightArr[1]/2) + 'px';

  s = this.imageElements.ba.style;
  s.left = ((this.width - this.imageElements.ba.clientWidth) / 2) + 'px';
  s.top = this.height + 'px';
  s.marginTop = '-18px'; // probably some way of calculating this...

};

WPopupBox.prototype.setSize = function(w,h) {
  this.width = w; this.height = h;
  this.glassElement.style.width = w+'px';
  this.glassElement.style.height = h+'px';
//  this.rootElement.style.width = w+'px';
//  this.rootElement.style.height = h+'px';
  this.contentElement.style.width = (w-this.config.padding[3]-this.config.padding[1]) + 'px';
  this._repositionEdges();
};

/*
 * Adjusts the height of the popup to fit the content. The
 * width will stay the same.
 */
WPopupBox.prototype.setHeightToFit = function() {
  if (this.everShown) {
	  var contentHeight = this.contentElement.clientHeight;
	  this.setSize(this.width, contentHeight + this.extraHeight);
  } else {
	  this.delayedSetHeight = true;
  }
}

WPopupBox.prototype.increaseHeightToFit = function() {
	  if (this.everShown) {
		  var height = this.contentElement.clientHeight + this.extraHeight;
		  if (height > this.height) {
			  this.setSize(this.width, height);
		  }
	  } else {
		  this.delayedIncreaseHeight = true;
	  }
	}


WPopupBox.prototype.hide = function() {
  this.rootElement.className = 'WPopupBox';
  this.showing = false;
  WPopupBox.showSelectBoxes();
};

WPopupBox.prototype.show = function() {
  this.rootElement.className = 'WPopupBox visible';
  this.showing = true;
  // if we've set size before the first view, do it again because
  // it was likely not done properly (dimensions not calculated when
  // element is invisible
  if (!this.everShown) {
	  this.everShown = true;
	  this.setSize(this.width, this.height);
	  if (this.delayedSetHeight) {
		 this.setHeightToFit();
	  }
	  if (this.delayedIncreaseHeight) {
		 this.increaseHeightToFit();
	  }
  }
  WPopupBox.hideSelectBoxes();
};

WPopupBox.prototype.toggle = function() {
  if (this.isShowing())
  { this.hide(); } else { this.show(); }
}

WPopupBox.prototype.isShowing = function() {
	return this.showing;
}

/*
 * Takes the given string as HTML and sets it as the content.
 * It will keep the close button, and will resize the height
 * to fit the new content.
 */
WPopupBox.prototype.setContent = function(content) {
	this.contentElement.innerHTML = content;
	this._afterContentSet();
}

/**
 * Fetch the given URL as content and show the popup.
 * Equilent to old showPopup() function.
 *
 * Valid options: {
 * 	 target: an element to point the popup at
 *   position: if set to 'right', it will use a left arrow to point. otherwise it will be below.
 *   params: parameters to pass to the AJAX request.
 * }
 */

WPopupBox.prototype.showUrl = function(url, options) {
	options = options || {};
	var method = options.method || 'POST';
	var popup = this;
	var callback = function(data) {
		document.body.style.cursor = "auto";
		popup.show();
		popup.setContent(data);
		if (typeof setupSubmitDisabling !== 'undefined') { // for Sitebuilder
		  setupSubmitDisabling();
		}
		if (options.target) {
			var t = options.target;
			if (options.position === 'right') { popup.positionRight(t); }
			else if (options.position === 'above') { popup.positionAbove(t); }
			else { popup.positionBelow(t); }
		}

		if (options.onComplete) {
			options.onComplete(popup.contentElement);
		}
	};
	var prototypeCallback = function(r) { callback(r.responseText); }

	if (this.jq) {
		if (options.params) {
			jQuery.ajax(url, {type:method, success:callback, data: options.params });
		} else {
			jQuery.ajax(url, {type:method, success:callback });
		}
	} else {
		if (options.params) {
			new Ajax.Request(url,{method:method,onComplete:prototypeCallback, parameters:options.params});
		} else {
			new Ajax.Request(url,{method:method,onComplete:prototypeCallback});
		}
	}
	document.body.style.cursor = "progress";
}

WPopupBox.prototype.setContentElement = function(element) {
	this.contentElement.innerHTML = '';
	this.contentElement.appendChild(element);
	this._afterContentSet();
};

WPopupBox.prototype.setContentFromElement = function(element) {
	this.contentElement.innerHTML = '';
	while (element.firstChild) this.contentElement.appendChild(element.firstChild);
	this._afterContentSet();
};

WPopupBox.prototype._afterContentSet = function() {
	var popup = this;
	this.closeButton = document.createElement('a');
	this.closeButton.setAttribute("style","cursor:pointer;cursor:hand;");
	if (this.jq) {
		jQuery(this.closeButton).click(function(ev){
			popup.hide();
			ev.preventDefault();
		});
	} else {
		this.closeButton.onclick = (function() {
			this.hide();
			return false;
		}).bind(this);
	}

	closeImg = document.createElement('img');
	closeImg.src = this.imageroot+'close.png';
	closeImg.title = 'Close this popup';
	closeImg.border = 0;

	//fudge since browsers don't agree on how to set float
	this.closeButton.className = 'WPopupCloseButton';
	this.closeButton.appendChild(closeImg);

	this.contentElement.insertBefore(this.closeButton, this.contentElement.firstChild);

	if (this.initialised) {
		this.setHeightToFit();
	}
};

WPopupBox.prototype._repositionEdges = function() {
  var sideWidth = (this.width - this.config.margin[3] - this.config.margin[1]) + 'px';
  var sideHeight = (this.height - this.config.margin[0] - this.config.margin[2]) + 'px';

  { var ims = this.imageElements;
   ims.t.style.width = sideWidth;
   ims.b.style.width = sideWidth;
   ims.l.style.height = sideHeight;
   ims.r.style.height = sideHeight;
   ims.c.style.width = sideWidth;
   ims.c.style.height = sideHeight;
  }
};

// function to set a PNG as the src of an image, or if ie<7 it will use the special image loader hack.
WPopupBox.prototype._setImage = function(img, url) {
  var ie_lt7 = ie_lt7;
  if (ie_lt7) {
    img.src = this.config.images.transparent.src;
    img.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url + "',sizingMethod='scale')";
  } else {
    img.src = url;
  }
}
WPopupBox.prototype._setDivBg = function(div, url) {
  var ie_lt7 = ie_lt7;
  if (ie_lt7) {
    div.runtimeStyle.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url + "',sizingMethod='image')";
  } else {
    this._setDivBgNoHack(div,url);
  }
}
WPopupBox.prototype._setDivBgNoHack = function(div, url) {
  if (!ie_lt7) {
    div.style.backgroundImage = 'url('+url+')';
    div.style.backgroundPosition = 'top left';
    div.style.backgroundRepeat = 'no-repeat';
  }
}

WPopupBox.prototype._makeDiv = function(clazz, image, width, height) {
  var el = document.createElement('div');
  el.className = clazz;
  this._setDivBg(el, image.src);
  el.style.width = width+'px';
  el.style.height = height+'px';
  return el;
}

WPopupBox.prototype._makeEdge = function(clazz, image, width, height) {
  var el = document.createElement('img');
  this._setImage(el, image.src);
  el.className = clazz;
  el.style.width = width+'px';
  el.style.height = height+'px';
  return el;
};

WPopupBox.prototype.setTopArrow = function() {
  this.imageElements.ta.style.display = 'block';
  this.topArrowOn = true;
};

WPopupBox.prototype.setBottomArrow = function() {
  this.imageElements.ba.style.display = 'block';
  this.bottomArrowOn = true;
};

WPopupBox.prototype.setLeftArrow = function() {
  this.imageElements.la.style.display = 'block';
  this.leftArrowOn = true;
};

WPopupBox.prototype.setRightArrow = function() {
  this.imageElements.ra.style.display = 'block';
  this.rightArrowOn = true;
};

/*
 * Remove all arrows so that it's just a box,
 * which is how it is by default.
 */
WPopupBox.prototype.removeArrows = function() {
  this.topArrowOn = false;
  this.bottomArrowOn = false;
  this.leftArrowOn = false;
  this.rightArrowOn = false;
  this.imageElements.ra.style.display = 'none';
  this.imageElements.la.style.display = 'none';
  this.imageElements.ba.style.display = 'none';
  this.imageElements.ta.style.display = 'none';
};

/*
 * Places the popup below the given element, and adds
 * a top arrow pointing at the element.
 */
WPopupBox.prototype.positionBelow = function(el) {
  this.removeArrows();
  this.setTopArrow();

  var midpoint = this.getMidpoint(el);

  this.setPosition(midpoint.x - ((this.width)/2),midpoint.y);
};


/*
 * Finds the centre of the given element, so we know where to
 * point at. Returned as an {x:.., y:..} hash.
 *
 * Also returns the absolute bounds of the element as top,left,bottom,right.
 */
WPopupBox.prototype.getMidpoint = function(el) {
	if (el && el.x && el.y) {
      // el itself is the midpoint coordinates, return it
      return el;
	}

	var parentPosition, dimensions;

	if (this.jq) {
		$el = jQuery(el);

		parentPosition = $el.offset();
		var width = $el.outerWidth();
		var height = $el.outerHeight();

		if ($el.css('display') === 'inline') {
			var originalPosition = $el.css('position');
			$el.css('position','absolute');
			var absoluteWidth = $el.outerWidth();
			var absoluteHeight = $el.outerHeight();
			$el.css('position', originalPosition);

			if (width != absoluteWidth) {
				width = (width+absoluteWidth) * 0.5;
				height = (height+absoluteHeight) * 0.5;
			}
		}


		return {
			x: Math.floor(parentPosition.left + width/2),
			y: Math.floor(parentPosition.top + height/2),
			left: Math.floor(parentPosition.left),
			right: Math.floor(parentPosition.left + width),
			top: Math.floor(parentPosition.top),
			bottom: Math.floor(parentPosition.top + height)
		};

	} else {
		el = $(el);
		parentPosition = Position.cumulativeOffset(el);
		dimensions = Element.getDimensions(el);

		var width = dimensions.width;
		var height = dimensions.height;

		if (el.getStyle('display') === 'inline') {
			// now get the absolute dimensions - for wrapped inline elements
			var els = el.style;
			var originalPosition = els.position;
			els.position = 'absolute';
			var absDimensions = Element.getDimensions(el);
			els.position = originalPosition;

			// Note: Even for regular inline elements that don't wrap, the dimensions
			// are likely to change when you absolutely position it.
			if (dimensions.width != absDimensions.width) {
				// set the actual dimensions to be midway between the two values
				height = (absDimensions.height + dimensions.height)/2;
				width = (absDimensions.width + dimensions.width)/2;
			}
		}

		return {
			x: Math.floor(parentPosition[0] + width/2),
			y: Math.floor(parentPosition[1] + height),
			left: Math.floor(parentPosition[0]),
			right: Math.floor(parentPosition[0] + width),
			top: Math.floor(parentPosition[1]),
			bottom: Math.floor(parentPosition[1] + height)
		};
	}


};


WPopupBox.prototype.positionAbove = function(el) {
	this.removeArrows();
	this.setBottomArrow();
	if (!this.jq) el = $(el);

	var midpoint = this.getMidpoint(el);

	var x = midpoint.x - this.width/2;
	var y = Math.max(0, midpoint.y - this.height - this.config.bottArr[1]);
	this.setPosition(x, y);
};

WPopupBox.prototype.positionRight = function(el, hideArrows) {
	if (hideArrows) {
		this.removeArrows();
	} else {
		this.setLeftArrow();
	}

	var midpoint = this.getMidpoint(el);

	h = this.height;

	var scrollTop;
	if (this.jq) scrollTop = jQuery(document.body).scrollTop();
	else scrollTop = $(document.body).cumulativeScrollOffset()[1];

	x = Math.floor(midpoint.right);
	y = Math.max(scrollTop, Math.floor(midpoint.y - h/2));
	this.setPosition(x,y);

	if (this.jq) {
		var thisPosition = jQuery(this.rootElement).offset();
		jQuery(this.imageElements.la).css('top', midpoint.y - thisPosition.top);
	} else {
		var thisPosition = Position.cumulativeOffset(this.rootElement);
		this.imageElements.la.style.top = parseInt(midpoint.y - thisPosition[1]) + 'px';
	}
};

WPopupBox.prototype.positionLeft = function(el, hideArrows) {
	if (hideArrows) {
		this.removeArrows();
	} else {
		this.setRightArrow();
	}

	var midpoint = this.getMidpoint(el);

	h = this.height;

	var scrollTop;
	if (this.jq) scrollTop = jQuery(document.body).scrollTop();
	else scrollTop = $(document.body).cumulativeScrollOffset()[1];

	x = Math.floor(midpoint.left) - this.width;
	y = Math.max(scrollTop, Math.floor(midpoint.y - h/2));
	this.setPosition(x,y);

	if (this.jq) {
		var thisPosition = jQuery(this.rootElement).offset();
		jQuery(this.imageElements.ra).css('top', midpoint.y - thisPosition.top);
	} else {
		var thisPosition = Position.cumulativeOffset(this.rootElement);
		this.imageElements.ra.style.top = parseInt(midpoint.y - thisPosition[1]) + 'px';
	}
};

WPopupBox.hideSelectBoxes = function() {
	if (!ie_lt7)
		return;
	if (WPopupBox.jq)
		jQuery('select').css('visibility','hidden');
	else
		$A(document.getElementsByTagName('select')).each(
			function(select) {
				select.style.visibility = 'hidden';
			}
		);
};

WPopupBox.showSelectBoxes = function() {
	if (!ie_lt7)
		return;
	if (WPopupBox.jq)
		jQuery('select').css('visibility','visible');
	else
		$A(document.getElementsByTagName('select')).each(
			function(select) {
				select.style.visibility = 'visible';
			}
		);
};

};