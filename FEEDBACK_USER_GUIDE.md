# Feedback Feature User Guide

## Overview

The chat interface now includes an interactive feedback system that allows users to rate responses with thumbs up 👍 or thumbs down 👎.

## How It Works

### 1. User Experience Flow

```
User asks question
    ↓
AI responds with answer
    ↓
Feedback buttons appear (👍 👎)
    ↓
User clicks a button
    ↓
Optional comment field appears
    ↓
User can:
  - Add comment and click "Submit"
  - Click "Submit" without comment
  - Click "Cancel" to go back
    ↓
"Thank you for your feedback!" message appears
```

### 2. UI Components

**Feedback Buttons**
- Located at the bottom-right of each AI response
- Two buttons: 👍 (helpful) and 👎 (not helpful)
- Hover effect: slight scale animation
- Click to proceed to comment section

**Comment Section** (Optional)
- Appears after clicking thumbs up/down
- Textarea with 1000 character limit
- Placeholder: "Optional: Tell us more..."
- Two action buttons:
  - **Submit**: Send feedback (with or without comment)
  - **Cancel**: Return to thumbs up/down buttons

**Confirmation Message**
- Replaces entire feedback section after submission
- Shows the selected emoji + "Thank you for your feedback!"
- Green color for positive feedback
- Prevents duplicate submissions

### 3. Technical Details

**Frontend (TypeScript)**
- Captures `responseId` from chat API response
- Sends feedback to `POST /api/chat/feedback`
- Handles success/error states
- Disables buttons after submission

**Backend (Kotlin)**
- Stores feedback in `chat_feedback` table
- Links feedback to original question/response
- Calculates success metrics
- Provides aggregated statistics via `/api/chat/metrics`

## What is recentComments?

`recentComments` in the `/api/chat/metrics` response shows the **last 5 feedback submissions that included text comments**.

**Example Response:**
```json
{
  "totalResponses": 25,
  "positiveCount": 20,
  "negativeCount": 5,
  "successRate": 80.0,
  "recentComments": [
    {
      "rating": true,
      "comment": "Very clear explanation of the policy!",
      "timestamp": "2024-01-15T10:30:00Z"
    },
    {
      "rating": false,
      "comment": "Could use more specific examples",
      "timestamp": "2024-01-15T09:15:00Z"
    }
  ]
}
```

**Notes:**
- Only feedback **with comments** appears here
- Feedback without comments (just thumbs up/down) is counted in metrics but not shown in `recentComments`
- Limited to 5 most recent comments
- Ordered by timestamp (newest first)

## Testing the Feature

### Start the Application

1. **Backend**:
   ```bash
   ./gradlew bootRun
   ```

2. **Frontend**:
   ```bash
   cd ui
   npm install
   npm run dev
   ```

3. **Open Browser**: http://localhost:5173

### Test Feedback Flow

1. Ask a question: "What is the vacation policy?"
2. Wait for AI response
3. Click 👍 or 👎 button
4. Verify "Thank you for your feedback!" appears
5. Check metrics: `GET http://localhost:8080/api/chat/metrics`

### View Metrics

**Via API**:
```bash
curl http://localhost:8080/api/chat/metrics
```

**Response**:
```json
{
  "totalResponses": 10,
  "positiveCount": 8,
  "negativeCount": 2,
  "successRate": 80.0,
  "recentComments": []
}
```

## Styling

The feedback buttons are designed to be:
- **Subtle**: Transparent background, light border
- **Responsive**: Hover effects and scale animation
- **Accessible**: Clear visual feedback on interaction
- **Non-intrusive**: Small size, positioned at bottom-right

## Future Enhancements

Potential improvements:
- Add comment field for detailed feedback
- Show feedback statistics in UI
- Add "Report issue" option
- Track feedback trends over time
- A/B test different response strategies

## Troubleshooting

**Buttons don't appear**:
- Check that backend is returning `responseId` in response
- Verify API is running on http://localhost:8080
- Check browser console for errors

**Feedback not saving**:
- Verify database connection
- Check `chat_feedback` table exists
- Review backend logs for errors

**Metrics show 0**:
- Submit some feedback first
- Verify feedback API calls are successful
- Check database has records in `chat_feedback` table
