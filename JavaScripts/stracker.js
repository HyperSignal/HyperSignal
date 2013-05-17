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
	
	function TogglePoints(anchor) {
		pointsEnable = !pointsEnable;
		anchor.innerText = (pointsEnable?"Desativar Sinais":"Ativar Sinais");
		pointLayer.setVisible(pointsEnable);

	}
	function ToggleAntenas(anchor) {
		antenasEnable = !antenasEnable;
		anchor.innerHTML = (antenasEnable?"Desativar Antenas <font color=red>Beta</font>":"Ativar Antenas <font color=red>Beta</font>");
		for(var i=0;i<antenasOverlay.length;i++) {
			antenasOverlay[i].setMap(antenasEnable?map:null);
		}	
	}
	function pointIdExistAnt(id) {
		if(loadedAntenas.length > 0 ) {
			for(var i=0;i<loadedAntenas.length;i++) {
				if(loadedAntenas[i].id == id)
					return true;
			}
			return false;
		}else
			return false;
	}	
	function changeOperator(operator) {
		if(selectedOperator != operator) {
			pointLayer.removeAllTiles();
			for(var i=0;i<antenasOverlay.length;i++) {
				antenasOverlay[i].setMap(null);
			}	
			antenasOverlay = [];
			loadedAntenas = [];
			operatorLayer.innerHTML = operator;
			logoLayer.innerHTML = '<img src="{APIURL}?operadora='+operator+'&mode=logo" width=72 height=72/>';
			selectedOperator = operator;
			pointLayer.redraw();
			loadAntena();
		}
	}
	function loadData(data) {
		for(var i=0;i<data["antenas"].length;i++) {
			if(!pointIdExistAnt(data["antenas"][i].id)) {
				loadedAntenas.push(data["antenas"][i]);
				var myLatLng = new google.maps.LatLng(data["antenas"][i].latitude, data["antenas"][i].longitude);
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
				
		var address = document.getElementById('search_address').value;
		geocoder.geocode( { 'address': address}, function(results, status) {
		  if (status == google.maps.GeocoderStatus.OK) {
			map.setCenter(results[0].geometry.location);
		  } else {
			alert("Não pudemos achar seu endereço!");
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
			var container = document.getElementById('operator_list');
			container.innerHTML = "";
			for(var i=0;i<data.data.length;i++) {
				var new_element = document.createElement('li');
				new_element.innerHTML = "<a href=\"javascript:void(0);\" onClick=\"changeOperator('"+data.data[i]+"');\">"+data.data[i]+"</a>";
				container.insertBefore(new_element, container.firstChild);	
			}		
		});
	}

	function initialize() {
		var myPoint;
		var myOptions = {zoom: 16,mapTypeId: google.maps.MapTypeId.ROADMAP};
		geocoder = new google.maps.Geocoder();
		infoDiv = document.getElementById("info");
		map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
		if(navigator.geolocation) {
				infoDiv.innerHTML = "Localização determinada por HTML5<BR>"; 
				navigator.geolocation.getCurrentPosition(function(position) {
				myPoint = new google.maps.LatLng(position.coords.latitude,position.coords.longitude);
				map.setCenter(myPoint);
			}, function() {
					infoDiv.innerHTML = "Localização padrão<BR>"; 
					center = new google.maps.LatLng(-23.54894330,-46.63881820);
					zoom = 16;
					map.setCenter(center);
				});
			}else{
				infoDiv.innerHTML = "Localização padrão<BR>"; 
				center = new google.maps.LatLng(-23.54894330,-46.63881820);
				zoom = 16;
				map.setCenter(center);
		}
		if(preaddress != "") {
		geocoder.geocode( { 'address': preaddress}, function(results, status) {
		  if (status == google.maps.GeocoderStatus.OK) 
			map.setCenter(results[0].geometry.location);
		  else
			alert("Não pudemos achar seu endereço!");
		});			
		}
		loadOperators();
		scaleLayer = document.createElement('div');
		scaleLayer.innerHTML = '<img src="{SITEURL}images/scale.png"/>';
		scaleLayer.style.opacity = '0.8';
		map.controls[google.maps.ControlPosition.TOP_CENTER].push(scaleLayer);
		
		operatorLayer = document.createElement('div');
		operatorLayer.innerHTML = selectedOperator==""?"Selecione a operadora":selectedOperator;
		operatorLayer.style.backgroundColor = 'white';
		operatorLayer.style.opacity = '0.8';
		operatorLayer.style.width = '200px';
		operatorLayer.style.textAlign = 'center';
		operatorLayer.style.borderRadius = '12px';
		map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(operatorLayer);
		
		logoLayer = document.createElement('div');
		logoLayer.innerHTML = '<img src="{APIURL}?operadora=none&mode=logo" width=72 height=72/>';
		logoLayer.style.opacity = '0.8';
		logoLayer.style.width = '120px';
		logoLayer.style.height = '80px';
		logoLayer.style.textAlign = 'center';
		map.controls[google.maps.ControlPosition.RIGHT_TOP].push(logoLayer);
		
        pointLayer = new mobilles.web.TileOverlay(function(x, y, z) {  return selectedOperator!=""?"{APIURL}?operadora="+selectedOperator+"&tile=" + z + "-" + x + "-" + y:""; },{'map': map, 'visible': true,'minZoom': 10,'maxZoom': 16, 'percentOpacity': 100} );
		google.maps.event.addListener(map, 'bounds_changed', function() {
			center = map.getCenter();
			zoom = map.getZoom();
			bounds = map.getBounds();
			loadAntena();
			//infoDiv.innerHTML = pointToTile(center, map.getZoom());
		});
	}
	function gotoPos(lat,lon) {
			myPoint = new google.maps.LatLng(lat,lon);
			map.setCenter(myPoint);	
	}
