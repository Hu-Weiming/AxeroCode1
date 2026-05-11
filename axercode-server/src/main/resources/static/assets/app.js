const appShell = document.getElementById('appShell');
const sidebar = document.getElementById('sidebar');
const sidebarToggleButton = document.getElementById('sidebarToggleButton');
const sidebarCloseButton = document.getElementById('sidebarCloseButton');
const sidebarScrim = document.getElementById('sidebarScrim');
const sessionList = document.getElementById('sessionList');
const conversationTitle = document.getElementById('conversationTitle');
const welcomePanel = document.getElementById('welcomePanel');
const transcript = document.getElementById('transcript');
const composer = document.getElementById('composer');
const promptInput = document.getElementById('prompt');
const sendButton = document.getElementById('sendButton');
const statusLine = document.getElementById('status');
const sessionNote = document.getElementById('sessionNote');
const newChatButton = document.getElementById('newChatButton');
const planModeButton = document.getElementById('planModeButton');
const shellStateSummary = document.getElementById('shellStateSummary');
const providerSelect = document.getElementById('providerSelect');
const modelInput = document.getElementById('modelInput');
const parseEventFrame = window.AxerCodeSse?.parseEventFrame;

const SESSION_STORAGE_KEY = 'axercode.sessionId';
const SIDEBAR_COLLAPSED_STORAGE_KEY = 'axercode.sidebarCollapsed';
const PROVIDER_STORAGE_KEY = 'axercode.provider';
const MODEL_STORAGE_KEY = 'axercode.model';
const SESSION_LIST_LIMIT = 20;
const COMPOSER_MAX_ROWS = 10;
const mobileSidebarQuery = window.matchMedia('(max-width: 960px)');
const OPENAI_COMING_SOON_MESSAGE = '当前功能开发难度较大，目前正在加急修复中。';
const FALLBACK_PROVIDERS = [
    {
        id: 'ollama',
        label: 'Ollama',
        defaultModel: 'qwen2.5:7b',
        configured: true,
        comingSoon: false,
        supportsStreaming: true
    },
    {
        id: 'anthropic',
        label: 'Anthropic',
        defaultModel: 'claude-3-5-sonnet-latest',
        configured: false,
        comingSoon: false,
        supportsStreaming: true
    },
    {
        id: 'openai',
        label: 'OpenAI-compatible',
        defaultModel: 'gpt-4o-mini',
        configured: false,
        comingSoon: true,
        supportsStreaming: true
    }
];

let sessionId = localStorage.getItem(SESSION_STORAGE_KEY);
let sessionSummaries = [];
let providers = [];
let inFlight = false;
let btwInFlight = false;
let shellState = null;
let sidebarOpen = false;
let sidebarCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY) === 'true';

newChatButton.addEventListener('click', () => {
    if (inFlight || btwInFlight) {
        return;
    }

    sessionId = null;
    localStorage.removeItem(SESSION_STORAGE_KEY);
    clearTranscript();
    renderSessionList();
    updateConversationMeta();
    setBusy(false, 'Ready to start a new chat.');
    setSidebarOpen(false);
    promptInput.focus();
});

providerSelect.addEventListener('change', () => {
    if (inFlight || btwInFlight) {
        return;
    }

    const provider = selectedProvider();
    if (!provider) {
        return;
    }

    syncRuntimeSelection({
        providerId: provider.id,
        model: provider.defaultModel
    });

    if (provider.comingSoon) {
        statusLine.textContent = OPENAI_COMING_SOON_MESSAGE;
        return;
    }

    if (provider.id === 'anthropic' && !provider.configured) {
        statusLine.textContent = 'Anthropic API key is not configured on the server.';
        return;
    }

    statusLine.textContent = `${provider.label} ready.`;
});

modelInput.addEventListener('change', () => {
    persistRuntimeSelection(currentProviderId(), currentModel());
});

promptInput.addEventListener('input', () => {
    resizeComposerInput();
});

