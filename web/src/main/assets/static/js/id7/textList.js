TextListController = function (target, formField){
	this.newItemHead = '<li class="item"><span>';
	this.newItemTail = '</span><a class="close" href="#"></a></li>';

	this.formField = jQuery(formField);

	this.container = jQuery(target);
	this.inputContainer = jQuery(".inputContainer", this.container);

	this.seperator = ',';
	this.minInputWidth = 75; // px
	this.preventDuplicates = false;
	this.transformInput = undefined;
};

TextListController.prototype = {

	init: function (){
		this.bindInputBoxEvents();
		this.bindRemoveItemEvent();
		this.initaliseListItems();
	},

	bindInputBoxEvents: function (){
		var self = this;

		jQuery("input", this.inputContainer).on('keyup', function(event){
			if(event.keyCode == "32" || event.keyCode == "13"){ //' ' or Enter
		        var newItem = jQuery(this).val();
		        newItem = self.sanitiseInput(newItem);

		        // If it was a space, remove the last item
		        if(event.keyCode == "32")
		        	newItem = newItem.substring(0, newItem.length-1);


				if(self.preventDuplicates && self.isDuplicate(newItem)){
					jQuery(this).val(newItem);
				} else {
					self.addItem(newItem);
					jQuery(this).val('');
				}
		    } else if (event.keyCode == "8"){ //backspace
				//Now instead of calling start you can get pos directly by invoking caret() (caret-1.5.2 api)
				var caret = jQuery(this).caret();
		    	if (caret == 0) {
		    		// Remove the last item in the list
		    		var items = jQuery(".item", self.container);
		    		if (items.length) {
		    			items.last().remove();
						self.syncFormField();
						self.resizeInputContainer();

			    		// Prevent annoying "bip" from being at start of list
			    		event.stopPropagation();
			    		event.preventDefault();
			    		return false;
		    		}
		    	}
		    }
		}).on("keyup keydown keypress", function(event){
			if (event.keyCode == "13") {
				// Prevent default behaviour for Enter
				event.stopPropagation();
	    		event.preventDefault();
	    		return false;
			}
		});

		// if there is any text remaining in the input box when it loses focus convert it into a new item
		jQuery("input", this.inputContainer).on('focusout', function(event){
			if(jQuery(this).val() !== ""){
		        var newItem = jQuery(this).val();
		        newItem = self.sanitiseInput(newItem);
				if(self.preventDuplicates && self.isDuplicate(newItem)){
					jQuery(this).val(newItem);
				} else {
					self.addItem(newItem);
					jQuery(this).val('');
				}
		    }
		});
	},

	bindRemoveItemEvent: function(){
		var self = this;
		this.container.on("click", ".item a.close", function(event){
			var parent = jQuery(this).closest(".item");
			parent.fadeOut(function(){
				parent.remove();
				self.syncFormField();
				self.resizeInputContainer();
			});
			event.preventDefault();
		});
	},

	initaliseListItems: function(){
		var items = this.formField.val().split(this.seperator);
		for(var i=0; i<items.length; i++){
			if(items[i] !== ""){
				this.inputContainer.before(this.newItemHead + items[i] + this.newItemTail);
			}
		}
		this.resizeInputContainer();
	},

	syncFormField: function(){
		this.formField.val(''); //clear the form field
		var items = jQuery(".item", this.container);
		var newValue = "";
		items.each(function(index){
			newValue += jQuery(this).find("span").html();
			if(index < items.length-1) newValue += ","
		});
		this.formField.val(newValue);
	},

	addItem: function(text){
		//check if the individual value is separated by spaces/tabs etc - user might copy/paste direct value
		var items = text.split(/\s+/);
		var fieldObject = this;
		jQuery.each(items, function(index, item) {
			var isPresent = fieldObject.isDuplicate(item);
			if(!isPresent) {
				fieldObject.inputContainer.before(fieldObject.newItemHead + item + fieldObject.newItemTail);
			}

		});
		this.syncFormField();
		this.resizeInputContainer();
	},

	sanitiseInput: function(text){
		// strip any instances of the separator from the new item
		var result = text.replace(new RegExp(this.seperator, 'g') , '');
		if(this.transformInput){
			// if a custom text transform for the input exists then apply it
			result = this.transformInput(result);
		}
		return result;
	},

	isDuplicate: function(text){
		var result = false;
		var items = jQuery(".item", this.container)
		items.each(function(index){
			var item = jQuery(this)
			if(item.find("span").html() == text){
				item.animate({backgroundColor:'#00AACC'}, 500, function(){
					item.animate({backgroundColor:'#0074CC'}, 500);
				});
				result=true;
			}
		});
		return result;
	},

	resizeInputContainer: function(){
		var width=0;
		var totalWidth = Math.round(jQuery("ul", this.container).outerWidth(true));
		jQuery(".item", this.container).each(function(){
			var itemWidth = jQuery(this).outerWidth(true);
			width += jQuery(this).outerWidth(true);
			if (width > totalWidth){ // row has wapped restart counting size from the next row
				width=itemWidth;
			}
		});
		width=Math.round(width);
		if(totalWidth-width > this.minInputWidth){
			this.inputContainer.css("width", (totalWidth-width-3)+"px");
		} else {
			this.inputContainer.css("width", "100%");
		}
	}
};