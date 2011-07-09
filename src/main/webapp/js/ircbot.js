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
    },
    extractTime: function(utcDateTime) {
	var d = new Date(utcDateTime);
	return d.getHours().toString() + ":" + d.getMinutes().toString();
    },
    extractDate: function(datetimeDay) {
	var y = datetimeDay.substring(0, 4);
	var m = datetimeDay.substring(4, 6);
	var d = datetimeDay.substring(6, 8);
	return m + "/" + d + "/" + y;
    },
    formatNick: function(nick) {
	return "&lt;" + nick + "&gt;";
    },
    setChannel: function(channel) {
	_channel = channel;
    },
    getChannel: function() {
	return _channel;
    },
    presentIndex: function(channel) {
	ircbot.ircindex(channel, function(data, status, request) {
			    $("#ircbottitle").addClass("irc-title")
				.html($("<a>")
				      .addClass("irc-title-link")
				      .html(_channel + " Log"));
			    $("#logrecords").empty();
			    $(".days-hidden").removeClass("days-hidden").addClass("days");
			    var days = $("<ul>").addClass("days");
			    $.each(data["days-in-log"], function(i, day) {
				       var li = $("<li>").addClass("day")
					   .append($("<div>").addClass("day-text").html(ircbot.extractDate(day)))
					   .data("day", day)
					   .appendTo(days);
				   });			    
			    $("#indexlist").html(days);
			});
    }
};

$(document).ready(
    function() {
	ircbot.botchannel(function(data, status, request) {
	    ircbot.setChannel(data.channel.substring(1, data.channel.length));
	    ircbot.presentIndex(ircbot.getChannel());
	});

	$("#ircbottitle").live("click", function() {
				   ircbot.presentIndex(ircbot.getChannel());
			       });

	$(".day").live("click", function() {
            $("#logrecords").empty();
            $(".days").removeClass("days").addClass("days-hidden");
	    var day = $(this).data("day");
	    ircbot.irclog(ircbot.getChannel(), day, function(data, status, request) {
		var records = $("#logrecords").addClass("records");
		$.each(data["records"], function(i, rec) {
		    var datetime = $("<a>").addClass("log-entry-datetime")
			.attr("href", "#" + ircbot.extractTime(rec.datetime))
			.html(ircbot.extractTime(rec.datetime));
		    var sender = $("<span>").addClass("log-entry-sender")
			.html(ircbot.formatNick(rec.sender));
		    var message = $("<span>").addClass("log-entry-message")
			.html(rec.message);
		    var logentry = $("<p>").addClass("log-entry")
			.append(datetime)
			.append(sender)
			.append(message)
			.data("log-entry", rec)
			.appendTo(records);
		});
	    });
	});
    });

 })();
