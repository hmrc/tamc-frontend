(function($, window, document) {
    $("a#apply.button").click(function(event) {
        if ($(this).hasClass("disabled")) {
            event.preventDefault();
        }
        $(this).addClass("disabled");
    });
})(jQuery, window, document);

$(function() {

    $('*[data-inline-error]').each(function() {

        var $self = $(this);
        var $error = $('#' + $self.data('inline-error'));
        var $input = $self.find('input');

        $input.each(function() {

            var $this = $(this);

            if ($this.val() === 'false' && $this.prop('checked')) {

                $error.show();
            }
        });

        $input.change(function() {

            var $this = $(this);

            if ($this.val() === 'false') {
                $error.show();
            } else {
                $error.hide();
            }
        });
    });
});

$(function() {

    $('*[data-inline-instruction]').each(function() {

        var $self = $(this);
        var $input = $self.find('input');

        $input.change(function() {

            var $this = $(this);

            if ($this.val() === 'DIVORCE_CY') {
            	$( "#eoy-yes" ).show();
            	$( "#eoy-no" ).hide();
            } else {
            	$( "#eoy-no" ).show();
            	$( "#eoy-yes" ).hide();
            }
        });
    });
});

$(function() {

     var $radio = $("input:radio[name=marriage-criteria]");
  	 $radio.click(function(){
        var $input = $("input:radio[value=true]:checked").val();
        if ($input) {
          ga('send', {
            			hitType: 'event',
            			eventCategory: 'marriage-allowance',
            			eventAction: 'radio selection',
           			    eventLabel: 'married_yes'

          });
        } else {
             ga('send', {
                             hitType: 'event',
                             eventCategory: 'marriage-allowance',
                             eventAction: 'radio selection',
                             eventLabel: 'married_no'
             });
        }
   	 });
 });

 $(function() {

      var $radio = $("input:radio[name=lower-earner]");
   	 $radio.click(function(){

         var $input = $("input:radio[value=true]:checked").val();

         if ($input) {


           ga('send', {
             			hitType: 'event',
             			eventCategory: 'marriage-allowance',
             			eventAction: 'radio selection',
            			eventLabel: 'lowerearner_yes'

           });
         } else {

              ga('send', {
                             hitType: 'event',
                             eventCategory: 'marriage-allowance',
                             eventAction: 'radio selection',
                             eventLabel: 'lowerearner_no'
              });
         }
    	 });
  });

  $(function() {

      var $radio = $("input:radio[name=date-of-birth]");
      $radio.click(function(){
          var $input = $("input:radio[value=true]:checked").val();
          if ($input) {
              ga('send', {
                  hitType: 'event',
                  eventCategory: 'marriage-allowance',
                  eventAction: 'radio selection',
                  eventLabel: 'dateofbirth_yes'
              });
          } else {

              ga('send', {
                  hitType: 'event',
                  eventCategory: 'marriage-allowance',
                  eventAction: 'radio selection',
                  eventLabel: 'dateofbirth_no'
              });
          }
      });
  });

$(function() {

    var $radio = $("input:radio[name=do-you-live-in-scotland]");
    $radio.click(function(){
        var $input = $("input:radio[value=true]:checked").val();
        if ($input) {
            ga('send', {
                hitType: 'event',
                eventCategory: 'marriage-allowance',
                eventAction: 'radio selection',
                eventLabel: 'liveinscotland_yes'
            });
        } else {

            ga('send', {
                hitType: 'event',
                eventCategory: 'marriage-allowance',
                eventAction: 'radio selection',
                eventLabel: 'liveinscotland_no'
            });
        }
    });
});

