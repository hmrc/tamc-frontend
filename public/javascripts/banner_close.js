$(document).ready(function() {
  var URbanner = $("#full-width-banner");

  $(".full-width-banner__close").on("click", function(e) {
    e.preventDefault();
    GOVUK.setCookie("tamc_ur_panel", 1, 99999999999);
    URbanner.removeClass("full-width-banner").addClass('banner-panel-close');
  });
});