<%@ page import="frontlinesms2.Webconnection" %>
<r:script>
	var webconnectionDialog = (function() {
		var _updateConfirmationScreen = function() {};
		var validationMessageGenerator = function(fieldName, ruleName) {
			var i18nKey = "webconnection." + fieldName + ".validation.error";
			var i18nString = i18n(i18nKey);
			return i18nKey == i18nString? "": i18nString;
		};
		var _handlers = {}
		var generateMessages = function(fieldsAndRules) {
			var messageMap = {};
			for(field in fieldsAndRules) {
				var i, rules = fieldsAndRules[field];
				messageMap[field] = {};
				if(typeof(rules) === "string") {
					messageMap[field][rules] = validationMessageGenerator(field, rules);
				} else {
					for(i=0; i<rules.length; ++i) {
						messageMap[field][rules[i]] = validationMessageGenerator(field, rules[i]);
					}
				}
			}
			return messageMap;
		};

		return {
			resetValidator: function(messageRules) {
				$("#new-webconnection-form").data("validator", null);
				validator = $("#new-webconnection-form").validate({
					errorContainer:".error-panel",
					messages:generateMessages(messageRules)
				});
			},
			setScripts: function(scripts) {
				webconnectionDialog.resetValidator(scripts.validation);
				webconnectionDialog.updateConfirmationScreen = scripts.updateConfirmationScreen;
				webconnectionDialog.handlers = scripts.handlers
			},
			updateConfirmationScreen:_updateConfirmationScreen,
			handlers:_handlers,
			___end___:null
		};
	})();

	function initializePopup() {
		<g:if test="${activityInstanceToEdit?.id}">
			var initialScripts = <fsms:render template="/webconnection/${activityInstanceToEdit?.class?.type}/scripts"/>;
			webconnectionDialog.setScripts(initialScripts);
			webconnectionDialog.updateConfirmationScreen()
		</g:if>
		<g:else>
			var initialScripts = <fsms:render template="/webconnection/${Webconnection.implementations[0].type}/scripts"/>;
			webconnectionDialog.setScripts(initialScripts);
		</g:else>
		
		aliasCustomValidation();
		genericSortingValidation();

		var keyWordTabValidation = function() {
			 if(!isGroupChecked("blankKeyword")) return validator.element('#keywords');
			 else return true;
		};

		var configureTabValidation = function() {
			var isValid = true;
			$('#webconnection-config input:visible').each(function() {
				isValid = isValid && validator.element(this);
			});
			return isValid;
		};

		var confirmTabValidation = function() {
			return validator.element('input[name=name]');
		};

		mediumPopup.addValidation('activity-generic-sorting', keyWordTabValidation);
		mediumPopup.addValidation('webconnection-configure', configureTabValidation);
		mediumPopup.addValidation('webconnection-confirm', confirmTabValidation);

		$("#tabs").bind("tabsshow", function(event, ui) {
			updateConfirmationMessage();
			webconnectionDialog.updateConfirmationScreen();
		});
	}

	function setType(type) {
		$.getJSON(url_root + "webconnection/" + type + "/config", function(data) {
			var configTab = $("#webconnection-config");
			var confirmTab = $("#webconnection-confirm");
			configTab.html(data.config);
			confirmTab.html(data.confirm);
			magicwand.init(configTab.find('select[id^="magicwand-select"]'));

			$("#webconnection-confirm").html(data.confirm);

			webconnectionDialog.setScripts(eval("(" + data.scripts + ")"));
			webconnectionDialog.updateConfirmationScreen();
		});
	}

	function setPara(selecter, text) {
		$(selecter).html("<p>" + text + "</p>");
	}
	
	function updateConfirmationMessage() {
		var keywordConfirmationText;

		if(mediumPopup.getCurrentTabIndex() === mediumPopup.getTabLength()) {
			showTestRouteBtn();
		}
		if(!(isGroupChecked("blankKeyword"))) {
			keywordConfirmationText = $('#keywords').val().toUpperCase();
		} else {
			keywordConfirmationText = i18n("autoreply.blank.keyword");
		}
		setPara("#keyword-confirm", keywordConfirmationText);
		setPara("#autoreply-confirm", $('#messageText').val());
	}


	function toggleTestButton(ctx) {
		if(ctx.checked) {
			showTestRouteBtn();
		} else {
			hideTestRouteBtn();
		}
	}

	function showTestRouteBtn() {
		var buttonSet, testRouteBtn; 
		buttonSet = $('.ui-dialog-buttonset');
		testRouteBtn = buttonSet.find("#testRoute");
		if(testRouteBtn.length === 0) {
			testRouteBtn = $('<input/>', {
								id: "testRoute",
								type:"submit",
								value: i18n('webconnection.testroute.label'),
								click: testRouteStatus
							});
		} else {
			testRouteBtn.show();
		}
		buttonSet.append(testRouteBtn);
	}

	function hideTestRouteBtn() {
		$("#submit").css("display", "inline");
		$("#testRoute").hide();
	}

	function testRouteStatus() {
		if(mediumPopup.tabValidates(mediumPopup.getCurrentTab())) {
			$.ajax({
				type: 'post',
				data: $("#new-webconnection-form").serialize(),
				url: "${g.createLink(controller:'webconnection', action:'testRoute', params:['ownerId': activityInstanceToEdit?.id, 'format':'json'])}",
				success: function(data, textStatus) {  	checkRouteStatus(data)}
			});	
		} else {
			$('.error-panel').show();
		}
		return false;
	}

	var pollInterval

	function checkRouteStatus(response) {
		$("#testRoute").attr('disabled');
		if(response.ok) {
			$("#testRoute").attr("value", i18n('webconnection.testing.label'));
			$.ajaxSetup({
				type: 'post',
				data: {ownerId:response.ownerId},
				url: "${g.createLink(controller:'webconnection', action:'checkRouteStatus')}"
			});
			
			pollInterval = setInterval( function() {
				$.ajax({
					success: function(response) {
								if(response.status === "${Webconnection.OWNERDETAIL_SUCCESS}" || response.status === "${Webconnection.OWNERDETAIL_FAILED}") {
									$(".error-panel").text(i18n('webconnection.popup.'+ response.status + '.label'));
									$(".error-panel").show();
									if(response.status === "${Webconnection.OWNERDETAIL_SUCCESS}") {
										loadSummaryTab(response, i18n('webconnection.label'));
									} else {
										$("#testRoute").attr("value", i18n('webconnection.testroute.label'));
									}
									clearInterval(pollInterval);
								}
							}
				});	
			}, 3000);

		} else {
			displayErrors(response)
		}
	}
		
</r:script>