planModeButton.addEventListener('click', async () => {
    if (inFlight || btwInFlight) {
        return;
    }

    const nextEnabled = !(shellState?.planModeEnabled);
    statusLine.textContent = nextEnabled ? 'Enabling plan mode...' : 'Disabling plan mode...';
    planModeButton.disabled = true;

    try {
        await updatePlanMode(nextEnabled);
        statusLine.textContent = nextEnabled ? 'Plan mode enabled.' : 'Plan mode disabled.';
    } catch (error) {
        statusLine.textContent = 'Could not update plan mode.';
    } finally {
        refreshComposerState();
    }
});

sidebarToggleButton.addEventListener('click', () => {
    if (inFlight || btwInFlight) {
        return;
    }

    if (isMobileViewport()) {
        setSidebarOpen(!sidebarOpen);
        return;
    }

    setSidebarCollapsed(!sidebarCollapsed);
});

sidebarCloseButton.addEventListener('click', () => {
    if (isMobileViewport()) {
        setSidebarOpen(false);
        return;
    }

    setSidebarCollapsed(true);
});

sidebarScrim.addEventListener('click', () => {
    setSidebarOpen(false);
});

mobileSidebarQuery.addEventListener('change', () => {
    if (!isMobileViewport()) {
        sidebarOpen = false;
    }
    applySidebarState();
});

sessionList.addEventListener('click', async (event) => {
    const target = event.target.closest('[data-session-id]');
    if (!target || inFlight || btwInFlight) {
        return;
    }

    await activateSession(target.dataset.sessionId);
});

composer.addEventListener('submit', async (event) => {
    event.preventDefault();
    const prompt = promptInput.value.trim();
    if (!prompt) {
        return;
    }

    if (inFlight) {
        await submitBtw(prompt);
        return;
    }

    if (selectedProvider()?.comingSoon) {
        appendMessage('assistant', assistantLabel(currentModel()), OPENAI_COMING_SOON_MESSAGE);
        promptInput.value = '';
        setBusy(false, OPENAI_COMING_SOON_MESSAGE);
        return;
    }

    appendMessage('user', '', prompt);
    promptInput.value = '';
    resizeComposerInput();
    if (!sessionId) {
        conversationTitle.textContent = summarizeText(prompt, 48, 'New chat');
    }

    const assistantView = appendMessage('assistant', assistantLabel(currentModel()), '');
    assistantView.body.classList.add('is-streaming');
    assistantView.rawContent = '';
    setBusy(true, 'Streaming...');

    try {
        await streamChat(prompt, assistantView);
        setBusy(false, 'Ready.');
    } catch (error) {
        assistantView.article.classList.add('error');
        assistantView.body.classList.remove('is-streaming');
        const message = error instanceof Error && error.message
                ? error.message
                : 'The request did not complete. Check the local server and try again.';
        const fallbackBody = assistantView.rawContent.trim()
                ? `${assistantView.rawContent}\n\n${message}`
                : message;
        renderMessageBody(assistantView.body, fallbackBody);
        setBusy(false, message);
    }
});

promptInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        composer.requestSubmit();
    }
});

function setSidebarOpen(nextState) {
    sidebarOpen = Boolean(nextState);
    applySidebarState();
}

function setSidebarCollapsed(nextState) {
    sidebarCollapsed = Boolean(nextState);
    localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, String(sidebarCollapsed));
    applySidebarState();
}

function applySidebarState() {
    const mobile = isMobileViewport();
    appShell.classList.toggle('sidebar-open', mobile && sidebarOpen);
    appShell.classList.toggle('sidebar-collapsed', !mobile && sidebarCollapsed);
    sidebarScrim.hidden = !(mobile && sidebarOpen);
    sidebarToggleButton.setAttribute('aria-expanded', String(mobile ? sidebarOpen : !sidebarCollapsed));
    updateSidebarToggleLabel();
}

function isMobileViewport() {
    return mobileSidebarQuery.matches;
}

function updateSidebarToggleLabel() {
    const expanded = isMobileViewport() ? sidebarOpen : !sidebarCollapsed;
    sidebarToggleButton.textContent = expanded ? 'Hide history' : 'Show history';
    sidebarToggleButton.setAttribute('aria-label', expanded ? 'Hide history' : 'Show history');
}

