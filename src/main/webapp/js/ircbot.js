(function() {

var _channel;

var ircbot = {
    jsonget: function(url, callbackFn) {
	$.get(url,
	      function(data, status, request) {
		  if(callbackFn) {
		      callbackFn(data, status, request);		     
		  }
	      }, "json");	
    },
    botchannel: function(callbackFn) {
	console.log("ircbot.botchannel");
	ircbot.jsonget("/botchannel", callbackFn);
    },
    ircindex: function(channel, callbackFn) {
            console.log("ircbot.ircindex[" + channel + "]");
            ircbot.jsonget("/ircindex/" + channel, callbackFn);
    },
    irclog: function(channel, day, callbackFn) {
	console.log("ircbot.irclog[" + channel + "][" + day + "]");
	ircbot.jsonget("/irclog/" + channel + "/" + day, callbackFn);
    }
};

$(document).ready(
    function() {
	ircbot.botchannel(function(data, status, request) {
			  _channel = data.channel;
			  });

	ircbot.ircindex(_channel, function(data, status, request) {
			    var days = $("<ul>").addClass("days");
			    $.each(data["days-in-log"], function(i, day) {
				       var li = $("<li>").addClass("day")
					   .html(day)
					   .data("day", day)
					   .appendTo(days);
				   });			    
			    $("#indexlist").html(days);
			});

	$(".day").live("click", function(){
			   var day = $(this).data("day");
			   ircbot.irclog(_channel, day, function(data, status, request) {
					     var records = $("<ul>").addClass("records");
					     $.each(data["records"], function(i, rec) {
							var datetime = $("<span>").addClass("log-entry-datetime")
							    .html(rec.datetime);
							var sender = $("<span>").addClass("log-entry-sender")
							    .html(rec.sender);
							var message = $("<span>").addClass("log-entry-message")
							    .html(rec.message);
							var li = $("<li>").addClass("log-entry")
							    .append(datetime)
							    .append(sender)
							    .append(message)
							    .data("log-entry", rec)
							    .appendTo(records);
						    });
					     $("#logrecords").html(records);	
					 });
		       });
    });

 })();
