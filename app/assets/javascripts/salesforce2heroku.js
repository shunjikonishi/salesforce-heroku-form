if (typeof(flect) === "undefined") flect = {};
if (typeof(flect.app) === "undefined") flect.app = {};

flect.app.Salesforce2Heroku = function(status, obj) {
	var formJson = {
		"title" : obj.label,
		"helpImage" : "/public/images/help.png",
		"items" : {},
		"requiredAppendix" : "<span style='color:red;'>(*)</span>"
	};
	var btnSelObject = $("#btnSelObject").click(function() {
		var name = $("#selObject").val();
		if (!name) {
			alert("オブジェクトを選択してください");
			return;
		}
		location.href= "/selectField/" + name;
	});
	var navIndex = status - 1;
	if (navIndex > 3) {
		navIndex = 3;
	}
	$("#navList").find("li:eq(" + navIndex + ")").each(function() {
		var li = $(this),
			text = li.text();
		li.html("<b>" + text + "</b>");
	});
	$("#selObject").change(function() {
		var name = $(this).val();
		btnSelObject.attr("disabled", "disabled");
		$("#fieldLoading").css("display", "");
		if (name) {
			loadField(name);
		}
	});
	
	function getField(name) {
		for (var i=0; i<obj.fields.length; i++) {
			var f = obj.fields[i];
			if (f.name == name) {
				return f;
			}
		}
		return null;
	}
	function loadField(name) {
		$("#fieldList").load("/fieldList/" + name, function() {
			$("#fieldLoading").css("display", "none");
			var count = $("#fieldList").find("tr").length - 1;
			if (count > 0) {
				btnSelObject.removeAttr("disabled");
			} else {
				btnSelObject.attr("disabled", "disabled");
			}
		});
	}
	$("#tblSelectField").find(":input").change(function() {
		var checkbox = $(this),
			name = checkbox.val(),
			checked = checkbox.is(":checked");
			f = getField(name),
			$form = $("#generatedForm");
		if (!f) {
			return;
		}
		if (checked) {
			var item = {
				"label" : f.label,
				"type" : "text"
			}
			if (f.type == "textarea") {
				item.type = "textarea";
			} else if (f.type == "int" || f.type == "double") {
				item.number = true;
			} else if (f.type == "boolean") {
				item.type = "checkbox";
				if (f.defaultedOnCreate) {
					item.checked = true;
				}
			} else if (f.type == "date") {
				item.type = "date";
			} else if (f.type == "picklist") {
				item.type = "select";
				var options = [];
				for (var i=0; i<f.picklistValues.length; i++) {
					var v = f.picklistValues[i];
					if (v.active) {
						var v2 = {
							"value" : v.value,
							"text" : v.label
						};
						if (v.defaultValue) {
							v2.selected = true;
						}
						options.push(v2);
					}
				}
				item.values = options;
			} else if (f.type == "email") {
				item.email = true;
			} else if (f.type == "url") {
				item.url = true;
			}
			if (!f.nillable) {
				item.required = true;
			}
			if (f.inlineHelpText) {
				item.helpText = f.inlineHelpText;
			}
			formJson.items[name] = item;
		} else {
			delete formJson.items[name];
		}
		$.removeData($form[0], "validator");
		$form.empty().formbuilder(formJson);
	});
	$("#btnHerokuLogin").click(function() {
		$.ajax({
			"url" : "/postJson",
			"type" : "POST",
			"data" : {
				"json" : JSON.stringify(formJson)
			}, 
			"success" : function(data) {
				location.href = data;
			}
		});
	});
	
	$.extend(this, {
		"loadField" : loadField
	});
}

