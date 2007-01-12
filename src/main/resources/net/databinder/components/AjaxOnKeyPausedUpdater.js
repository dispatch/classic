/* Global pause timing variables */
var AjaxOnKeyPausedTimerN = 0;
var AjaxOnKeyPausedTimerElement;

function AjaxOnKeyPausedTimerReset(element) {
	AjaxOnKeyPausedTimerElement = element;
	var n = ++AjaxOnKeyPausedTimerN;
	// set half second timer
	setTimeout(function() { AjaxOnKeyPausedTimerCheck(n); }, 500);
}

function AjaxOnKeyPausedTimerCheck(n) {
	if (n == AjaxOnKeyPausedTimerN) {
		AjaxOnKeyPausedTimerElement.blur();
		AjaxOnKeyPausedTimerElement.focus();
	}
}

function AjaxOnKeyPausedTimerCancel() {
	AjaxOnKeyPausedTimerN = 0;
}