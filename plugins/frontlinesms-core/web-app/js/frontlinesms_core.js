// Array.indexOf is not available in IE before IE9, so
// add it here using the jQuery implementation
if(!Array.prototype.indexOf) {
	Array.prototype.indexOf = function(index) {
		return jQuery.inArray(index, this);
	};
}

// String.trim is not available in IE before IE9, so
// add it here using the jQuery implementation
if(!String.prototype.trim) {
	String.prototype.trim = function() {
		return jQuery.trim(this);
	};
}

if(!String.prototype.startsWith) {
	String.prototype.startsWith = function(prefix) {
		return this.slice(0, prefix.length) === prefix;
	};
}

if(!String.prototype.endsWith) {
	String.prototype.endsWith = function(suffix) {
		return this.indexOf(suffix, this.length - suffix.length) !== -1;
	};
}

if(!String.prototype.htmlEncode) {
	String.prototype.htmlEncode = function() {
		return this
			.replace(/&/g, '&amp;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;');
	};
}

// Standardise the onclick/onchange firing in IE before IE9
function addChangeHandlersForRadiosAndCheckboxes() {
	$('input:radio, input:checkbox').click(function() {
		this.blur();
		this.focus();
	});
}
if(jQuery.browser.msie) { $(function() {
	addChangeHandlersForRadiosAndCheckboxes();
});}

(function($) {
	$.fn.disableField = function(){
	    return this.each(function(){
	        this.disabled = true;
	    });
	};
	$.fn.enableField = function(){
	    return this.each(function(){
	        this.disabled = false;
	    });
	};
}(jQuery));

function getSelectedGroupElements(groupName) {
	return $('input[name=' + groupName + ']:checked');
}

function isGroupChecked(groupName) {
	return getSelectedGroupElements(groupName).length > 0;
}

$('.check-bound-text-area').live('focus', function() {
	var checkBoxId = $(this).attr('checkbox_id');
	$('#' + checkBoxId).attr('checked', true);
});

$.fn.renderDefaultText = function() {
	return this.focus(function() {
			$(this).toggleClass('default-text-input', false);
			var element = $(this).val();
			$(this).val(element === this.defaultValue ? '' : element);
		}).blur(function() {
			var element = $(this).val();
			$(this).val(element.match(/^\s+$|^$/) ? this.defaultValue : element);
			$(this).toggleClass('default-text-input', $(this).val() === this.defaultValue); });
};

function showThinking() {
	$('#thinking').fadeIn();
}

function hideThinking() {
	$('#thinking').fadeOut();
}

function insertAtCaret(areaId, text) {
	var front, back, range,
	txtarea = document.getElementById(areaId),
	scrollPos = txtarea.scrollTop,
	strPos = 0,
	browser = ((txtarea.selectionStart || txtarea.selectionStart === '0') ?
			"ff" : (document.selection ? "ie" : false ) );
	if (browser === "ie") {
		txtarea.focus();
		range = document.selection.createRange();
		range.moveStart ('character', -txtarea.value.length);
		strPos = range.text.length;
	} else if (browser === "ff") {
		strPos = txtarea.selectionStart;
	}

	front = (txtarea.value).substring(0, strPos);
	back = (txtarea.value).substring(strPos, txtarea.value.length);
	txtarea.value=front + text + back;
	strPos = strPos + text.length;
	if (browser === "ie") {
		txtarea.focus();
		range = document.selection.createRange();
		range.moveStart ('character', -txtarea.value.length);
		range.moveStart ('character', strPos);
		range.moveEnd ('character', 0);
		range.select();
	} else if (browser === "ff") {
		txtarea.selectionStart = strPos;
		txtarea.selectionEnd = strPos;
		txtarea.focus();
	}
	txtarea.scrollTop = scrollPos;
}

jQuery(document).ajaxError(function(request, data, settings, error) {
	var title;
	// Ignore errors with AppInfo as they should already be handled.  If status is zero, the
	// server is likely down, or an auth error.  AppInfo should update this page anyway shortly
	if(!settings.url.match(/^.*\/w\/[0-9]+\/appInfo.*$/) && data.status !== 0) {
		// remove loading screen just in case it is there
		hideThinking();
		// display details of the error page
		launchSmallPopup(data.status + ": " + data.statusText, data.responseText, i18n("action.ok"), function() { console.log("Error dialog closed."); });
	}
});

$(function() {
	$.extend($.validator.messages, {
		required: i18n("jquery.validation.required"),
		remote: i18n("jquery.validation.remote"),
		email: i18n("jquery.validation.email"),
		url: i18n("jquery.validation.url"),
		date: i18n("jquery.validation.date"),
		dateISO: i18n("jquery.validation.dateISO"),
		number: i18n("jquery.validation.number"),
		digits: i18n("jquery.validation.digits"),
		creditcard: i18n("jquery.validation.creditcard"),
		equalTo: i18n("jquery.validation.equalto"),
		accept: i18n("jquery.validation.accept"),
		maxlength: jQuery.validator.format(i18n("jquery.validation.maxlength")),
		minlength: jQuery.validator.format(i18n("jquery.validation.minlength")),
		rangelength: jQuery.validator.format(i18n("jquery.validation.rangelength")),
		range: jQuery.validator.format(i18n("jquery.validation.range")),
		max: jQuery.validator.format(i18n("jquery.validation.max")),
		min: jQuery.validator.format(i18n("jquery.validation.min"))
	});
});