function resizeComposerInput() {
    promptInput.style.height = 'auto';
    const computedStyle = window.getComputedStyle(promptInput);
    const lineHeight = Number.parseFloat(computedStyle.lineHeight) || 24;
    const paddingTop = Number.parseFloat(computedStyle.paddingTop) || 0;
    const paddingBottom = Number.parseFloat(computedStyle.paddingBottom) || 0;
    const minHeight = lineHeight + paddingTop + paddingBottom;
    const maxHeight = lineHeight * COMPOSER_MAX_ROWS + paddingTop + paddingBottom;
    const nextHeight = Math.min(Math.max(promptInput.scrollHeight, minHeight), maxHeight);
    promptInput.style.height = `${nextHeight}px`;
    promptInput.style.overflowY = promptInput.scrollHeight > maxHeight ? 'auto' : 'hidden';
}

function appendMessage(kind, label, content, options = {}) {
    const article = document.createElement('article');
    article.className = `message ${kind}`;
    if (options.toolMessage) {
        article.classList.add('tool-message');
    }

    const card = document.createElement('div');
    card.className = 'message-card';

    const role = document.createElement('p');
    role.className = 'message-role';
    role.textContent = label;

    const body = document.createElement('div');
    body.className = 'message-body';
    if (content) {
        renderMessageBody(body, content);
    }

    card.append(role, body);
    article.append(card);
    transcript.append(article);
    syncEmptyState();
    article.scrollIntoView({ behavior: 'smooth', block: 'end' });
    return { article, role, body, rawContent: content || '' };
}

function setMessageRoleLabel(messageView, label) {
    messageView.role.textContent = label;
}

function assistantLabel(modelName) {
    const label = (modelName || '').trim();
    return label || 'Assistant';
}

function renderMessageBody(container, content) {
    container.innerHTML = '';

    const source = content ?? '';
    const fencePattern = /```([\s\S]*?)```/g;
    let lastIndex = 0;
    let match;

    while ((match = fencePattern.exec(source)) !== null) {
        appendTextBlocks(container, source.slice(lastIndex, match.index));
        appendCodeBlock(container, match[1]);
        lastIndex = fencePattern.lastIndex;
    }

    appendTextBlocks(container, source.slice(lastIndex));

    if (!container.childNodes.length) {
        const paragraph = document.createElement('p');
        paragraph.textContent = '';
        container.append(paragraph);
    }
}

function appendTextBlocks(container, text) {
    const trimmed = text.replace(/^\n+|\n+$/g, '');
    if (!trimmed) {
        return;
    }

    const paragraphs = trimmed.split(/\n{2,}/);
    for (const paragraphText of paragraphs) {
        const paragraph = document.createElement('p');
        paragraph.textContent = paragraphText;
        container.append(paragraph);
    }
}

