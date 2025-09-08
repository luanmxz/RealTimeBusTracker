let userPosition = { lat: 0, lon: 0 };
let paths = [{ points: [] }, { points: [] }];
let stops = {
	points: [],
};
let evtSource = null;
let map = L.map('map').setView([51.505, -0.09], 13);
let routesLayerGroup = L.layerGroup().addTo(map);
let isMonitoring = false;

let bntCloseConnection = document.getElementById('btn-close-connection');

L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
	maxZoom: 19,
	attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);

document.getElementById('btn-close-connection').onclick = function () {
	if (evtSource != null) {
		closeEventSource(evtSource);
	}
};

function error() {
	alert('Unable to retrieve your location');
}

window.onload = function () {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(success, error);
	} else {
		alert('Geolocation is not supported by this browser.');
	}
};

function success(position) {
	//var marker = L.marker([position.coords.latitude, position.coords.longitude]).addTo(map);
	var marker = L.marker([39.162954, -76.90073]).addTo(map);
	map.flyTo([39.162954, -76.90073], 13);
	//userPosition.lat = position.coords.latitude;
	//userPosition.lon = position.coords.longitude;
	userPosition.lat = 39.1628136;
	userPosition.lon = -76.891511;

	getAgencies().then((data) => {
		console.log(data);
		var agencySelect = document.getElementById('agency-select');

		data.agencies.forEach((agency) => {
			var option = document.createElement('option');
			option.value = agency.tag;
			option.text = agency.title;
			agencySelect.appendChild(option);

			agencySelect.onchange = function () {
				let overlay = document.getElementById('map-overlay');
				overlay.style.display = 'flex';

				if (evtSource != null) {
					closeEventSource(evtSource);
				}
				routesLayerGroup.clearLayers();

				bntCloseConnection.disabled = false;
				bntCloseConnection.style.cssText = 'color: green';
				bntCloseConnection.innerText = `Monitoring ${
					agencySelect.options[agencySelect.selectedIndex].text
				} routes - Click to Stop`;
				isMonitoring = true;

				evtSource = getAgencyRoutes(agencySelect.value);

				evtSource.onopen = function () {
					overlay.style.display = 'none';
				};
			};
		});
	});
}

async function getAgencies() {
	const response = await fetch('api/bustrack/agencies', {
		method: 'GET',
		headers: {
			'Content-Type': 'application/json',
		},
	});

	return response.json();
}

function getAgencyRoutes(agencyTag) {
	const endpoint = `/api/bustrack/routes?agency=${agencyTag}`;
	let es = new EventSource(endpoint);

	es.onmessage = function (event) {
		const route = JSON.parse(event.data);
		console.log('Received data:', route);

		route.paths.forEach((path) => {
			const latlngs = path.points.map((p) => [p.lat, p.lon]);
			L.polyline(latlngs, { color: `#${route.color}`, dashArray: '5, 5' }).addTo(routesLayerGroup);
		}),
			route.stops.forEach((stop) => {
				let icon = L.icon({
					iconUrl: '../img/bus-stop.png',
					iconSize: [20, 20],
				});

				let busStopMarker = L.marker([stop.lat, stop.lon], { icon: icon }).addTo(routesLayerGroup);
				busStopMarker.bindPopup(`<b>${stop.title}</b><br>Stop ID: ${stop.stopId}`);
			});

		if (route.vehicles != null && route.vehicles.length > 0) {
			route.vehicles.forEach((v) => {
				let icon = L.icon({
					iconUrl: '../img/bus.png',
					iconSize: [70, 70],
				});

				vehicle = L.marker([v.lat, v.lon], { icon: icon }).addTo(routesLayerGroup);
				vehicle.bindPopup(
					`<b>Route: ${v.routeTag}</b><br>Vehicle ID: ${v.id}<br>Speed: ${v.speedKmHr} km/h<br>Heading: ${v.heading}°`
				);
			});
		}

		routeBound = new L.LatLngBounds([
			[route.latMax, route.lonMax],
			[route.latMin, route.lonMin],
		]);
		//map.fitBounds(routeBound, { padding: [200, 200] });
	};
	return es;
}

function closeEventSource(evtSource) {
	evtSource.close();
	console.log('SSE connection closed');
}

bntCloseConnection.addEventListener('click', () => {
	if (isMonitoring) {
		bntCloseConnection.disabled = true;
		bntCloseConnection.style.cssText = 'color: grey';
		bntCloseConnection.innerText = 'Close Connection';
		isMonitoring = false;
	}
});
