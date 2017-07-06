// we will handle click events
if (!window.hasOwnProperty('frost_click_interceptor')) {
  console.log('Registering frost_click_interceptor');
  window.frost_click_interceptor = true;
  document.addEventListener('click', function _menuClick(e) {
    var element = e.target || e.srcElement;
    console.log(element.tagName);
  }, true);
}
