var map = L.map('map').setView([51.505, -0.09], 13);

L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
	maxZoom: 19,
	attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);

document.getElementById('btn-get-location').onclick = function () {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(success, error);
	} else {
		alert('Geolocation is not supported by this browser.');
	}
};

let userPosition = { lat: 0, lon: 0 };

function success(position) {
	//var marker = L.marker([position.coords.latitude, position.coords.longitude]).addTo(map);
	var marker = L.marker([39.1628136, -76.891511]).addTo(map);
	map.flyTo([39.1628136, -76.891511], 13);
	//userPosition.lat = position.coords.latitude;
	//userPosition.lon = position.coords.longitude;
	userPosition.lat = 39.1628136;
	userPosition.lon = -76.891511;
}

function error() {
	alert('Unable to retrieve your location');
}

var paths = [{ points: [] }, { points: [] }];

var stops = {
	points: [],
};

document.getElementById('btn-get-routes').onclick = function () {
	if (userPosition.lat === 0 && userPosition.lon === 0) {
		alert('Please update your location first');
		return;
	}
	console.log('Searching best routes from', userPosition);
	search_best_routes(userPosition).then((data) => {
		console.log(data);
		console.log(data[0].paths[0]);

		data[0].paths.forEach((path) => {
			const latlngs = path.points.map((p) => [p.lat, p.lon]);
			var polyline = L.polyline(latlngs, { color: '#' + data[0].color, weight: 3 }).addTo(map);
		});

		data[0].stops.forEach((stop) => {
			stops.points.push([stop.lat, stop.lon]);
			var stopMarker = L.circleMarker([stop.lat, stop.lon], { color: 'purple' }).addTo(map);
			stopMarker.bindPopup(`<b>${stop.tag}</b><br>Stop ID: ${stop.stopId}`);
		});
	});
};

async function search_best_routes(userPosition) {
	const response = await fetch('/api/bustrack/search', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify({
			latFrom: userPosition.lat,
			lonFrom: userPosition.lon,
			latTo: 39.159605,
			lonTo: -76.893621,
		}),
	});
	return response.json();
}