function appendCodeBlock(container, fencedBlock) {
    let language = '';
    let code = fencedBlock;
    const firstNewline = fencedBlock.indexOf('\n');

    if (firstNewline >= 0) {
        const possibleLanguage = fencedBlock.slice(0, firstNewline).trim();
        const remainder = fencedBlock.slice(firstNewline + 1);
        if (/^[A-Za-z0-9_+#.-]{1,20}$/.test(possibleLanguage)) {
            language = possibleLanguage;
            code = remainder;
        }
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'code-block';

    if (language) {
        const badge = document.createElement('span');
        badge.className = 'code-language';
        badge.textContent = language;
        wrapper.append(badge);
    }

    const pre = document.createElement('pre');
    const codeElement = document.createElement('code');
    codeElement.textContent = code.replace(/^\n+|\n+$/g, '');
    pre.append(codeElement);
    wrapper.append(pre);
    container.append(wrapper);
}

async function streamChat(prompt, assistantView) {
    const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            prompt,
            sessionId,
            provider: currentProviderId(),
            model: currentModel()
        })
    });

    if (!response.ok || !response.body) {
        throw new Error(`Unexpected response: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
        const { value, done } = await reader.read();
        if (done) {
            break;
        }

        buffer += decoder.decode(value, { stream: true });
        const frames = buffer.split('\n\n');
        buffer = frames.pop() ?? '';

        for (const frame of frames) {
            const parsed = parseEventFrame(frame);
            if (!parsed) {
                continue;
            }

            if (parsed.event === 'session') {
                const payload = JSON.parse(parsed.data);
                sessionId = payload.sessionId ?? sessionId;
                if (sessionId) {
                    localStorage.setItem(SESSION_STORAGE_KEY, sessionId);
                    updateConversationMeta();
                }
                continue;
            }

            if (parsed.event === 'token') {
                assistantView.rawContent += parsed.data;
                assistantView.body.textContent = assistantView.rawContent;
                continue;
            }

            if (parsed.event === 'complete') {
                const payload = JSON.parse(parsed.data);
                sessionId = payload.sessionId ?? sessionId;
                if (sessionId) {
                    localStorage.setItem(SESSION_STORAGE_KEY, sessionId);
                }
                syncRuntimeSelection({
                    providerId: payload.provider ?? currentProviderId(),
                    model: payload.model ?? currentModel()
                });

                const finalReply = assistantView.rawContent.trim() ? assistantView.rawContent : (payload.reply ?? '');
                setMessageRoleLabel(assistantView, assistantLabel(payload.model ?? currentModel()));
                assistantView.body.classList.remove('is-streaming');
                renderMessageBody(assistantView.body, finalReply);
                renderToolResults(assistantView.article, payload.toolResults ?? []);
                await loadSessionSummaries();
                updateConversationMeta();
                continue;
            }

            if (parsed.event === 'error') {
                const payload = JSON.parse(parsed.data);
                throw new Error(payload.message || 'The request did not complete.');
            }
        }
    }
}

function renderToolResults(article, toolResults) {
    const existing = article.querySelector('.tool-results');
    if (existing) {
        existing.remove();
    }

    if (!toolResults.length) {
        return;
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'tool-results';

    for (const toolResult of toolResults) {
        const item = document.createElement('details');
        item.className = 'tool-result';

        const summary = document.createElement('summary');
        summary.textContent = `Tool ${toolResult.toolName} ${toolResult.status}`;

        const output = document.createElement('pre');
        output.className = 'tool-output';
        output.textContent = toolResult.output;

        item.append(summary, output);
        wrapper.append(item);
    }

    article.querySelector('.message-card').append(wrapper);
}

async function loadSessionSummaries() {
    try {
        const response = await fetch('/api/sessions?limit=20');
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        sessionSummaries = await response.json();
        renderSessionList();
        updateConversationMeta();
    } catch (error) {
        sessionSummaries = [];
        renderSessionList('Could not load conversation history.');
    }
}

function renderSessionList(emptyMessage = 'No saved conversations yet.') {
    sessionList.innerHTML = '';

    if (!sessionSummaries.length) {
        const empty = document.createElement('div');
        empty.className = 'session-empty';
        empty.textContent = emptyMessage;
        sessionList.append(empty);
        return;
    }

    for (const summary of sessionSummaries) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'session-item';
        button.dataset.sessionId = summary.sessionId;
        if (summary.sessionId === sessionId) {
            button.classList.add('active');
        }

        const title = document.createElement('p');
        title.className = 'session-title';
        title.textContent = summary.title;

        const preview = document.createElement('p');
        preview.className = 'session-preview';
        preview.textContent = summary.preview;

        const meta = document.createElement('span');
        meta.className = 'session-meta';
        meta.textContent = `${summary.messageCount} messages`;

        button.append(title, preview, meta);
        sessionList.append(button);
    }
}

async function activateSession(nextSessionId) {
    if (!nextSessionId) {
        return;
    }

    sessionId = nextSessionId;
    localStorage.setItem(SESSION_STORAGE_KEY, sessionId);
    renderSessionList();
    updateConversationMeta();
    setBusy(true, 'Loading conversation...');

    try {
        const response = await fetch(`/api/sessions/${sessionId}`);
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        const payload = await response.json();
        syncRuntimeSelection({
            providerId: payload.provider ?? currentProviderId(),
            model: payload.model ?? currentModel()
        });
        clearTranscript();
        for (const message of payload.messages) {
            renderStoredMessage(message, payload.model ?? currentModel());
        }
        updateConversationMeta();
        setBusy(false, 'Conversation loaded.');
        setSidebarOpen(false);
    } catch (error) {
        sessionId = null;
        localStorage.removeItem(SESSION_STORAGE_KEY);
        clearTranscript();
        renderSessionList();
        updateConversationMeta();
        setBusy(false, 'Could not load conversation.');
    }
}

function renderStoredMessage(message, fallbackAssistantModel = currentModel()) {
    const role = message.role || 'ASSISTANT';
    if (role === 'USER') {
        appendMessage('user', '', message.content);
        return;
    }

    if (role === 'TOOL') {
        appendMessage('assistant', 'Tool', message.content, { toolMessage: true });
        return;
    }

    appendMessage('assistant', assistantLabel(message.model || fallbackAssistantModel), message.content);
}

async function restoreSession() {
    if (!sessionId) {
        clearTranscript();
        updateConversationMeta();
        return;
    }

    await activateSession(sessionId);
}

async function submitBtw(message) {
    if (btwInFlight) {
        return;
    }
    if (!sessionId) {
        statusLine.textContent = 'BTW becomes available once the active turn session is ready.';
        return;
    }

    appendMessage('user', '', message);
    promptInput.value = '';
    resizeComposerInput();
    setBtwBusy(true, 'Queueing BTW...');
    let nextStatus = inFlight ? 'Streaming...' : 'Ready.';

    try {
        const response = await fetch('/api/chat/btw', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                sessionId,
                message
            })
        });

        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        const payload = await response.json();
        nextStatus = `BTW queued for the next round (${payload.queuedCount}).`;
    } catch (error) {
        nextStatus = 'Could not queue BTW for this turn.';
    } finally {
        setBtwBusy(false, nextStatus);
    }
}

async function loadShellState() {
    try {
        const response = await fetch('/api/shell-state');
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        shellState = await response.json();
        renderShellState();
    } catch (error) {
        shellState = null;
        renderShellState('Shell state unavailable.');
    }
}

async function updatePlanMode(enabled) {
    const response = await fetch('/api/shell-state/plan-mode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ enabled })
    });

    if (!response.ok) {
        throw new Error(`Unexpected response: ${response.status}`);
    }

    shellState = await response.json();
    renderShellState();
}

function updateConversationMeta() {
    const activeSummary = sessionSummaries.find((summary) => summary.sessionId === sessionId);
    conversationTitle.textContent = activeSummary?.title ?? conversationTitle.textContent ?? 'New chat';

    if (!activeSummary) {
        if (!sessionId) {
            conversationTitle.textContent = 'New chat';
            sessionNote.textContent = 'Start a new conversation.';
            return;
        }

        sessionNote.textContent = `Session ${sessionId.slice(0, 8)} is active.`;
        return;
    }

    sessionNote.textContent = `${activeSummary.messageCount} messages in this conversation.`;
    conversationTitle.textContent = activeSummary.title;
}

function clearTranscript() {
    transcript.innerHTML = '';
    syncEmptyState();
}

function syncEmptyState() {
    const hasMessages = transcript.childElementCount > 0;
    welcomePanel.hidden = hasMessages;
}

function setBusy(nextState, message) {
    inFlight = nextState;
    statusLine.textContent = message;
    refreshComposerState();
}

function setBtwBusy(nextState, message) {
    btwInFlight = nextState;
    statusLine.textContent = message;
    refreshComposerState();
}

function refreshComposerState() {
    const controlsLocked = btwInFlight;
    promptInput.disabled = controlsLocked;
    sendButton.disabled = controlsLocked;
    newChatButton.disabled = inFlight || btwInFlight;
    sidebarToggleButton.disabled = inFlight || btwInFlight;
    sidebarCloseButton.disabled = inFlight || btwInFlight;
    planModeButton.disabled = inFlight || btwInFlight;
    providerSelect.disabled = inFlight || btwInFlight;
    modelInput.disabled = inFlight || btwInFlight;
    sendButton.textContent = inFlight ? 'BTW' : 'Send';
    promptInput.placeholder = inFlight ? 'Add a BTW note for the next round' : 'Message AxerCode';
}

function renderShellState(fallbackMessage = null) {
    if (!shellState) {
        planModeButton.textContent = 'Plan mode: off';
        planModeButton.setAttribute('aria-pressed', 'false');
        shellStateSummary.textContent = fallbackMessage ?? 'Plan mode: off';
        return;
    }

    const enabled = Boolean(shellState.planModeEnabled);
    const segments = [enabled ? 'Plan mode: on' : 'Plan mode: off'];
    if (shellState.focusPath) {
        segments.push(`Focus: ${shellState.focusPath}`);
    }
    if (shellState.activeCheckpointName) {
        segments.push(`Checkpoint: ${shellState.activeCheckpointName}`);
    }
    if (shellState.activeBranchName) {
        segments.push(`Branch: ${shellState.activeBranchName}`);
    }

    planModeButton.textContent = enabled ? 'Plan mode: on' : 'Plan mode: off';
    planModeButton.setAttribute('aria-pressed', String(enabled));
    shellStateSummary.textContent = segments.join(' | ');
}

function summarizeText(text, maxLength, fallback) {
    const normalized = (text || '').replace(/\s+/g, ' ').trim();
    if (!normalized) {
        return fallback;
    }
    if (normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.slice(0, maxLength - 3).trim()}...`;
}

async function initializeApp() {
    applySidebarState();
    resizeComposerInput();
    refreshComposerState();
    await loadProviders();
    await loadShellState();
    await loadSessionSummaries();
    await restoreSession();
    syncEmptyState();
}

initializeApp();

async function loadProviders() {
    try {
        const response = await fetch('/api/providers');
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }
        providers = await response.json();
    } catch (error) {
        providers = [...FALLBACK_PROVIDERS];
        statusLine.textContent = 'Using fallback provider catalog.';
    }

    renderProviderOptions();
}

