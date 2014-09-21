var TILE_SIZE = 256;
var TILE_INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / TILE_SIZE;
var TILE_ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;
var IE6 = /MSIE 6/i.test(navigator.userAgent);
var TILE_CSS_TEXT = /webkit/i.test(navigator.userAgent) ?   "-webkit-user-select:none;" :   (/Gecko[\/]/i.test(navigator.userAgent) ? "-moz-user-select:none;" : "");
var TILE_TRANSPARENT = "http://maps.gstatic.com/intl/en_us/mapfiles/transparent.png";

function FromTileCoordinatesToLatLng(coords, zoom) {
    //Tile Coords to Meters
    var res = TILE_INITIAL_RESOLUTION / Math.pow(2, zoom);
    var mx = (coords.x * TILE_SIZE) * res - TILE_ORIGIN_SHIFT;
    var my = ((Math.pow(2, zoom) - coords.y) * TILE_SIZE) *
        res - TILE_ORIGIN_SHIFT;

    //Meters to LatLng
    var lng = (mx / TILE_ORIGIN_SHIFT) * 180.0;
    var lat = 180 / Math.PI * (2 * Math.atan( Math.exp(((my / TILE_ORIGIN_SHIFT) * 180.0) * Math.PI / 180.0)) - Math.PI / 2.0);

    return new google.maps.LatLng(lat, lng);
}

function FromLatLngToTileCoordinates(latLng, zoom) {
    //LatLng to Meters
    var mx = latLng.lng() * TILE_ORIGIN_SHIFT / 180.0;
    var my = (Math.log(Math.tan((90 + latLng.lat()) * Math.PI / 360.0))    / (Math.PI / 180.0)) * TILE_ORIGIN_SHIFT / 180.0;

    //Meters to Pixels
    var res = TILE_INITIAL_RESOLUTION / Math.pow(2, zoom);
    var px = (mx + TILE_ORIGIN_SHIFT) / res;
    var py = (my + TILE_ORIGIN_SHIFT) / res;

    //Pixels to Tile Coords
    var tx = Math.floor(Math.ceil(px / TILE_SIZE) - 1);
    var ty = Math.pow(2, zoom) - 1 - Math.floor(Math.ceil(py / TILE_SIZE) - 1);

    return new google.maps.Point(tx, ty);
}

function TileEvents() {
    this.overlays = {};
    this.zoomListener = null;
    this.idleListener = null,
    this.viewportBounds = null;
    this.viewportTileBounds = [null, null];
    this.viewportPixelBounds = [null, null];
    this.tileCoords = [null, null];
    this.overlayIndex = 0;
}

function MapEventInfo(map, tileEvents) {
    this.map = map;
    this.tileEvents = tileEvents;
}

MapEventInfo.prototype.map = null;
MapEventInfo.prototype.tileEvents = null;
var MapList = [];

