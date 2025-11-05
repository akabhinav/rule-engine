# Quick Start Guide: Phase 4 - Admin Dashboard

## 🎯 Goal
Build a beautiful, intuitive Admin Dashboard that allows users to:
- Create rules visually (no coding required)
- Monitor rule performance in real-time
- Test rules with live data
- Manage data sources and integrations

---

## 📋 Prerequisites

### Backend (Already Complete ✅)
- Spring Boot REST API is ready
- Rule engine core is functional
- All data integration components are in place

### What We Need to Add
1. **Frontend Application** (React + TypeScript)
2. **Enhanced REST APIs** for UI features
3. **WebSocket Support** for real-time updates
4. **Authentication & Authorization** (JWT + RBAC)

---

## 🏗️ Implementation Plan

### Step 1: Project Setup (Day 1)
Create a modern React application with best practices.

```bash
# Navigate to project root
cd /home/user/rule-engine

# Create frontend directory
mkdir -p frontend
cd frontend

# Initialize React + TypeScript project
npx create-react-app rule-engine-ui --template typescript

# Install core dependencies
cd rule-engine-ui
npm install @reduxjs/toolkit react-redux
npm install @mui/material @mui/icons-material @emotion/react @emotion/styled
npm install axios react-router-dom
npm install recharts
npm install monaco-editor @monaco-editor/react
npm install react-flow-renderer
```

### Step 2: Backend API Enhancements (Days 2-3)

Create new controllers and DTOs for UI-specific endpoints.

#### File: `src/main/java/com/ruleengine/api/DashboardController.java`
```java
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private AuditLogger auditLogger;

    @GetMapping("/metrics")
    public DashboardMetrics getMetrics() {
        return DashboardMetrics.builder()
            .totalRules(ruleService.getTotalRules())
            .activeRules(ruleService.getActiveRules())
            .totalExecutions(auditLogger.getTotalExecutions())
            .avgExecutionTime(auditLogger.getAverageExecutionTime())
            .successRate(auditLogger.getSuccessRate())
            .build();
    }

    @GetMapping("/recent-executions")
    public List<ExecutionSummary> getRecentExecutions(
        @RequestParam(defaultValue = "50") int limit
    ) {
        return auditLogger.getRecentExecutions(limit);
    }

    @GetMapping("/top-rules")
    public List<RulePerformance> getTopRules() {
        return ruleService.getTopRulesByExecutionCount(10);
    }
}
```

#### File: `src/main/java/com/ruleengine/api/dto/DashboardMetrics.java`
```java
@Data
@Builder
public class DashboardMetrics {
    private long totalRules;
    private long activeRules;
    private long totalExecutions;
    private double avgExecutionTime;
    private double successRate;
    private Map<String, Long> executionsByHour;
}
```

### Step 3: Frontend Components (Days 4-10)

#### Component Structure
```
frontend/rule-engine-ui/src/
├── components/
│   ├── Dashboard/
│   │   ├── MetricsCard.tsx
│   │   ├── ExecutionChart.tsx
│   │   ├── RecentActivity.tsx
│   │   └── TopRules.tsx
│   ├── RuleBuilder/
│   │   ├── VisualRuleBuilder.tsx
│   │   ├── ConditionEditor.tsx
│   │   ├── ActionEditor.tsx
│   │   └── RulePreview.tsx
│   ├── RuleList/
│   │   ├── RuleTable.tsx
│   │   ├── RuleCard.tsx
│   │   └── RuleFilters.tsx
│   ├── Testing/
│   │   ├── RuleTestPlayground.tsx
│   │   ├── JsonEditor.tsx
│   │   └── ExecutionTrace.tsx
│   └── DataSources/
│       ├── DataSourceList.tsx
│       ├── DataSourceForm.tsx
│       └── ConnectionTester.tsx
├── pages/
│   ├── DashboardPage.tsx
│   ├── RulesPage.tsx
│   ├── TestingPage.tsx
│   └── SettingsPage.tsx
├── services/
│   ├── api.ts
│   └── ruleService.ts
└── store/
    ├── rulesSlice.ts
    └── dashboardSlice.ts
```

#### Key Component: Visual Rule Builder