$(function() {

    var $radio = $("input:radio[name=do-you-want-to-apply]");
    $radio.click(function(){
        var $input = $("input:radio[value=true]:checked").val();
        if ($input) {
            ga('send', {
                hitType: 'event',
                eventCategory: 'marriage-allowance',
                eventAction: 'radio selection',
                eventLabel: 'applyformarriageallowance_yes'
            });
        } else {

            ga('send', {
                hitType: 'event',
                eventCategory: 'marriage-allowance',
                eventAction: 'radio selection',
                eventLabel: 'applyformarriageallowance_no'
            });
        }
    });
});

  $(function() {

       var $radio = $("input:radio[name=partners-income]");
    	 $radio.click(function(){
         var $input = $("input:radio[value=true]:checked").val();
          if ($input) {
            ga('send', {
              			hitType: 'event',
              			eventCategory: 'marriage-allowance',
              			eventAction: 'radio selection',
             			eventLabel: 'partnersincome_yes'
            });
          } else {

               ga('send', {
                              hitType: 'event',
                              eventCategory: 'marriage-allowance',
                              eventAction: 'radio selection',
                              eventLabel: 'partnersincome_no'
               });
          }
     	 });
   });

   $(function() {

        var $radio = $("input:radio[name=applyForCurrentYear]");
     	 $radio.click(function(){
           var $input = $("input:radio[value=true]:checked").val();

           if ($input) {
             ga('send', {
               			hitType: 'event',
               			eventCategory: 'marriage-allowance',
               			eventAction: 'radio selection',
              			eventLabel: 'currentyear_yes'
             });

           } else {

                ga('send', {
                              hitType: 'event',
                              eventCategory: 'marriage-allowance',
                              eventAction: 'radio selection',
                              eventLabel: 'currentyear_no'
                });
           }

      	 });
    });

   $(function() {

        var $radio = $("input:radio[name=endReason]");
     	 $radio.click(function(){
           var $divorce = $("input:radio[value=DIVORCE]:checked").val();
           var $cancel = $("input:radio[value=CANCEL]:checked").val();
           var $reject = $("input:radio[value=REJECT]:checked").val();

           if ($divorce) {
             ga('send', {
               			hitType: 'event',
               			eventCategory: 'marriage-allowance',
               			eventAction: 'radio selection',
              			eventLabel: 'reason_divorce'
             });

           }
           if ($cancel) {

                ga('send', {
                              hitType: 'event',
                              eventCategory: 'marriage-allowance',
                              eventAction: 'radio selection',
                              eventLabel: 'reason_cancel'
                });
           }
           if ($reject) {

                ga('send', {
                              hitType: 'event',
                              eventCategory: 'marriage-allowance',
                              eventAction: 'radio selection',
                              eventLabel: 'reason_reject'
                });
           }

      	 });
    });

   $(function() {

        var $radio = $("input:radio[name=endReason]");
     	 $radio.click(function(){
           var $current = $("input:radio[value=DIVORCE_CY]:checked").val();
           var $previous = $("input:radio[value=DIVORCE_PY]:checked").val();

           if ($current) {
             ga('send', {
               			hitType: 'event',
               			eventCategory: 'marriage-allowance',
               			eventAction: 'radio selection',
              			eventLabel: 'divorcechoice_endofyear'
             });

           }
           if ($previous) {

                ga('send', {
                              hitType: 'event',
                              eventCategory: 'marriage-allowance',
                              eventAction: 'radio selection',
                              eventLabel: 'divorcechoice_startofyear'
                });
           }

      	 });
    });

     $(function() {

        var $breadCrumb = $("#global-breadcrumb nav ol li a");
        	$breadCrumb.click(function(){

        		event.preventDefault();
        		$href = $(this).attr('href')
        		$path = $href.substring($href.lastIndexOf("/")+1);
        		ga('send', {
          			hitType: 'event',
          			eventCategory: 'marriage-allowance',
          			eventAction: 'outboundlink',
          			eventLabel: "'"+$path+"" + "click'",
        				hitCallback: function() {
             			window.location.href = $href;
        				}
        		});

        	});

        });

function toggle_div(divId) {
	$("#" + divId).toggle();
}

function getPageTitle() {
  return document.getElementsByTagName("title")[0].innerHTML.split("-")[0].trim();
}

$(function() {
  if ( $('.error-notification').length ) {
    ga('send', {
      hitType: 'event',
      eventCategory: 'error - field',
      eventAction: getPageTitle(),
      eventLabel: $('.error-notification').text().trim()
    });
  }
});

$(function() {
  $('span.error-message').each(function() {
    ga('send', {
      hitType: 'event',
      eventCategory: 'error - field',
      eventAction: getPageTitle(),
      eventLabel: $(this).text().trim()
    });
  });
});

$(function() {
  $('.accordion').click(function() {
    ga('send', {
      hitType: 'event',
      eventCategory: 'accordion - click',
      eventAction: getPageTitle(),
      eventLabel: $(this).text().trim()
    });
  });
});