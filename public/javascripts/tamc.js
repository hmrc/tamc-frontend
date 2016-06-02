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