**File: `frontend/rule-engine-ui/src/components/RuleBuilder/VisualRuleBuilder.tsx`**
```typescript
import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Select,
  MenuItem,
} from '@mui/material';
import { Add, Delete } from '@mui/icons-material';

interface Condition {
  field: string;
  operator: string;
  value: any;
}

interface Action {
  type: string;
  factKey?: string;
  factValue?: any;
}

export const VisualRuleBuilder: React.FC = () => {
  const [ruleName, setRuleName] = useState('');
  const [conditions, setConditions] = useState<Condition[]>([]);
  const [actions, setActions] = useState<Action[]>([]);

  const operators = [
    'EQUALS',
    'NOT_EQUALS',
    'GREATER_THAN',
    'LESS_THAN',
    'CONTAINS',
    'IN',
  ];

  const addCondition = () => {
    setConditions([...conditions, { field: '', operator: 'EQUALS', value: '' }]);
  };

  const addAction = () => {
    setActions([...actions, { type: 'SET_FACT', factKey: '', factValue: '' }]);
  };

  const saveRule = async () => {
    const rule = {
      name: ruleName,
      conditions,
      actions,
      enabled: true,
    };

    const response = await fetch('http://localhost:8080/api/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(rule),
    });

    if (response.ok) {
      alert('Rule saved successfully!');
    }
  };

  return (
    <Box>
      <Card>
        <CardContent>
          <TextField
            fullWidth
            label="Rule Name"
            value={ruleName}
            onChange={(e) => setRuleName(e.target.value)}
            margin="normal"
          />

          <h3>Conditions</h3>
          {conditions.map((condition, index) => (
            <Box key={index} display="flex" gap={2} mb={2}>
              <TextField
                label="Field"
                value={condition.field}
                onChange={(e) => {
                  const newConditions = [...conditions];
                  newConditions[index].field = e.target.value;
                  setConditions(newConditions);
                }}
              />
              <Select
                value={condition.operator}
                onChange={(e) => {
                  const newConditions = [...conditions];
                  newConditions[index].operator = e.target.value;
                  setConditions(newConditions);
                }}
              >
                {operators.map((op) => (
                  <MenuItem key={op} value={op}>
                    {op}
                  </MenuItem>
                ))}
              </Select>
              <TextField
                label="Value"
                value={condition.value}
                onChange={(e) => {
                  const newConditions = [...conditions];
                  newConditions[index].value = e.target.value;
                  setConditions(newConditions);
                }}
              />
              <Button
                onClick={() => {
                  setConditions(conditions.filter((_, i) => i !== index));
                }}
              >
                <Delete />
              </Button>
            </Box>
          ))}
          <Button onClick={addCondition} startIcon={<Add />}>
            Add Condition
          </Button>

          <h3>Actions</h3>
          {actions.map((action, index) => (
            <Box key={index} display="flex" gap={2} mb={2}>
              <TextField
                label="Fact Key"
                value={action.factKey}
                onChange={(e) => {
                  const newActions = [...actions];
                  newActions[index].factKey = e.target.value;
                  setActions(newActions);
                }}
              />
              <TextField
                label="Fact Value"
                value={action.factValue}
                onChange={(e) => {
                  const newActions = [...actions];
                  newActions[index].factValue = e.target.value;
                  setActions(newActions);
                }}
              />
              <Button
                onClick={() => {
                  setActions(actions.filter((_, i) => i !== index));
                }}
              >
                <Delete />
              </Button>
            </Box>
          ))}
          <Button onClick={addAction} startIcon={<Add />}>
            Add Action
          </Button>

          <Box mt={3}>
            <Button variant="contained" color="primary" onClick={saveRule}>
              Save Rule
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};
```

### Step 4: Real-Time Updates with WebSocket (Days 11-12)

#### Backend: WebSocket Configuration

**File: `src/main/java/com/ruleengine/config/WebSocketConfiguration.java`**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:3000")
            .withSockJS();
    }
}
```

**File: `src/main/java/com/ruleengine/websocket/RuleExecutionNotifier.java`**
```java
@Component
public class RuleExecutionNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyExecution(RuleResult result) {
        messagingTemplate.convertAndSend("/topic/executions", result);
    }
}
```

#### Frontend: WebSocket Client

**File: `frontend/rule-engine-ui/src/services/websocket.ts`**
```typescript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export class WebSocketService {
  private stompClient: any;

  connect(onExecutionUpdate: (result: any) => void) {
    const socket = new SockJS('http://localhost:8080/ws');
    this.stompClient = Stomp.over(socket);

    this.stompClient.connect({}, () => {
      this.stompClient.subscribe('/topic/executions', (message: any) => {
        const result = JSON.parse(message.body);
        onExecutionUpdate(result);
      });
    });
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
    }
  }
}
```

### Step 5: Authentication & Security (Days 13-15)

#### Backend: JWT Security

**File: `src/main/java/com/ruleengine/security/JwtTokenProvider.java`**
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(userPrincipal.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
```

### Step 6: Testing & Deployment (Days 16-18)

#### Frontend Tests
```bash
npm test
npm run build
```