TileEvents.prototype.addOverlay = function (newTileOverlay) {
    var thisIndex = ("_" + this.overlayIndex);
    this.overlayIndex++;
    this.overlays[thisIndex] = newTileOverlay;
    if (!this.idleListener)
        google.maps.event.addListener(newTileOverlay.settings.map, "idle",
            function () {
                var MapTileEvents = null;
                    
                for (var i = 0; i < MapList.length; i++) {
                    if(MapList[i].map === this) {
                        MapTileEvents = MapList[i].tileEvents;
                        break;
                    }
                }

                MapTileEvents.viewportPixelBounds = [null, null];

                var z = this.getZoom();
                var viewportBounds = this.getBounds();
                var viewportTileCoordsNorthEast =
                    FromLatLngToTileCoordinates(viewportBounds.getNorthEast(),
                        z);
                var viewportTileCoordsSouthWest =
                    FromLatLngToTileCoordinates(viewportBounds.getSouthWest(),
                        z);
                viewportBounds =
                    new google.maps.LatLngBounds(
                        FromTileCoordinatesToLatLng(
                            new google.maps.Point(
                                viewportTileCoordsSouthWest.x,
                                viewportTileCoordsSouthWest.y), z),
                        FromTileCoordinatesToLatLng(
                            new google.maps.Point(
                                viewportTileCoordsNorthEast.x,
                                viewportTileCoordsNorthEast.y), z));
            
                MapTileEvents.viewportBounds = viewportBounds;
                MapTileEvents.tileCoords = [viewportTileCoordsSouthWest,
                    viewportTileCoordsNorthEast];
                
                for (var overlay in MapTileEvents.overlays) {
                    if (overlay) {
                        var proj =
                            MapTileEvents.overlays[overlay].getProjection();
                        
                        if (proj) {
                            var viewportNorthEastPixel =
                                proj.fromLatLngToDivPixel(
                                    viewportBounds.getNorthEast());

                            var viewportSouthWestPixel =
                                proj.fromLatLngToDivPixel(
                                    viewportBounds.getSouthWest());
                            MapTileEvents.viewportPixelBounds = [
                                viewportSouthWestPixel,
                                viewportNorthEastPixel];
                        }
                    }
                }

            for (var overlay in MapTileEvents.overlays) {
                if (overlay)
                    MapTileEvents.overlays[overlay].redraw();
            }
        });
    
    google.maps.event.trigger(newTileOverlay.settings.map, "idle");

    if (!this.zoomListener)
        google.maps.event.addListener(newTileOverlay.settings.map,
            "zoom_changed", function () {
                var MapTileEvents = null;

                for (var i = 0; i < MapList.length; i++) {
                    if (MapList[i].map === this) {
                        MapTileEvents = MapList[i].tileEvents;
                        break;
                    }
                }
                
                for (var overlay in MapTileEvents.overlays) {
                    if (overlay)
                        MapTileEvents.overlays[overlay].removeAllTiles();
                }
            });

    return thisIndex;
}
TileEvents.prototype.removeOverlay = function (index) {
    if (!index)
        return;

    if (this.overlays[index])
        delete this.overlays[index];
    var hasOverlays = false;
    for (var overlay in this.overlays) {
        if (overlay) {
            hasOverlays = true;
            break;
        }
    }
    if (!hasOverlays) {
        if (this.zoomListener)
            google.maps.event.removeListener(this.zoomListener);
        if (this.idleListener)
            google.maps.event.removeListener(this.idleListener);

        this.idleListener = null;
        this.zoomListener = null;
    }
}

function TileOverlaySettings (bounds, minZoom, maxZoom) {
    this.getTileUrl = null;

    this.map = null;
    this.visible = true;
    this.mapTypes = null;
    this.div_ = null;
    this.divIEFix_ = null;
    /**
    * @const
    * @type {google.maps.LatLngBounds}
    */
    this.BOUNDS = bounds;

    /** @const */
    this.MIN_ZOOM = minZoom;
    /** @const */
    this.MAX_ZOOM = maxZoom;

    this.percentOpacity = 100;

    //For the current zoom level, keep track of which tiles have
    //already been drawn
    /** @type {Array.<Array.<Element>>} */
    this.tilesDrawn = [];

    /** @type {google.maps.LatLngBounds} */
    this.drawnBounds = null;

    /** @type {String} */
    this.overlayIndex = null;
}

function TileOverlay(GetTileUrl, TileOverlayOptions) {
    var minZoom = TileOverlayOptions["minZoom"] || 1, maxZoom = 19,
        bounds = null;

    if (TileOverlayOptions) {
        minZoom = TileOverlayOptions["minZoom"] || minZoom;
        maxZoom = TileOverlayOptions["maxZoom"] || maxZoom;
        if (TileOverlayOptions["bounds"])
            bounds = TileOverlayOptions["bounds"];
    }

    this.settings = new TileOverlaySettings(bounds, minZoom, maxZoom);

    this.settings.getTileUrl = GetTileUrl;

    if (TileOverlayOptions) {
        if(TileOverlayOptions["map"])
            this.settings.map = TileOverlayOptions["map"];
        
        if(TileOverlayOptions["visible"])
            this.settings.visible = TileOverlayOptions["visible"];
        
        if(TileOverlayOptions["mapTypes"])
            this.settings.mapTypes = TileOverlayOptions["mapTypes"];
        
        if(TileOverlayOptions["percentOpacity"])
            this.settings.percentOpacity =
                TileOverlayOptions["percentOpacity"];
    }

    if (this.settings.map)
        this.setMap(this.settings.map);
}
TileOverlay.prototype = new google.maps.OverlayView;