function renderProviderOptions() {
    providerSelect.innerHTML = '';
    for (const provider of providers) {
        const option = document.createElement('option');
        option.value = provider.id;
        option.textContent = providerLabel(provider);
        providerSelect.append(option);
    }

    const savedProviderId = localStorage.getItem(PROVIDER_STORAGE_KEY);
    const savedModel = localStorage.getItem(MODEL_STORAGE_KEY);
    const initialProvider = providers.find((provider) => provider.id === savedProviderId) ?? providers[0];
    syncRuntimeSelection({
        providerId: initialProvider?.id,
        model: savedModel || initialProvider?.defaultModel
    });
}

function syncRuntimeSelection({ providerId, model }) {
    const provider = providers.find((item) => item.id === providerId) ?? providers[0];
    if (!provider) {
        return;
    }

    providerSelect.value = provider.id;
    modelInput.value = model || provider.defaultModel || '';
    modelInput.placeholder = provider.defaultModel || 'Model name';
    persistRuntimeSelection(provider.id, modelInput.value);
}

function persistRuntimeSelection(providerId, model) {
    if (providerId) {
        localStorage.setItem(PROVIDER_STORAGE_KEY, providerId);
    }
    if (model) {
        localStorage.setItem(MODEL_STORAGE_KEY, model);
    }
}

function selectedProvider() {
    return providers.find((provider) => provider.id === providerSelect.value) ?? null;
}

function currentProviderId() {
    return providerSelect.value || selectedProvider()?.id || '';
}

function currentModel() {
    return modelInput.value.trim() || selectedProvider()?.defaultModel || '';
}

function providerLabel(provider) {
    if (provider.comingSoon) {
        return `${provider.label} (Coming soon)`;
    }
    if (!provider.configured && provider.id === 'anthropic') {
        return `${provider.label} (Needs server key)`;
    }
    return provider.label;
}
