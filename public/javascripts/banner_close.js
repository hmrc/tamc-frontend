$(document).ready(function() {
  var URbanner = $("#full-width-banner");
   $(".full-width-banner__close").on("click", function(e) {
    e.preventDefault();
    GOVUK.setCookie("mdtpurr", 1, {days: 30});
    URbanner.removeClass("full-width-banner").addClass('banner-panel-close');
  });
});
