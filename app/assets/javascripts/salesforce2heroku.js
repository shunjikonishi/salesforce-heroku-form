if (typeof(flect) === "undefined") flect = {};
if (typeof(flect.app) === "undefined") flect.app = {};

flect.app.Salesforce2Heroku = function() {
	
	var btnSelObject = $("#btnSelObject").click(function() {
		var name = $("#selObject").val();
		if (!name) {
			alert("オブジェクトを選択してください");
			return;
		}
		location.href= "/selectField/" + name;
	});
	$("#selObject").change(function() {
		var name = $(this).val();
		btnSelObject.attr("disabled", "disabled");
		if (name) {
			loadField(name);
		}
	});
	
	function loadField(name) {
		$("#fieldList").load("/fieldList/" + name, function() {
			var count = $("#fieldList").find("tr").length - 1;
			if (count > 0) {
				btnSelObject.removeAttr("disabled");
			} else {
				btnSelObject.attr("disabled", "disabled");
			}
		});
	}
	
	$.extend(this, {
		"loadField" : loadField
	});
}

