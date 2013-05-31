	var preoperator = "";
	var preaddres = "";	
	var antenasEnable = false;
	var pointsEnable = true;
	var antenasOverlay = [];
	var map;
	var request;
	var infoDiv;
	var MERCATOR_RANGE = 256;
	var center;
	var zoom = 1;
	var selectedOperator = preoperator;
	var pointLayer ;
	var scaleLayer ;
	var operatorLayer;
	var logoLayer;
	var geocoder;
	var mouseLatLng = [0,0];

	function TogglePoints(anchor) {
		pointsEnable = !pointsEnable;
		anchor.innerText = (pointsEnable?"[DISABLESIGNALS]":"[ENABLESIGNALS]");
		anchor.className = (pointsEnable?"btnenable":"btndisabled");
		pointLayer.setVisible(pointsEnable);
	}

	function ToggleAntenas(anchor) {
		antenasEnable = !antenasEnable;
		anchor.innerText = (antenasEnable?"[DISABLEANTENNAS]":"[ENABLEANTENNAS]");
		anchor.className = (antenasEnable?"btnenable":"btndisabled");
		for(var i=0;i<antenasOverlay.length;i++) {
			antenasOverlay[i].setMap(antenasEnable?map:null);
		}	
	}

	function pointIdExistAnt(ant) {
		if(loadedAntenas.length > 0 ) {
			for(var i=0;i<loadedAntenas.length;i++) {
				if(loadedAntenas[i].lat == ant.lat & loadedAntenas[i].lon == ant.lon & loadedAntenas[i].operator == ant.operator)
					return true;
			}
			return false;
		}else
			return false;
	}	

	function changeOperator(operator) {
		if(selectedOperator != operator) {
			console.log("[CHANGINGOPERATORTO]: "+operator);
			pointLayer.removeAllTiles();
			for(var i=0;i<antenasOverlay.length;i++) 
				antenasOverlay[i].setMap(null);
			antenasOverlay = [];
			loadedAntenas = [];
			logoLayer.innerHTML = '<img src="{APIURL}?operadora='+operator+'&mode=logo" width=72 height=72/>';
			selectedOperator = operator;
			pointLayer.redraw();
			loadAntena();
		}
	}
	function loadData(data) {
		for(var i=0;i<data["antenas"].length;i++) {
			if(!pointIdExistAnt(data["antenas"][i])) {
				loadedAntenas.push(data["antenas"][i]);
				var myLatLng = new google.maps.LatLng(data["antenas"][i].lat, data["antenas"][i].lon);
				var antenaMarker = new google.maps.Marker({
					position: myLatLng,
					map: (antenasEnable?map:null),
					icon: '{SITEURL}images/tower32.png'
				});
				antenasOverlay.push(antenaMarker);
			}
		}
	}
	function procurar() {
				
		var address = $("#search_address").val();
		console.log("[SEARCHINGADDRESS]: "+address);
		geocoder.geocode( { 'address': address}, function(results, status) {
		  if (status == google.maps.GeocoderStatus.OK) {
			map.setCenter(results[0].geometry.location);
		  } else {
			alert("[WECANTFINDADDRESS]");
		  }
		});	 
	}
	function loadAntena() {
		if(selectedOperator!="" && antenasEnable) {
			var boundsA = bounds.getNorthEast();
			var boundsB = bounds.getSouthWest();
			resultsLoaded = 0
			var rq;
			if(selectedOperator!="")
				rq = {"method":"antenas","lat1":boundsA.lat(),"lon1":boundsA.lng(),"lat2":boundsB.lat(),"lon2":boundsB.lng(),"operator":selectedOperator};
			else
				rq = {"method":"antenas","lat1":boundsA.lat(),"lon1":boundsA.lng(),"lat2":boundsB.lat(),"lon2":boundsB.lng()};
			$.getJSON('{APIURL}', rq, loadData);
		}
	}
	function loadOperators() {
		$.getJSON('{APIURL}', {"method":"operators"}, function(data) {
			var output = "";
			for(var i=0;i<data.data.length;i++) 
				output += "<li><a href=\"javascript:void(0);\" onClick=\"changeOperator('"+data.data[i]+"');\">"+data.data[i]+"</a></li>";
			$("#operator_list").html(output).listview('refresh');
		});
	}
	function initialize() {
		var myPoint;
		$("#operator_list").listview();
		$("#toplist").hide();
		var myOptions = {zoom: 16,mapTypeId: google.maps.MapTypeId.ROADMAP};

		geocoder = new google.maps.Geocoder();
		map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
		google.maps.event.addListener(map, 'click', function(event) {
			mouseLatLng[0] = event.latLng.lat();
			mouseLatLng[1] = event.latLng.lng();
			console.log("[CLICKEDIN] "+event.latLng.toString());	
		});
		loadOperators();
		scaleLayer = document.createElement('div');
		scaleLayer.innerHTML = '<center><h3 class="scale_Text" style="line-height: 0.2;">[SCALE]</h3><B class="scale_Text">0% </B><img class="scale_img" src="{SITEURL}images/scale.png" style="vertical-align:middle"/><B class="scale_Text"> 100%</B><BR><BR><B class="scale_Text">50%</B></center>';
		scaleLayer.style.opacity = '0.8';
		scaleLayer.style.lineHeight = "0.6";
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(scaleLayer);

		$("#search_address").keyup(function(event){
			if(event.keyCode == 13){
				procurar();
			}
		});
		logoLayer = document.createElement('div');
		logoLayer.innerHTML = '<img src="{APIURL}?operadora=none&mode=logo" width=72 height=72/>';
		logoLayer.style.opacity = '0.8';
		logoLayer.style.width = '120px';
		logoLayer.style.height = '80px';
		logoLayer.style.textAlign = 'center';
		map.controls[google.maps.ControlPosition.RIGHT_TOP].push(logoLayer);
		
		btnlay  = document.createElement('div');
		btnlay.innerHTML = '<center><a href="javascript:void(0);" onClick="TogglePoints(this);" class="btnenable" id="tgpoint" data-ajax="false" data-role="none">[DISABLESIGNALS]</a><BR><a href="javascript:void(0);" onClick="ToggleAntenas(this);" class="btndisabled" id="tgantena"data-ajax="false" data-role="none">[ENABLEANTENNAS]</a></center>';
		btnlay.style.width = '220px';
		btnlay.style.height = '100px';
		map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(btnlay);

		if(navigator.geolocation & preaddress == "") {
				console.log("[GOINGTOHTMLLOC]"); 
				navigator.geolocation.getCurrentPosition(function(position) {
				myPoint = new google.maps.LatLng(position.coords.latitude,position.coords.longitude);
				map.setCenter(myPoint);
			}, function() {
				console.log("[GOINGTODEFAULTLOC]"); 
					center = new google.maps.LatLng(-23.54894330,-46.63881820);
					zoom = 16;
					map.setCenter(center);
				});
			}else{
				console.log("[GOINGTODEFAULTLOC]"); 
				center = new google.maps.LatLng(-23.54894330,-46.63881820);
				zoom = 16;
				map.setCenter(center);
		}
	
        pointLayer = new mobilles.web.TileOverlay(function(x, y, z) {  return selectedOperator!=""?"{APIURL}?operadora="+selectedOperator+"&tile=" + z + "-" + x + "-" + y:""; },{'map': map, 'visible': true,'minZoom': 10,'maxZoom': 16, 'percentOpacity': 100} );
		google.maps.event.addListener(map, 'bounds_changed', function() {
			center = map.getCenter();
			zoom = map.getZoom();
			bounds = map.getBounds();
			loadAntena();
		});
		if(preaddress != "") {
			$("#search_address").val(preaddress);
			geocoder.geocode( { 'address': preaddress}, function(results, status) {
			  if (status == google.maps.GeocoderStatus.OK) 
				map.setCenter(results[0].geometry.location);
			  else
				alert("[WECANTFINDADDRESS]");
			});			
		}

		if(preoperator != "")	{
			changeOperator('none');
			changeOperator(preoperator);
		}
	}
	function ShortLink(link)	{
		console.log("Shortening URL: "+link);
		$.getJSON('{SITEURL}short.php', {"URL":link}, ShortLinkCB);
	}
	function ShortLinkCB(data)	{
		if(data.url == null)
			copyToClipboard("{SITEURL}maps/"+encodeURIComponent(preaddress)+"/"+encodeURIComponent(selectedOperator));
		else
			copyToClipboard(data.url);
	}
	function generateLink()	{
		console.log("[SEARCHINGADDRESS]: "+center);
		geocoder.geocode( { 'latLng': center}, function(results, status) {
		  if (status == google.maps.GeocoderStatus.OK) {
			preaddress = results[0].formatted_address;
			ShortLink("{SITEURL}maps/"+encodeURIComponent(results[0].formatted_address)+"/"+encodeURIComponent(selectedOperator));
		  } else {
			alert("[WECANTFINDADDRESS]");
		  }
		});	 
	}
	function gotoPos(lat,lon) {
			myPoint = new google.maps.LatLng(lat,lon);
			map.setCenter(myPoint);	
	}
	function copyToClipboard (text) {
	  window.prompt ("[COPYTOCLIPBOARD]: Ctrl+C, Enter", text);
	}
