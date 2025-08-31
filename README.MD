# Real-Time Vehicle Tracker 🚌

🎯 Application Goal
The main goal of our application is to explore modern technologies within the Spring ecosystem to create a real-time vehicle tracking service. The system identifies a user's location from their IP address, searches for nearby public transit agencies, and will eventually display the real-time location of their vehicles.

This project serves as a practical learning ground for the following concepts:

Reactive and Non-Blocking Programming with Spring WebClient.

Real-Time Communication with Spring WebSockets.

Handling Data in Different Formats (JSON and XML).

Integrating with External APIs for geolocation and transit data.

Best Practices in Java Architecture with Spring Boot.

🛠️ How It Works: The Data Flow
We designed our application's data flow to be efficient and scalable. Here is the step-by-step process:

Capture User IP: As soon as a request hits our endpoint, the system extracts the user's IP address. In a production environment, this IP often comes from the x-forwarded-for header, as the application usually runs behind a proxy or load balancer.

Geolocation: With the IP address, we make a call to an external API (like ip-api.com) to get the user's geolocation, including their latitude and longitude.

Search for Transit Agencies: Next, we use the NextBus API, which provides public transportation data. The first step is to query the list of available transit agencies (agencyList).

Data Processing: The NextBus API returns data in XML format. Our application converts this response into Java objects so we can easily work with them.

(Next Steps) Find Nearby Vehicles: With the user's location and the list of agencies, the next step would be to implement the logic to cross-reference this information and find the routes and vehicles closest to the user.

(Next Steps) Real-Time Push: Finally, the plan is to use WebSockets to continuously send vehicle location updates to the client, without requiring the client to make new requests.
