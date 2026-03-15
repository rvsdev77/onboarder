# UI-Backend Connection Setup

## What Was Done

### 1. Backend (Spring Boot)
- **ChatRestController**: Created REST endpoint at `/api/chat`
  - Accepts: `{ "message": "your question" }`
  - Returns: `{ "response": "answer" }`
  - CORS enabled for development

- **ChatService**: Fixed to properly inject ChatClient.Builder

### 2. Frontend (Vite + TypeScript)
- Already configured with proxy to `http://localhost:8080`
- Sends POST requests to `/api/chat`
- Displays chat messages in real-time

## Running the Application

### Development Mode

1. **Start PostgreSQL**
   ```bash
   docker-compose up -d
   ```

2. **Start Backend** (from project root)
   ```bash
   export OPENAI_API_KEY=your-key
   ./gradlew bootRun
   ```
   Backend runs on: `http://localhost:8080`

3. **Start Frontend** (from ui folder)
   ```bash
   cd ui
   npm install
   npm run dev
   ```
   Frontend runs on: `http://localhost:5173`

4. **Open browser**: http://localhost:5173

### Production Mode

1. **Build UI**
   ```bash
   cd ui
   npm run build
   ```

2. **Copy to Spring Boot static resources**
   ```bash
   cp -r dist/* ../src/main/resources/static/
   ```

3. **Run Spring Boot**
   ```bash
   ./gradlew bootRun
   ```

4. **Access**: http://localhost:8080

## API Contract

**Endpoint**: `POST /api/chat`

**Request**:
```json
{
  "message": "What is the vacation policy?"
}
```

**Response**:
```json
{
  "response": "According to the company policy..."
}
```

## Files Modified/Created

- `src/main/kotlin/com/example/onboarder/chat/ChatRestController.kt` - REST endpoint
- `src/main/kotlin/com/example/onboarder/chat/ChatService.kt` - Fixed ChatClient injection
- `src/main/kotlin/com/example/onboarder/configuration/WebConfig.kt` - Static file serving