/** @type {TileEvents} */
TileOverlay.prototype.TileEvents = null;

/** @type {TileOverlaySettings} */
TileOverlay.prototype.settings = null;

/**
 * Cleanup drawn tiles
 * @return {undefined}
 */
TileOverlay.prototype.removeAllTiles = function () {
    if (!this.settings.div_)
        return;

    while (this.settings.div_.childNodes.length > 0) {
        if (IE6)
            this.settings.div_.childNodes[0].removeChild(
                this.settings.div_.childNodes[0].childNodes[0]);
        this.settings.div_.removeChild(this.settings.div_.childNodes[0]);
    }

    this.settings.tilesDrawn = [];
    this.settings.drawnBounds = null;
}

TileOverlay.prototype["draw"] = function () { }

/** @returns {undefined} */
TileOverlay.prototype.redraw = function () {
    var mapTypeId = this.settings.map.getMapTypeId();
    if (this.settings.mapTypes) {
        var matchingType = false;
        for (var i = 0; i < this.settings.mapTypes.length; i++) {
            if (this.settings.mapTypes[i] == mapTypeId) {
                matchingType = true;
                break;
            }
        }
        if (!matchingType) {
            this.removeAllTiles();
            return;
        }
    }

    if (!this.settings.div_)
        return;

    var z = this.settings.map.getZoom();


    //Calculate the boundaries for the tiles which overlap the viewport
    var viewportBounds = this.TileEvents.viewportBounds;
    if (!viewportBounds)
        return;

    var viewportTileCoordsNorthEast = this.TileEvents.tileCoords[1];
    var viewportTileCoordsSouthWest = this.TileEvents.tileCoords[0];

    //Calculate the boundaries for the tiles at this zoom level
    var TileCoordsNorthEast = viewportTileCoordsNorthEast;
    var TileCoordsSouthWest = viewportTileCoordsSouthWest;

    if (this.settings.BOUNDS) {
        TileCoordsNorthEast =
            FromLatLngToTileCoordinates(
                this.settings.BOUNDS.getNorthEast(), z);
        TileCoordsSouthWest = FromLatLngToTileCoordinates(
            this.settings.BOUNDS.getSouthWest(), z);
    }
    var TileLatLngBoundsForZoom =
        new google.maps.LatLngBounds(
            FromTileCoordinatesToLatLng(
                new google.maps.Point(TileCoordsSouthWest.x,
                    TileCoordsSouthWest.y), z),
            FromTileCoordinatesToLatLng(
                new google.maps.Point(TileCoordsNorthEast.x,
                    TileCoordsNorthEast.y), z));

    //Check to see if there are any tiles defined for this zoom level and
    // if they fall within the viewport
    if (z < this.settings.MIN_ZOOM || z > this.settings.MAX_ZOOM ||
        !viewportBounds.intersects(TileLatLngBoundsForZoom))
        return this.removeAllTiles();

    //If tiles previously drawn are now all out of the viewport, start over
    if (this.settings.drawnBounds &&
        !viewportBounds.intersects(this.settings.drawnBounds))
        this.removeAllTiles();

    //Some of the tiles are still displayed.
    //Loop through all the previously drawn tiles and remove those no longer
    //within the viewport
    else if (this.settings.drawnBounds) {
        var drawnNorthEast =
            FromLatLngToTileCoordinates(viewportBounds.getNorthEast(), z);
        var drawnSouthWest =
            FromLatLngToTileCoordinates(viewportBounds.getSouthWest(), z);

        for (var x = drawnNorthEast.x; x <= drawnSouthWest.x; x++)
            for (var y = drawnSouthWest.y; y <= drawnNorthEast.y; y++)
                if (x < viewportTileCoordsNorthEast.x ||
                    x > viewportTileCoordsSouthWest.x ||
                    y < viewportTileCoordsSouthWest.y ||
                    y > viewportTileCoordsNorthEast.y) {
                    this.settings.div_.removeChild(
                        this.settings.tilesDrawn["_" + x]["_" + y]);
                    delete this.settings.tilesDrawn["_" + x]["_" + y];
                }
    }
    this.settings.drawnBounds = viewportBounds;

    var viewportNorthEastPixel = this.TileEvents.viewportPixelBounds[1];
    var viewportSouthWestPixel = this.TileEvents.viewportPixelBounds[0];

    //Loop through all of the possible viewport tiles and
    //see if we need to draw new tiles
    for (var x = viewportTileCoordsSouthWest.x;
        x <= viewportTileCoordsNorthEast.x;
        x++) {
        for (var y = viewportTileCoordsNorthEast.y;
            y <= viewportTileCoordsSouthWest.y;
            y++) {
            //Check to see if this is a valid tile for this overlay,
            //and that we haven't already drawn it.
            if (x >= TileCoordsSouthWest.x && x <= TileCoordsNorthEast.x
                && y >= TileCoordsNorthEast.y && y <= TileCoordsSouthWest.y &&
                (!this.settings.tilesDrawn["_" + x] ||
                !this.settings.tilesDrawn["_" + x]["_" + y])) {

                var img = document.createElement("IMG");
                google.maps.event.addDomListenerOnce(img, "error",
                    function () { this.src = TILE_TRANSPARENT; });
                google.maps.event.addDomListenerOnce(img, "load",
                    function () {
                        google.maps.event.clearListeners(this, "error"); });
                
                var imgSrc = this.settings.getTileUrl(x, y, z);
                img.style.cssText = "position:absolute;left:" +
                    (viewportSouthWestPixel.x +
                    ((x - viewportTileCoordsSouthWest.x) * TILE_SIZE)) +
                    "px;top:" +
                    (viewportNorthEastPixel.y +
                    ((y - viewportTileCoordsNorthEast.y) * TILE_SIZE)) +
                    "px;width:" + TILE_SIZE + "px;height:" + TILE_SIZE + "px;"
                    + TILE_CSS_TEXT;

                img.alt = "";

                if (IE6) {
                    var div = document.createElement("DIV");
                    div.appendChild(img);
                    div.style.cssText = "position:absolute;left:" +
                        (viewportSouthWestPixel.x +
                        ((x - viewportTileCoordsSouthWest.x) * TILE_SIZE)) +
                        "px;top:" +
                        (viewportNorthEastPixel.y +
                        ((y - viewportTileCoordsNorthEast.y) * TILE_SIZE)) +
                        "px;width:" + TILE_SIZE + "px;height:" + TILE_SIZE +
                        "px;";

                    img.style.cssText = "zoom:1;" + TILE_CSS_TEXT;

                    if (imgSrc.substr(imgSrc.length - 4).toLowerCase() ==
                        ".png") {
                        img.style.filter =
                            "progid:DXImageTransform.Microsoft." +
                            "AlphaImageLoader(src='" +
                            this.settings.getTileUrl(x, y, z) +
                            "', sizingMethod='scale');";

                        img.src = TILE_TRANSPARENT;
                    }
                    else
                        img.src = imgSrc;

                    img.width = TILE_SIZE;
                    img.height = TILE_SIZE;

                    div.style.filter = this.settings.percentOpacity < 100 ?
                        "alpha(opacity=" + this.settings.percentOpacity + ")" :
                        "";
                    
                    img = div;
                }
                else if (typeof (this.settings.div_.style.filter) ==
                    "string") {
                    img.style.filter = this.settings.percentOpacity < 100 ?
                        "alpha(opacity=" + this.settings.percentOpacity + ")" :
                        "";
                    img.src = imgSrc;
                }
                else
                    img.src = imgSrc;

                this.settings.div_.appendChild(img);

                this.settings.tilesDrawn["_" + x] =
                    this.settings.tilesDrawn["_" + x] || [];

                this.settings.tilesDrawn["_" + x]["_" + y] = img;
            }
        }
    }
}

