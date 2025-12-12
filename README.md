# Minecraft Stats Web

Web application for displaying Minecraft server player statistics.

Built with **Kotlin**, **Spring Boot 3.4**, and **Gradle Kotlin DSL**.

## Features

- 📊 **Server Statistics Dashboard** - Total blocks mined, mobs killed, play time, and more
- 🏆 **Leaderboards** - Rankings for various statistics
- 👤 **Player Profiles** - Detailed stats for each player
- 🔄 **Auto-refresh** - Statistics cached for 5 minutes
- 📱 **Responsive Design** - Works on desktop and mobile
- 🔌 **REST API** - JSON endpoints for programmatic access

## Tech Stack

- **Language**: Kotlin 1.9
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Gradle with Kotlin DSL
- **Template Engine**: Thymeleaf
- **Caching**: Caffeine
- **Styling**: Tailwind CSS (via CDN)

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `MINECRAFT_STATS_PATH` | `/data/world/stats` | Path to Minecraft stats JSON files |
| `MINECRAFT_USER_CACHE_PATH` | `/data/usercache.json` | Path to player name cache |
| `MINECRAFT_SERVER_NAME` | `Minecraft Server` | Server name displayed in UI |

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /` | Web dashboard |
| `GET /player/{uuid}` | Player detail page |
| `GET /api/stats` | Server stats JSON |
| `GET /api/stats/player/{uuid}` | Player stats JSON |
| `GET /actuator/health` | Health check |

## Building

```bash
./gradlew bootJar
```

## Docker

```bash
docker build -t minecraft-stats-web .
docker run -p 8080:8080 -v /path/to/minecraft:/minecraft-data:ro minecraft-stats-web
```

## Kubernetes Deployment

```bash
kubectl apply -f k8s/
```

## Development

```bash
./gradlew bootRun
```

## License

MIT
