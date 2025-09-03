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

function success(position) {
	var marker = L.marker([position.coords.latitude, position.coords.longitude]).addTo(map);
	map.flyTo([position.coords.latitude, position.coords.longitude], 13);
}

function error() {
	alert('Unable to retrieve your location');
}