TileOverlay.prototype["setMap"] = function (Map) {
    if (Map == null) {
        if (this.TileEvents)
            this.TileEvents.removeOverlay(this.settings.overlayIndex);
    }
    google.maps.OverlayView.prototype.setMap.call(this, Map);
}

/**
* @override
* @this {TileOverlay}
*/
TileOverlay.prototype["onAdd"] = function () {
    this.settings.div_ = document.createElement("DIV");
    this.settings.div_.style.position = "relative";

    if (!this.settings.visible)
        this.settings.div_.style.display = "none";

    if (typeof (this.settings.div_.style.filter) != "string")
        this.setOpacity(this.settings.percentOpacity);

    this.getPanes().mapPane.appendChild(this.settings.div_);


    for (var i = 0; i < MapList.length; i++) {
        if (MapList[i].map === this.settings.map) {
            this.TileEvents = MapList[i].tileEvents;
            break;
        }
    }

    if (!this.TileEvents) {
        this.TileEvents = new TileEvents();
        MapList.push(new MapEventInfo(this.settings.map, this.TileEvents));
    }

    this.settings.overlayIndex = this.TileEvents.addOverlay(this);
}

/**
* @override
* @this {TileOverlay}
*/
TileOverlay.prototype["onRemove"] = function () {
    this.removeAllTiles();
    this.settings.div_.parentNode.removeChild(this.settings.div_);
    this.settings.div_ = null;
}

