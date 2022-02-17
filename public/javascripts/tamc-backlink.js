/* Back link configuration */
// store referrer value to cater for IE - https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/10474810/  */
var docReferrer = document.referrer
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}
var backLinkElem = document.getElementById("backLink");
if (backLinkElem !=  null){
    if (backLinkElem.getAttribute("href") == "" && window.history && window.history.back && typeof window.history.back === 'function') {
        var backScript = (docReferrer === "" || docReferrer.indexOf(window.location.host) !== -1) ? "javascript:window.history.back(); return false;" : "javascript:void(0);"
        backLinkElem.setAttribute("onclick",backScript);
        backLinkElem.setAttribute("href","javascript:void(0);");
    }
}