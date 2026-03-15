# Onboarder UI

A simple chat interface built with TypeScript and Vite for the Onboarder RAG chatbot.

## Prerequisites

- Node.js (v18 or higher)
- npm (comes with Node.js)

## Installation

```bash
npm install
```

## Running the Application

### Development Mode

Start the development server with hot reload:

```bash
npm run dev
```

The UI will be available at `http://localhost:5173`

### Build for Production

Build the application for production:

```bash
npm run build
```

The built files will be in the `dist/` directory.

### Preview Production Build

Preview the production build locally:

```bash
npm run preview
```

## Backend Configuration

The UI expects the Spring Boot backend to be running on `http://localhost:8080`. The Vite dev server automatically proxies API requests to the backend.

Make sure the backend is running before using the chat interface.

## Features

- Real-time chat interface
- Message history display
- User and assistant message differentiation
- Responsive design
- Keyboard shortcuts (Enter to send)
