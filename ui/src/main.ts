import './style.css';
import { marked } from 'marked';

interface Message {
  role: 'user' | 'assistant' | 'tool-request';
  content: string;
  responseId?: string;
  toolRequest?: {
    toolName: string;
    description: string;
    requestId: string;
  };
}

const messages: Message[] = [];
const promptHistory: string[] = [];
let historyIndex = -1;
const messagesContainer = document.getElementById('messages')!;
const promptInput = document.getElementById('prompt-input') as HTMLInputElement;
const sendButton = document.getElementById('send-button') as HTMLButtonElement;

function addMessage(message: Message) {
  messages.push(message);
  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${message.role}`;
  
  if (message.role === 'tool-request' && message.toolRequest) {
    messageDiv.innerHTML = `
      <div class="tool-request-container">
        <div class="tool-icon">🔧</div>
        <div class="tool-content">
          <div class="tool-title">Permission Required</div>
          <div class="tool-description">${message.toolRequest.description}</div>
          <div class="tool-actions">
            <button class="tool-approve" data-request-id="${message.toolRequest.requestId}">Allow</button>
            <button class="tool-deny" data-request-id="${message.toolRequest.requestId}">Deny</button>
          </div>
        </div>
      </div>
    `;
    
    const approveBtn = messageDiv.querySelector('.tool-approve') as HTMLButtonElement;
    const denyBtn = messageDiv.querySelector('.tool-deny') as HTMLButtonElement;
    
    approveBtn.addEventListener('click', () => handleToolApproval(message.toolRequest!.requestId, true, messageDiv));
    denyBtn.addEventListener('click', () => handleToolApproval(message.toolRequest!.requestId, false, messageDiv));
  } else if (message.role === 'assistant') {
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.innerHTML = marked.parse(message.content) as string;
    messageDiv.appendChild(contentDiv);
    
    // Add feedback buttons if responseId exists
    if (message.responseId) {
      const feedbackDiv = document.createElement('div');
      feedbackDiv.className = 'feedback-container';
      feedbackDiv.innerHTML = `
        <div class="feedback-buttons">
          <button class="feedback-btn thumbs-up" data-response-id="${message.responseId}" title="Helpful">
            👍
          </button>
          <button class="feedback-btn thumbs-down" data-response-id="${message.responseId}" title="Not helpful">
            👎
          </button>
        </div>
        <div class="feedback-comment-section" style="display: none;">
          <textarea class="feedback-comment-input" placeholder="Optional: Tell us more..." maxlength="1000"></textarea>
          <div class="feedback-comment-actions">
            <button class="feedback-submit-btn">Submit</button>
            <button class="feedback-cancel-btn">Cancel</button>
          </div>
        </div>
      `;
      
      const thumbsUpBtn = feedbackDiv.querySelector('.thumbs-up') as HTMLButtonElement;
      const thumbsDownBtn = feedbackDiv.querySelector('.thumbs-down') as HTMLButtonElement;
      const commentSection = feedbackDiv.querySelector('.feedback-comment-section') as HTMLElement;
      const commentInput = feedbackDiv.querySelector('.feedback-comment-input') as HTMLTextAreaElement;
      const submitBtn = feedbackDiv.querySelector('.feedback-submit-btn') as HTMLButtonElement;
      const cancelBtn = feedbackDiv.querySelector('.feedback-cancel-btn') as HTMLButtonElement;
      
      let selectedRating: boolean | null = null;
      
      const showCommentSection = (rating: boolean) => {
        selectedRating = rating;
        feedbackDiv.querySelector('.feedback-buttons')!.setAttribute('style', 'display: none;');
        commentSection.style.display = 'flex';
        commentInput.focus();
      };
      
      const hideCommentSection = () => {
        selectedRating = null;
        feedbackDiv.querySelector('.feedback-buttons')!.removeAttribute('style');
        commentSection.style.display = 'none';
        commentInput.value = '';
      };
      
      thumbsUpBtn.addEventListener('click', () => showCommentSection(true));
      thumbsDownBtn.addEventListener('click', () => showCommentSection(false));
      
      submitBtn.addEventListener('click', () => {
        if (selectedRating !== null) {
          const comment = commentInput.value.trim() || undefined;
          handleFeedback(message.responseId!, selectedRating, comment, feedbackDiv);
        }
      });
      
      cancelBtn.addEventListener('click', hideCommentSection);
      
      messageDiv.appendChild(feedbackDiv);
    }
  } else {
    messageDiv.textContent = message.content;
  }
  
  messagesContainer.appendChild(messageDiv);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

async function handleFeedback(responseId: string, rating: boolean, comment: string | undefined, feedbackDiv: HTMLElement) {
  const buttons = feedbackDiv.querySelectorAll('button');
  buttons.forEach(btn => (btn as HTMLButtonElement).disabled = true);

  try {
    const response = await fetch('/api/chat/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ responseId, rating, comment })
    });
    
    const data = await response.json();
    
    if (data.status === 'success') {
      feedbackDiv.innerHTML = `
        <div class="feedback-thanks">
          <span class="feedback-icon">${rating ? '👍' : '👎'}</span>
          <span class="feedback-text">Thank you for your feedback!</span>
        </div>
      `;
    } else {
      feedbackDiv.innerHTML = `
        <div class="feedback-error">Unable to submit feedback</div>
      `;
    }
  } catch (error) {
    feedbackDiv.innerHTML = `
      <div class="feedback-error">Error submitting feedback</div>
    `;
  }
}

async function handleToolApproval(requestId: string, approved: boolean, messageDiv: HTMLElement) {
  const buttons = messageDiv.querySelectorAll('button');
  buttons.forEach(btn => (btn as HTMLButtonElement).disabled = true);

  if (!approved) {
    addMessage({ role: 'assistant', content: 'Tool execution cancelled. How else can I help you?' });
    return;
  }

  const thinkingDiv = document.createElement('div');
  thinkingDiv.className = 'message assistant thinking';
  thinkingDiv.textContent = 'Executing tool...';
  messagesContainer.appendChild(thinkingDiv);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;

  try {
    const response = await fetch('/api/chat/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId, approved })
    });
    
    thinkingDiv.remove();
    const data = await response.json();
    if (data.response) {
      addMessage({ 
        role: 'assistant', 
        content: data.response,
        responseId: data.responseId 
      });
    }
  } catch (error) {
    thinkingDiv.remove();
    addMessage({ role: 'assistant', content: 'Error: Could not execute tool' });
  }
}

async function sendMessage() {
  const prompt = promptInput.value.trim();
  if (!prompt) return;

  promptHistory.push(prompt);
  historyIndex = promptHistory.length;
  addMessage({ role: 'user', content: prompt });
  promptInput.value = '';
  sendButton.disabled = true;

  const thinkingDiv = document.createElement('div');
  thinkingDiv.className = 'message assistant thinking';
  thinkingDiv.textContent = 'Thinking...';
  messagesContainer.appendChild(thinkingDiv);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;

  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: prompt })
    });
    
    thinkingDiv.remove();
    const data = await response.json();
    
    if (data.response) {
      addMessage({ 
        role: 'assistant', 
        content: data.response,
        responseId: data.responseId 
      });
    }
    
    if (data.toolRequest) {
      addMessage({ 
        role: 'tool-request', 
        content: '', 
        toolRequest: data.toolRequest 
      });
    }
  } catch (error) {
    thinkingDiv.remove();
    addMessage({ role: 'assistant', content: 'Error: Could not connect to server' });
  } finally {
    sendButton.disabled = false;
    promptInput.focus();
  }
}

sendButton.addEventListener('click', sendMessage);
promptInput.addEventListener('keypress', (e) => {
  if (e.key === 'Enter') sendMessage();
});

promptInput.addEventListener('keydown', (e) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault();
    if (historyIndex > 0) {
      historyIndex--;
      promptInput.value = promptHistory[historyIndex];
    }
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    if (historyIndex < promptHistory.length - 1) {
      historyIndex++;
      promptInput.value = promptHistory[historyIndex];
    } else {
      historyIndex = promptHistory.length;
      promptInput.value = '';
    }
  }
});