#### Docker Deployment
**File: `docker-compose.yml`**
```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod

  frontend:
    build: ./frontend/rule-engine-ui
    ports:
      - "80:80"
    depends_on:
      - backend

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

---

## 🎨 UI Screenshots (Mock Concepts)

### Dashboard View
```
┌─────────────────────────────────────────────────────┐
│  📊 Rule Engine Dashboard                            │
├─────────────────────────────────────────────────────┤
│                                                       │
│  [Total Rules: 127] [Active: 98]  [Executions: 1.2M]│
│                                                       │
│  📈 Execution Trend (Last 24h)                      │
│  ┌──────────────────────────────────────┐           │
│  │         ╱╲    ╱╲                     │           │
│  │      ╱╲╱  ╲  ╱  ╲  ╱╲                │           │
│  │   ╱╲╱      ╲╱    ╲╱  ╲               │           │
│  └──────────────────────────────────────┘           │
│                                                       │
│  🔥 Top Rules by Execution                          │
│  1. fraud-detection-001    (15,234 exec)            │
│  2. dynamic-pricing-002    (12,891 exec)            │
│  3. eligibility-check-005   (9,456 exec)            │
│                                                       │
│  🕐 Recent Activity                                  │
│  • Rule "fraud-check" executed - MATCHED (2ms)      │
│  • Rule "pricing-rule" executed - NO_MATCH (1ms)    │
│  • New rule "compliance-01" created                  │
└─────────────────────────────────────────────────────┘
```

### Visual Rule Builder
```
┌─────────────────────────────────────────────────────┐
│  🎨 Create New Rule                                  │
├─────────────────────────────────────────────────────┤
│  Rule Name: [High Value Transaction Alert        ]  │
│                                                       │
│  📋 Conditions (IF)                                  │
│  ┌───────────────────────────────────────────────┐  │
│  │ Field: [transaction.amount    ]               │  │
│  │ Operator: [GREATER_THAN       ▼]              │  │
│  │ Value: [10000                 ]    [❌]        │  │
│  └───────────────────────────────────────────────┘  │
│  [+ Add Condition]                                   │
│                                                       │
│  ⚡ Actions (THEN)                                   │
│  ┌───────────────────────────────────────────────┐  │
│  │ Type: [SET_FACT              ▼]               │  │
│  │ Key: [alertLevel             ]                │  │
│  │ Value: [HIGH                 ]    [❌]         │  │
│  └───────────────────────────────────────────────┘  │
│  [+ Add Action]                                      │
│                                                       │
│  [Test Rule]  [Save]  [Cancel]                      │
└─────────────────────────────────────────────────────┘
```

---

## 📦 Dependencies to Add

### Backend (`build.gradle`)
```gradle
dependencies {
    // WebSocket support
    implementation 'org.springframework.boot:spring-boot-starter-websocket'

    // JWT authentication
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

    // Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // Redis for distributed cache
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### Frontend (`package.json`)
```json
{
  "dependencies": {
    "@mui/material": "^5.14.0",
    "@reduxjs/toolkit": "^1.9.5",
    "axios": "^1.5.0",
    "monaco-editor": "^0.44.0",
    "react-flow-renderer": "^10.3.17",
    "recharts": "^2.8.0",
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

---

## ✅ Checklist

### Backend Tasks
- [ ] Create `DashboardController` with metrics endpoints
- [ ] Create `WebSocketConfiguration` for real-time updates
- [ ] Implement `JwtTokenProvider` for authentication
- [ ] Add `UserDetailsService` for user management
- [ ] Create `SecurityConfiguration` with CORS
- [ ] Add WebSocket message broker
- [ ] Enhance `AuditLogger` with aggregation methods

### Frontend Tasks
- [ ] Initialize React + TypeScript project
- [ ] Set up Redux Toolkit for state management
- [ ] Create Dashboard page with metrics cards
- [ ] Build Visual Rule Builder component
- [ ] Implement Rule List with search/filter
- [ ] Create Test Playground with JSON editor
- [ ] Add WebSocket client for real-time updates
- [ ] Implement authentication flow (login/logout)
- [ ] Add routing with React Router
- [ ] Create responsive layout

### Testing Tasks
- [ ] Unit tests for new backend endpoints
- [ ] Integration tests for WebSocket
- [ ] Frontend component tests
- [ ] E2E tests with Cypress
- [ ] Performance testing (load test dashboard)

### Documentation Tasks
- [ ] API documentation for new endpoints
- [ ] UI user guide with screenshots
- [ ] Video tutorial for rule creation
- [ ] Deployment guide

---

## 🚀 Quick Commands

### Start Backend
```bash
cd /home/user/rule-engine
./gradlew bootRun
```

### Start Frontend
```bash
cd frontend/rule-engine-ui
npm start
```

### Access
- **Backend API**: http://localhost:8080
- **Frontend UI**: http://localhost:3000
- **WebSocket**: ws://localhost:8080/ws

---

## 📚 Resources

- **Material-UI Docs**: https://mui.com/
- **Redux Toolkit**: https://redux-toolkit.js.org/
- **Monaco Editor**: https://microsoft.github.io/monaco-editor/
- **React Flow**: https://reactflow.dev/
- **Spring WebSocket**: https://docs.spring.io/spring-framework/reference/web/websocket.html

---

**Once Phase 4 is complete, you'll have a production-ready Admin Dashboard that makes the rule engine accessible to both developers and business users!** 🎉
