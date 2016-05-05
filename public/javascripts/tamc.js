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

function toggle_div(divId) {
	$("#" + divId).toggle();
}
