# Minecraft Stats Web

Web application for displaying Minecraft server player statistics with **real-time updates**.

Built with **Kotlin**, **Spring Boot 3.4**, and **Gradle Kotlin DSL**.

## 🌟 Features

### Dashboard
- 📊 **Server Statistics** - Total blocks mined, mobs killed, play time, deaths, and more
- 🏆 **Leaderboards** - Rankings for various statistics (blocks, mobs, time, deaths, distance)
- 👤 **Player Profiles** - Detailed stats and achievements for each player
- 🏅 **Advancements** - Track player achievements with progress bar

### Real-Time
- ⚡ **WebSocket Live Updates** - See events as they happen
- 💬 **Live Chat Feed** - Server chat messages in real-time
- 🟢 **Online Players** - See who's currently playing
- 📜 **Event Log** - Joins, leaves, deaths, and advancements

### Technical
- 🔄 **Smart Caching** - 5-minute cache with Caffeine
- 📱 **Responsive Design** - Works on desktop and mobile
- 🔌 **REST API** - JSON endpoints for programmatic access
- 🔐 **RCON Support** - Query server status via RCON protocol

## Tech Stack

- **Language**: Kotlin 1.9
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Gradle with Kotlin DSL
- **Template Engine**: Thymeleaf
- **Real-Time**: WebSocket + STOMP
- **Caching**: Caffeine
- **Styling**: Tailwind CSS (via CDN)
- **Icons**: Font Awesome

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `MINECRAFT_STATS_PATH` | `/data/world/stats` | Path to Minecraft stats JSON files |
| `MINECRAFT_USER_CACHE_PATH` | `/data/usercache.json` | Path to player name cache |
| `MINECRAFT_SERVER_NAME` | `Minecraft Server` | Server name displayed in UI |

## API Endpoints

### Web Pages
| Endpoint | Description |
|----------|-------------|
| `GET /` | Web dashboard with live updates |
| `GET /player/{uuid}` | Player detail page with advancements |

### REST API
| Endpoint | Description |
|----------|-------------|
| `GET /api/stats` | Server stats JSON |
| `GET /api/stats/player/{uuid}` | Player stats JSON |
| `GET /api/status` | Server status (online players) |
| `GET /api/events` | Recent server events |
| `GET /api/chat` | Recent chat messages |
| `GET /api/advancements/{uuid}` | Player advancements |
| `GET /actuator/health` | Health check |

### WebSocket
| Endpoint | Description |
|----------|-------------|
| `/ws` | STOMP WebSocket endpoint |
| `/topic/status` | Server status updates (every 30s) |
| `/topic/events` | New events (every 10s) |
| `/topic/stats` | Stats updates (every 5m) |

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
