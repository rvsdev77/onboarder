# Onboarder

AI-powered onboarding assistant using Spring AI with RAG (Retrieval Augmented Generation).

## Features

- **Vector Store Auto-Initialization**: Automatically populates the vector store on first startup
- **Smart Duplicate Prevention**: Skips re-initialization on subsequent startups using database flag
- **RAG-based Chat**: Answers onboarding questions using company policy documents
- **PgVector Integration**: PostgreSQL with vector extension for semantic search
- **Feedback System**: Collect user feedback (thumbs up/down) to measure chat accuracy and relevance
- **Success Metrics**: Track response quality with aggregated statistics and success rates
- **Interactive UI**: Web-based chat interface with real-time feedback buttons

## Prerequisites

- Docker & Docker Compose
- OpenAI API Key

## Quick Start

1. **Set OpenAI API Key**
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. **Run the full stack**
   ```bash
   docker-compose up --build
   ```

   Open http://localhost:3000 in your browser.

On first startup, the application will:
- Create the `vector_store_init_status` table
- Load and process `company_policy.pdf` from resources
- Populate the vector store with document embeddings
- Mark initialization as complete

On subsequent startups, it will skip the initialization process.

## Architecture

### Services

| Service    | Description                          | Port |
|------------|--------------------------------------|------|
| `postgres` | PostgreSQL with PgVector extension   | —    |
| `backend`  | Spring Boot API                      | 8080 |
| `frontend` | Vite/TypeScript UI served via nginx  | 3000 |

### Components

- **VectorStoreStartupInitializer**: Runs on startup, checks initialization status
- **DocumentIndexer**: Orchestrates document ingestion pipeline
- **DocumentReader**: Reads PDF documents
- **ContentSplitter**: Chunks documents into smaller pieces
- **KeywordEnricher**: Enriches chunks with AI-generated keywords
- **VectorStore**: Stores document embeddings (PgVector)

### Ingestion Pipeline

```
PDF Document → Read → Split → Enrich → Embed → Store
```

### Database Tables

- `vector_store_init_status`: Tracks vector store initialization
  - `id`: Primary key (always "default")
  - `is_initialized`: Boolean flag
  - `last_initialized_at`: Timestamp of last initialization

- `chat_feedback`: Stores user feedback on chat responses
  - `id`: Primary key (UUID)
  - `response_id`: Links feedback to specific response
  - `question`: User's original question
  - `response`: AI-generated answer
  - `rating`: Boolean (true = thumbs up, false = thumbs down)
  - `comment`: Optional text feedback
  - `timestamp`: When feedback was submitted

## Configuration

### Vector Store Initialization

Configure in `src/main/resources/application.yml`:

```yaml
onboarder:
  vectorstore:
    force-reinit: false  # Set to true to force re-initialization
```

### Force Re-initialization

**Option 1**: Set configuration property
```yaml
onboarder:
  vectorstore:
    force-reinit: true
```

**Option 2**: Delete database record
```sql
DELETE FROM vector_store_init_status;
```

Then restart the containers:
```bash
docker-compose restart backend
```

## API Endpoints

### Chat
```http
POST /api/chat
Content-Type: application/json

{
  "message": "What is the vacation policy?"
}
```

**Response:**
```json
{
  "response": "Based on the company policy, AeroSpire Technologies provides...",
  "responseId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Debug Vector Search
```http
GET /api/chat/debug?question=What is the PTO policy?
```

### Submit Feedback
```http
POST /api/chat/feedback
Content-Type: application/json

{
  "responseId": "550e8400-e29b-41d4-a716-446655440000",
  "rating": true,
  "comment": "Very helpful!"
}
```

### Get Feedback Metrics
```http
GET /api/chat/metrics
```

## Development

### Running Locally (without Docker)

**Prerequisites**: Java 22, Node.js 20

1. **Start PostgreSQL**
   ```bash
   docker-compose up postgres -d
   ```

2. **Run backend**
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ./gradlew bootRun
   ```

3. **Run frontend**
   ```bash
   cd ui
   npm install
   npm run dev
   ```
   Open http://localhost:5173

### Project Structure
```
src/main/kotlin/com/example/onboarder/
├── chat/              # Chat API and service
├── configuration/     # Spring configuration
├── feedback/          # Feedback collection and metrics
├── ingestion/         # Document ingestion pipeline
└── OnboarderApplication.kt
ui/                    # Vite/TypeScript frontend
├── src/
├── Dockerfile
└── nginx.conf
Dockerfile             # Backend Docker image
docker-compose.yml     # Full stack orchestration
```

### Adding New Documents

1. Place PDF files in `src/main/resources/rag/`
2. Update `DocumentIndexer.kt` to reference new files
3. Force re-initialization or delete the status record
4. Rebuild and restart: `docker-compose up --build backend`

## Technology Stack

- **Spring Boot 3.4.1**
- **Spring AI 1.0.0-M5**
- **Kotlin 2.1.21**
- **PostgreSQL + PgVector**
- **OpenAI GPT-4o-mini**
- **Vite + TypeScript**
- **nginx**
- **Docker & Docker Compose**
