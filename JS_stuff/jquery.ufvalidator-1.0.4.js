/*
 * 
 * Utopic Farm 2010
 * @author Tolga Arican
 * @website www.utopicfarm.com
 * @version 1.0.4
 * 
 */


// FORM VALIDATOR JQUERY PLUGIN - START

(function($) {
	
	$.fn.formValidator = function(options) {
		$(this).click($.process(options));
	};


    $.fn.formValidator2 = function(options) {
        $(":input").blur(function () {
            var formId = $(this).parents("form").attr('id');
            //alert("blur: "+formId);
            return $.process({scope : '#'+formId});
        });

        $("form").submit(function () {
            var formId = $(this).attr('id'); 
            //alert("submit: "+formId);
            return $.process({scope : '#'+formId});
        });

    };

     $.process = function (options) {

			var result = $.formValidator(options);

			if (result && jQuery.isFunction(options.onSuccess)) {
				options.onSuccess();
				return false;
			} else if (!result && jQuery.isFunction(options.onError)) {
				options.onError();
				return false;
			} else {
                return result;
			}
		}

	$.formValidator = function (options) {
		
		// merge options with defaults
		var merged_options = $.extend({}, $.formValidator.defaults, options);
		
		// result boolean
		var boolValid = true;
		
		// result error message
		var errorMsg = '';
		
		// clean errors
		$(merged_options.scope + ' .error-both, ' + merged_options.scope + ' .error-same, ' + merged_options.scope + ' .error-input').removeClass('error-both').removeClass('error-same').removeClass('error-input');
		
		// gather inputs & check is valid
		$(merged_options.scope+' .req-email, '+merged_options.scope+' .req-string, '+merged_options.scope+' .req-same, '+merged_options.scope+' .req-both, '+merged_options.scope+' .req-numeric, '+merged_options.scope+' .req-date, '+merged_options.scope+' .req-datetime, '+merged_options.scope+' .req-min').each(function() {
			thisValid = $.formValidator.validate($(this),merged_options);
			boolValid = boolValid && thisValid.error;
			if (!thisValid.error) errorMsg  = thisValid.message;
		});
		
		// check extra bool
		if (!merged_options.extraBool() && boolValid) {
			boolValid = false;
			errorMsg = merged_options.extraBoolMsg;
		}
		
		// submit form if there is and valid
		if ((merged_options.scope != '') && boolValid) {
			$(merged_options.errorDiv).fadeOut();
		}
		
		// if there is errorMsg print it if it is not valid
		if (!boolValid && errorMsg != '') {
			
			var tempErr = (merged_options.customErrMsg != '') ? merged_options.customErrMsg : errorMsg;
			$(merged_options.errorDiv).hide().html(tempErr).fadeIn();
		}
		return boolValid;
	};
	
	$.formValidator.validate = function(obj,opts) {

		var valAttr = obj.val();
		var css = opts.errorClass;
		var mail_filter = /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
		var numeric_filter = /(^-?\d\d*\.\d*$)|(^-?\d\d*$)|(^-?\.\d\d*$)|(^-?\d*$)/;
		var tmpresult = true;
		var result = true;
		var errorTxt = '';
		
		if(obj.is('input:enabled')) {
			
			// REQUIRED FIELD VALIDATION
			if (obj.hasClass('req-string')) {
				tmpresult = (valAttr != '');
				if (!tmpresult) errorTxt = opts.errorMsg.reqString;
				result = result && tmpresult;
			}
			// SAME FIELD VALIDATION
			if (obj.hasClass('req-same')) {
				
				tmpresult = true;
				
				group = obj.attr('rel');
				tmpresult = true;
				$(opts.scope+' .req-same[rel="'+group+'"]').each(function() { 
					if($(this).val() != valAttr || valAttr == '') {
						tmpresult = false;
					}
				});
				if (!tmpresult) {
					$(opts.scope+' .req-same[rel="'+group+'"]').parent().parent().addClass('error-same');
					errorTxt = opts.errorMsg.reqSame;
				} else {
					$(opts.scope+' .req-same[rel="'+group+'"]').parent().parent().removeClass('error-same');
				}
				
				result = result && tmpresult;
			}
			// BOTH INPUT CHECKING
			// if one field entered, the others should too.
			if (obj.hasClass('req-both')) {
				
				tmpresult = true;
				
				if (valAttr != '') {
					
					group = obj.attr('rel');
	
					$(opts.scope+' .req-both[rel="'+group+'"]').each(function() { 
						if($(this).val() == '') {
							tmpresult = false;
						}
					});
					
					if (!tmpresult) {
						$(opts.scope+' .req-both[rel="'+group+'"]').parent().parent().addClass('error-both');
						errorTxt = opts.errorMsg.reqBoth;
					} else {
						$(opts.scope+' .req-both[rel="'+group+'"]').parent().parent().removeClass('error-both');
					}
				}
				
				result = result && tmpresult;
			}
			// E-MAIL VALIDATION
			if (obj.hasClass('req-email')) {
				tmpresult = mail_filter.test(valAttr);
				if (!tmpresult) errorTxt = (valAttr == '') ? opts.errorMsg.reqMailEmpty : opts.errorMsg.reqMailNotValid;
				result = result && tmpresult;
			}
			// DATE VALIDATION
			if (obj.hasClass('req-date')) {
				
				tmpresult = true;
				
				if (valAttr == '') {
					tmpresult = true;
				} else {
	                var arr = valAttr.split(opts.dateSeperator);
	                var curDate = new Date();
					if (arr.length < 3) {
						tmpresult = false;
					} else {
						tmpresult = (arr[0] > 0) && (arr[0] <= 31) && (arr[1] > 0) && (arr[1] <= 21) && (arr[2] > 0) &&(arr[2] <= curDate.getFullYear());
					}
				}
				
				if (!tmpresult) errorTxt = opts.errorMsg.reqDate;
				result = result && tmpresult;
			}
	        // DATETIME VALIDATION
	        if (obj.hasClass('req-datetime')) {
	
	            tmpresult = true;
	
	            if (valAttr == '') {
	                tmpresult = true;
	            } else {
	                var dateTimeArr = valAttr.split(opts.dateTimeSeperator);
	                tmpresult = (dateTimeArr.length == 2);
	                if (tmpresult) {
	                    var dateArr = dateTimeArr[0].split(opts.dateSeperator);
	                    var timeArr = dateTimeArr[1].split(opts.timeSeperator);
	                    var curDate = new Date();
	                    if(timeArr.length != 2) {
	                        tmpresult = false;
	                    } else {
	                        tmpresult = (timeArr[0] >=0)&&(timeArr[0]<=24) && (timeArr[1] >=0)&&(timeArr[1]<=60);
	                    }
	                    if (dateArr.length != 3) {
	                        tmpresult = false;
	                    } else {
	                        tmpresult = tmpresult && isDate(dateTimeArr[0]);
	                        //tmpresult = tmpresult && (dateArr[0] > 0) && (dateArr[0] <= 31) && (dateArr[1] > 0) && (dateArr[1] <= 21) && (dateArr[2] > 0) &&(dateArr[2] <= curDate.getFullYear());
	                    }
	                }
	
	            }
	
	            if (!tmpresult) errorTxt = opts.errorMsg.reqDate;
	            result = result && tmpresult;
	        }
			// MINIMUM REQUIRED FIELD VALIDATION
			if (obj.hasClass('req-min')) {
				tmpresult = (valAttr.length >= obj.attr('minlength'));
				if (!tmpresult) errorTxt = opts.errorMsg.reqMin.replace('%1',obj.attr('minlength'));
				result = result && tmpresult;
			}
			// NUMERIC FIELD VALIDATION
			if (obj.hasClass('req-numeric')) {
				tmpresult = numeric_filter.test(valAttr);
				if (!tmpresult) errorTxt = opts.errorMsg.reqNum;
				result = result && tmpresult;
			}
			
			if (obj.attr('rel')) {
				if (result) { $('#'+obj.attr('rel')).removeClass(css); } else { $('#'+obj.attr('rel')).addClass(css); }
			} else {
				if (result) { obj.removeClass(css); } else { obj.addClass(css); }
			}
		}
		return {
			error: result,
			message: errorTxt
		};
	};
	
	// CUSTOMIZE HERE or overwrite by sending option parameter
	$.formValidator.defaults = {
		onSuccess		:	null,
		onError			:	null,
		scope			:	'',
		errorClass		:	'error-input',
		errorDiv		:	'#warn',
		errorMsg		: 	{
								reqString		:	'Gerekli alanları doldurun',
								reqDate			:	'Tarih geçerli <b>değil</b>!',
								reqNum			:	'Sadece sayı girebilirsiniz',
								reqMailNotValid	:	'E-mail geçerli <b>değil</b>!',
								reqMailEmpty	:	'E-mail girin',
								reqSame			:	'Tekrar alanları aynı <b>değil</b>!',
								reqBoth			:	'İlgili alanları doldurmalısınız!',
								reqMin			:	'Minimum %1 karakter girmelisiniz'
							},
		customErrMsg	:	'',
		extraBoolMsg	:	'Formu dikkatli kontrol edin!',
		dateSeperator	:	'.',
        timeSeperator	:	':',
        dateTimeSeperator	:	' ',
		extraBool		:	function() { return true; }
	};
})(jQuery);

// FORM VALIDATOR JQUERY PLUGIN - END

