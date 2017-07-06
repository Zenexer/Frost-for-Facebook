// we will handle click events
if (!window.hasOwnProperty('frost_click_a')) {
  console.log('Registering frost_click_a');
  window.frost_click_a = true;

  var _frostAClick = function(e) {
    var element = e.target || e.srcElement;
    if (element.tagName !== 'A') element = element.parentNode;
    //Notifications is two layers under
    if (element.tagName !== 'A') element = element.parentNode;
    if (element.tagName === 'A' && element.getAttribute('href') !== '#') {
      var url = element.getAttribute('href');
      if (url.includes('photoset_token')) return;
      console.log('Click Intercept', url);
      Frost.loadUrl(url);
      e.stopPropagation();
      e.preventDefault();
    }
  }

  document.addEventListener('click', _frostAClick, true);
}