/**
* @this {TileOverlay}
* @returns {boolean}
*/
TileOverlay.prototype["getVisible"] = function () {
    return this.settings.visible;
}

/**
* @param {boolean} Visible
* @this {TileOverlay}
* @returns {boolean}
*/
TileOverlay.prototype["setVisible"] = function (Visible) {
    if (this.settings.div_) {
        if (Visible)
            this.settings.div_.style.display = "block";
        else
            this.settings.div_.style.display = "none";
    }

    this.settings.visible = Visible;
}

/**
* @param {number} percentOpacity
* @this {TileOverlay}
*/
TileOverlay.prototype.setOpacity = function (percentOpacity) {
    if (percentOpacity < 0)
        percentOpacity = 0;

    if (percentOpacity > 100)
        percentOpacity = 100;

    this.settings.percentOpacity = percentOpacity;

    if (!this.settings.div_)
        return;

    var opacity = percentOpacity / 100;

    if (typeof (this.settings.div_.style.filter) == "string") {
        for (var i = 0; i < this.settings.div_.childNodes.length; i++) {
            this.settings.div_.childNodes[i].style.filter =
                percentOpacity < 100 ?
                    "alpha(opacity=" + percentOpacity + ");" :
                    "";
        }
        return;
    }

    if (typeof (this.settings.div_.style.KHTMLOpacity) == "string")
        this.settings.div_.style.KHTMLOpacity = opacity;

    if (typeof (this.settings.div_.style.MozOpacity) == "string")
        this.settings.div_.style.MozOpacity = opacity;

    if (typeof (this.settings.div_.style.opacity) == "string")
        this.settings.div_.style.opacity = opacity;
}
TileOverlay.prototype["setOpacity"] = TileOverlay.prototype.setOpacity;

/**
* @this {TileOverlay}
* @returns {number}
*/
TileOverlay.prototype["getOpacity"] = function () {
    return this.settings.percentOpacity;
}

window["mobilles"] = window["mobilles"] || {};
window["mobilles"]["web"] = window["mobilles"]["web"] || {};
window["mobilles"]["web"]["TileOverlay"] = TileOverlay;
