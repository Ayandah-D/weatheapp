
<img width="1266" height="898" alt="UI" src="https://github.com/user-attachments/assets/10c7c410-e3ce-4199-9d9b-d4701c85646d" />


## Tech Stack 
Front-end: React. js 
Back-end:  Springboot
Batabase:  MongoDB
WeatherAPI : Open-meteo https://open-meteo.com

## How to Run Project 

## Docker 

**Prerequisites:** Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)

**Steps:**

1. Download the ZIP File to local machine  from Github
2. Unzip it, then open a terminal in the `weather-api-backend/` folder
3. Run:


```shellscript
docker-compose up --build
```

This starts both **MongoDB** on port `27017` and the **Spring Boot API** on port `8080`. The database is automatically initialized with indexes and seed data.

- API: `http://localhost:8080/api/...`
- Swagger UI: `http://localhost:8080/swagger-ui.html`



The API will be live at `http://localhost:8080`. The key endpoints are:

| Method | Endpoint | Description
|-----|-----|-----
| `GET` | `/api/locations` | List all tracked locations
| `POST` | `/api/locations` | Add a new city
| `DELETE` | `/api/locations/{id}` | Remove a location
| `GET` | `/api/weather/{locationId}/current` | Current weather for a city
| `GET` | `/api/weather/{locationId}/forecast` | 7-day forecast
| `POST` | `/api/sync/trigger` | Force sync all locations
| `GET` | `/api/preferences/default` | Get user preferences

