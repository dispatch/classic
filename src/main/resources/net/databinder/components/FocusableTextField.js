/* Try to focus any element requesting focus */
function initFocusableTextField() {
	wantsFocus = document.getElementById('focusMe');
	if (wantsFocus != null)
		wantsFocus.focus();
	
	if (document.getElementById('info-feedback') != null) {
	  new Effect.Fade('info-feedback', { delay: 3 });
	}		
}