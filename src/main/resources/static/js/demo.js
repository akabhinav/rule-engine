// Demo Application State
const DemoApp = {
    useCases: [],
    currentUseCase: null,
    loadedRules: [],
    executionCount: 0,

    // API Base URL
    API_BASE: '/api/rules',
    DEMO_BASE: '/demo',

    // Initialize the application
    async init() {
        console.log('Initializing Rule Engine Demo...');
        await this.loadUseCases();
        this.setupEventListeners();
        this.updateStats();
    },

    // Load all use cases
    async loadUseCases() {
        try {
            this.showLoading('Loading use cases...');
            const response = await fetch(`${this.DEMO_BASE}/use-cases`);
            this.useCases = await response.json();
            this.renderUseCases();
            this.hideLoading();
            this.showToast('Ready', 'Demo loaded successfully', 'success');
        } catch (error) {
            console.error('Failed to load use cases:', error);
            this.showToast('Error', 'Failed to load use cases', 'error');
            this.hideLoading();
        }
    },

    // Render use cases in sidebar
    renderUseCases() {
        const complexCases = this.useCases.filter(uc => uc.category === 'complex');
        const mediumCases = this.useCases.filter(uc => uc.category === 'medium');

        const complexContainer = document.getElementById('complexUseCases');
        const mediumContainer = document.getElementById('mediumUseCases');

        complexContainer.innerHTML = complexCases.map(uc => this.createUseCaseItem(uc)).join('');
        mediumContainer.innerHTML = mediumCases.map(uc => this.createUseCaseItem(uc)).join('');
    },

    // Create use case item HTML
    createUseCaseItem(useCase) {
        return `
            <div class="use-case-item" data-id="${useCase.id}" onclick="DemoApp.selectUseCase('${useCase.id}')">
                <div class="use-case-item-header">
                    <span class="use-case-icon">${useCase.icon}</span>
                    <span class="use-case-name">${useCase.name}</span>
                </div>
                <div class="use-case-category">${useCase.category}</div>
            </div>
        `;
    },

    // Select a use case
    selectUseCase(id) {
        this.currentUseCase = this.useCases.find(uc => uc.id === id);

        // Update active state
        document.querySelectorAll('.use-case-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-id="${id}"]`).classList.add('active');

        // Show use case view
        document.getElementById('welcomeScreen').style.display = 'none';
        document.getElementById('useCaseView').style.display = 'block';

        // Render use case details
        this.renderUseCaseDetails();

        // Reset state
        this.loadedRules = [];
        document.getElementById('rulesDisplay').style.display = 'none';
        document.getElementById('resultsSection').style.display = 'none';
        document.getElementById('viewRulesBtn').disabled = true;
    },

    // Render use case details
    renderUseCaseDetails() {
        const uc = this.currentUseCase;

        document.getElementById('useCaseTitle').innerHTML = `${uc.icon} ${uc.name}`;
        document.getElementById('useCaseCategory').innerHTML = uc.category;
        document.getElementById('useCaseCategory').className = `category-badge ${uc.category}`;
        document.getElementById('useCaseDescription').textContent = uc.description;

        // Render field descriptions
        const fieldList = document.getElementById('fieldList');
        fieldList.innerHTML = Object.entries(uc.fieldDescriptions)
            .map(([field, desc]) => `
                <div class="field-item">
                    <div class="field-name">${field}</div>
                    <div class="field-description">${desc}</div>
                </div>
            `).join('');

        // Render scenarios
        const scenariosList = document.getElementById('scenariosList');
        scenariosList.innerHTML = uc.scenarios.map((scenario, index) => `
            <div class="scenario-card" onclick="DemoApp.executeScenario(${index})">
                <div class="scenario-name">${scenario.name}</div>
                <div class="scenario-description">${scenario.description}</div>
                <div class="scenario-outcome">
                    <div class="scenario-outcome-label">Expected Outcome</div>
                    <div class="scenario-outcome-value">${scenario.expectedOutcome}</div>
                </div>
            </div>
        `).join('');
    },

    // Load rules for current use case
    async loadRules() {
        if (!this.currentUseCase) return;

        try {
            this.showLoading('Loading rules...');
            const response = await fetch(`${this.DEMO_BASE}/use-cases/${this.currentUseCase.id}/load`, {
                method: 'POST'
            });
            const data = await response.json();
            this.loadedRules = data.rules;

            this.showToast('Success', data.message, 'success');
            this.displayLoadedRules();
            this.hideLoading();
            document.getElementById('viewRulesBtn').disabled = false;
            this.updateStats();
        } catch (error) {
            console.error('Failed to load rules:', error);
            this.showToast('Error', 'Failed to load rules', 'error');
            this.hideLoading();
        }
    },

    // Display loaded rules
    displayLoadedRules() {
        const rulesDisplay = document.getElementById('rulesDisplay');
        const rulesList = document.getElementById('rulesList');

        rulesList.innerHTML = this.loadedRules.map(rule => `
            <div class="rule-item">
                <div class="rule-header">
                    <div class="rule-name">${rule.name}</div>
                    <div class="rule-priority">Priority: ${rule.priority}</div>
                </div>
                <div class="rule-description">${rule.description}</div>
            </div>
        `).join('');

        rulesDisplay.style.display = 'block';
    },

    // Execute a scenario
    async executeScenario(index) {
        if (!this.currentUseCase || this.loadedRules.length === 0) {
            this.showToast('Warning', 'Please load rules first', 'warning');
            return;
        }

        const scenario = this.currentUseCase.scenarios[index];

        try {
            // Visual feedback
            const cards = document.querySelectorAll('.scenario-card');
            cards[index].classList.add('executing');

            this.showLoading('Executing rules...');

            const response = await fetch(`${this.DEMO_BASE}/use-cases/${this.currentUseCase.id}/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: scenario.name,
                    facts: scenario.facts
                })
            });

            const data = await response.json();
            this.executionCount++;
            this.updateStats();

            this.displayResults(data, scenario);
            this.hideLoading();
            cards[index].classList.remove('executing');

            this.showToast('Executed', `Scenario "${scenario.name}" completed`, 'success');
        } catch (error) {
            console.error('Failed to execute scenario:', error);
            this.showToast('Error', 'Failed to execute scenario', 'error');
            this.hideLoading();
        }
    },

    // Display execution results
    displayResults(data, scenario) {
        const resultsSection = document.getElementById('resultsSection');
        const executionSummary = document.getElementById('executionSummary');
        const executionResults = document.getElementById('executionResults');

        // Summary
        const summary = data.summary;
        executionSummary.innerHTML = `
            <div class="summary-item">
                <div class="summary-label">Total Rules</div>
                <div class="summary-value">${summary.totalRules}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">Executed</div>
                <div class="summary-value">${summary.executed}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">Matched</div>
                <div class="summary-value success">${summary.matched}</div>
            </div>
            <div class="summary-item">
                <div class="summary-label">Avg Time</div>
                <div class="summary-value time">${summary.averageExecutionTime.toFixed(2)}ms</div>
            </div>
        `;

        // Results
        executionResults.innerHTML = data.results.map(result => {
            const matchedClass = result.matched ? 'matched' : 'not-matched';
            const statusBadges = [
                result.matched ? '<span class="status-badge matched">Matched</span>' : '<span class="status-badge not-matched">Not Matched</span>',
                result.executed ? '<span class="status-badge executed">Executed</span>' : ''
            ].filter(Boolean).join('');

            return `
                <div class="result-card ${matchedClass}">
                    <div class="result-header">
                        <div class="result-rule-name">${result.ruleName}</div>
                        <div class="result-status">${statusBadges}</div>
                    </div>
                    <div class="result-details">
                        ${result.matched ? `
                            <div class="result-detail">
                                <span class="detail-label">Execution Time:</span>
                                <span class="detail-value">${result.executionTimeMs}ms</span>
                            </div>
                            ${result.output && Object.keys(result.output).length > 0 ? `
                                <div class="result-detail">
                                    <span class="detail-label">Output:</span>
                                    <span class="detail-value">${JSON.stringify(result.output)}</span>
                                </div>
                            ` : ''}
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');

        resultsSection.style.display = 'block';

        // Scroll to results
        resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    },

    // Toggle rules view
    toggleRulesView() {
        const rulesDisplay = document.getElementById('rulesDisplay');
        const isVisible = rulesDisplay.style.display === 'block';
        rulesDisplay.style.display = isVisible ? 'none' : 'block';
    },

    // Reset demo
    async resetDemo() {
        if (!confirm('This will delete all loaded rules. Continue?')) {
            return;
        }

        try {
            this.showLoading('Resetting demo...');
            await fetch(`${this.DEMO_BASE}/reset`, { method: 'POST' });

            this.loadedRules = [];
            this.executionCount = 0;
            this.currentUseCase = null;

            document.getElementById('useCaseView').style.display = 'none';
            document.getElementById('welcomeScreen').style.display = 'flex';

            document.querySelectorAll('.use-case-item').forEach(item => {
                item.classList.remove('active');
            });

            this.updateStats();
            this.hideLoading();
            this.showToast('Reset', 'Demo reset successfully', 'success');
        } catch (error) {
            console.error('Failed to reset demo:', error);
            this.showToast('Error', 'Failed to reset demo', 'error');
            this.hideLoading();
        }
    },

    // Update statistics display
    updateStats() {
        document.getElementById('rulesCount').textContent = this.loadedRules.length;
        document.getElementById('executionCount').textContent = this.executionCount;
    },

    // Show loading overlay
    showLoading(text = 'Loading...') {
        document.getElementById('loadingText').textContent = text;
        document.getElementById('loadingOverlay').style.display = 'flex';
    },

    // Hide loading overlay
    hideLoading() {
        document.getElementById('loadingOverlay').style.display = 'none';
    },

    // Show toast notification
    showToast(title, message, type = 'info') {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <div class="toast-icon">${icons[type]}</div>
                <div class="toast-message">
                    <div class="toast-title">${title}</div>
                    <div class="toast-text">${message}</div>
                </div>
            </div>
        `;

        document.getElementById('toastContainer').appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'toastSlideIn 0.3s ease reverse';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

    // Setup event listeners
    setupEventListeners() {
        document.getElementById('resetBtn').addEventListener('click', () => this.resetDemo());
        document.getElementById('loadRulesBtn').addEventListener('click', () => this.loadRules());
        document.getElementById('viewRulesBtn').addEventListener('click', () => this.toggleRulesView());

        // Modal close
        document.querySelector('.modal-close')?.addEventListener('click', () => {
            document.getElementById('ruleModal').classList.remove('show');
        });

        // Close modal on backdrop click
        document.getElementById('ruleModal')?.addEventListener('click', (e) => {
            if (e.target.id === 'ruleModal') {
                document.getElementById('ruleModal').classList.remove('show');
            }
        });
    }
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    DemoApp.init();
});

// Export for global access
window.DemoApp = DemoApp;
