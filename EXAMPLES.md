# Rule Engine Examples

Comprehensive examples demonstrating various use cases.

## 1. Banking & Payments

### Fraud Detection

```json
{
  "id": "fraud-001",
  "name": "Suspicious Transaction Pattern",
  "ruleGroup": "fraud-detection",
  "priority": 100,
  "conditions": [
    {
      "field": "transaction.amount",
      "operator": "GREATER_THAN",
      "value": 5000
    },
    {
      "field": "transaction.country",
      "operator": "NOT_EQUALS",
      "value": "user.homeCountry"
    },
    {
      "expression": "transaction.timestamp - user.lastTransaction < 300"
    }
  ],
  "conditionLogic": "AND",
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "fraudScore",
      "factValue": 95
    },
    {
      "type": "TRIGGER_RULE",
      "ruleId": "block-transaction"
    }
  ]
}
```

### Credit Card Limit Check

```json
{
  "id": "credit-limit-001",
  "name": "Credit Card Over Limit",
  "conditions": [
    {
      "expression": "transaction.amount + card.currentBalance > card.creditLimit"
    }
  ],
  "actions": [
    {
      "type": "ENRICH_RESULT",
      "metadata": {
        "decision": "DECLINE",
        "reason": "Credit limit exceeded"
      }
    }
  ]
}
```

## 2. E-Commerce

### Dynamic Pricing

```json
{
  "id": "pricing-001",
  "name": "Black Friday Discount",
  "contextConstraints": {
    "channel": "web",
    "event": "black-friday"
  },
  "conditions": [
    {
      "field": "cart.total",
      "operator": "GREATER_THAN",
      "value": 200
    }
  ],
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "discount",
      "valueExpression": "cart.total * 0.25"
    },
    {
      "type": "SET_FACT",
      "factKey": "discountCode",
      "factValue": "BF2024"
    }
  ]
}
```

### Abandoned Cart

```json
{
  "id": "abandoned-cart-001",
  "name": "Abandoned Cart Reminder",
  "temporalWindow": {
    "type": "SLIDING",
    "windowDuration": "PT24H"
  },
  "conditions": [
    {
      "field": "cart.items",
      "operator": "IS_EMPTY",
      "value": false
    },
    {
      "expression": "now() - cart.lastUpdated > 7200"
    }
  ],
  "actions": [
    {
      "type": "EMIT_EVENT",
      "eventTopic": "marketing-automation",
      "eventPayload": {
        "type": "SEND_EMAIL",
        "template": "abandoned-cart-reminder"
      }
    }
  ]
}
```

## 3. Insurance

### Risk Assessment

```json
{
  "id": "insurance-risk-001",
  "name": "Auto Insurance Risk Calculator",
  "conditions": [
    {
      "field": "driver.age",
      "operator": "LESS_THAN",
      "value": 25
    },
    {
      "field": "driver.accidents",
      "operator": "GREATER_THAN",
      "value": 2
    },
    {
      "field": "vehicle.type",
      "operator": "IN",
      "value": ["SPORTS_CAR", "LUXURY"]
    }
  ],
  "conditionLogic": "OR",
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "riskCategory",
      "factValue": "HIGH"
    },
    {
      "type": "SET_FACT",
      "factKey": "premiumMultiplier",
      "factValue": 1.8
    }
  ]
}
```

## 4. Healthcare

### Patient Triage

```json
{
  "id": "triage-001",
  "name": "Emergency Triage - Critical",
  "priority": 100,
  "conditions": [
    {
      "nestedConditions": [
        {
          "field": "patient.temperature",
          "operator": "GREATER_THAN",
          "value": 39.5
        },
        {
          "field": "patient.heartRate",
          "operator": "GREATER_THAN",
          "value": 120
        },
        {
          "field": "patient.respiratoryRate",
          "operator": "GREATER_THAN",
          "value": 25
        }
      ],
      "logicalOperator": "OR"
    }
  ],
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "triageLevel",
      "factValue": "CRITICAL"
    },
    {
      "type": "SET_FACT",
      "factKey": "priority",
      "factValue": 1
    }
  ]
}
```

## 5. IoT & Smart Home

### Temperature Control

```json
{
  "id": "smart-home-001",
  "name": "Auto Temperature Adjustment",
  "conditions": [
    {
      "expression": "sensor.temperature > settings.maxTemp || sensor.temperature < settings.minTemp"
    },
    {
      "field": "settings.autoMode",
      "operator": "EQUALS",
      "value": true
    }
  ],
  "actions": [
    {
      "type": "CALL_SERVICE",
      "serviceUrl": "http://hvac-controller/adjust",
      "serviceMethod": "POST",
      "serviceParams": {
        "targetTemp": "settings.targetTemp",
        "mode": "auto"
      }
    }
  ]
}
```

## 6. Workflow Automation

### Approval Workflow

```json
{
  "id": "approval-001",
  "name": "Purchase Order Approval",
  "priority": 50,
  "conditions": [
    {
      "field": "order.amount",
      "operator": "BETWEEN",
      "value": [1000, 10000]
    }
  ],
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "approvalLevel",
      "factValue": "MANAGER"
    },
    {
      "type": "TRIGGER_RULE",
      "ruleId": "notify-manager"
    }
  ],
  "triggers": ["notify-manager"]
}
```

### Escalation Rule

```json
{
  "id": "escalation-001",
  "name": "Support Ticket Escalation",
  "temporalWindow": {
    "type": "SLIDING",
    "windowDuration": "PT4H"
  },
  "conditions": [
    {
      "field": "ticket.priority",
      "operator": "EQUALS",
      "value": "HIGH"
    },
    {
      "expression": "now() - ticket.createdAt > 14400"
    },
    {
      "field": "ticket.status",
      "operator": "EQUALS",
      "value": "OPEN"
    }
  ],
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "escalate",
      "factValue": true
    },
    {
      "type": "EMIT_EVENT",
      "eventTopic": "support-escalation",
      "eventPayload": {
        "ticketId": "ticket.id",
        "escalateTo": "SENIOR_SUPPORT"
      }
    }
  ]
}
```

## 7. Multi-Tenant SaaS

### Tenant-Specific Rules

```json
{
  "id": "tenant-limit-001",
  "name": "API Rate Limit - Premium Tier",
  "tenant": "tenant-abc",
  "contextConstraints": {
    "tier": "PREMIUM"
  },
  "temporalWindow": {
    "type": "TUMBLING",
    "windowDuration": "PT1M"
  },
  "conditions": [
    {
      "expression": "count(api.requests) > 10000"
    }
  ],
  "actions": [
    {
      "type": "SET_FACT",
      "factKey": "rateLimitExceeded",
      "factValue": true
    }
  ]
}
```

## 8. Complex Rule Chaining

### Multi-Step Validation

```json
[
  {
    "id": "validation-step-1",
    "name": "Basic Validation",
    "priority": 100,
    "conditions": [
      {
        "field": "application.email",
        "operator": "MATCHES",
        "value": "^[A-Za-z0-9+_.-]+@(.+)$"
      }
    ],
    "actions": [
      {
        "type": "SET_FACT",
        "factKey": "basicValidationPassed",
        "factValue": true
      }
    ],
    "triggers": ["validation-step-2"]
  },
  {
    "id": "validation-step-2",
    "name": "Email Domain Check",
    "dependsOn": ["validation-step-1"],
    "priority": 90,
    "conditions": [
      {
        "field": "basicValidationPassed",
        "operator": "EQUALS",
        "value": true
      },
      {
        "field": "application.emailDomain",
        "operator": "NOT_IN",
        "value": ["spam.com", "disposable.com"]
      }
    ],
    "actions": [
      {
        "type": "SET_FACT",
        "factKey": "emailDomainValid",
        "factValue": true
      }
    ],
    "triggers": ["validation-step-3"]
  },
  {
    "id": "validation-step-3",
    "name": "Final Approval",
    "dependsOn": ["validation-step-2"],
    "priority": 80,
    "conditions": [
      {
        "field": "emailDomainValid",
        "operator": "EQUALS",
        "value": true
      }
    ],
    "actions": [
      {
        "type": "SET_FACT",
        "factKey": "applicationApproved",
        "factValue": true
      }
    ]
  }
]
```

## Testing Examples

### Using REST API

```bash
# Test fraud detection rule
curl -X POST http://localhost:8080/api/rules/fraud-001/execute \
  -H "Content-Type: application/json" \
  -d '{
    "transaction": {
      "amount": 8000,
      "country": "BR",
      "timestamp": 1699999999
    },
    "user": {
      "homeCountry": "US",
      "lastTransaction": 1699999700
    }
  }'

# Test without saving
curl -X POST http://localhost:8080/api/rules/test \
  -H "Content-Type: application/json" \
  -d '{
    "rule": {
      "name": "Test Rule",
      "conditions": [
        {
          "field": "amount",
          "operator": "GREATER_THAN",
          "value": 1000
        }
      ],
      "actions": []
    },
    "facts": {
      "transaction": {
        "amount": 1500
      }
    }
  }'
```

### Using Java API

```java
@Autowired
private RuleService ruleService;

// Create and execute rule
Rule rule = Rule.builder()
    .name("My Rule")
    .conditions(List.of(
        Condition.builder()
            .field("transaction.amount")
            .operator(Condition.Operator.GREATER_THAN)
            .value(1000)
            .build()
    ))
    .build();

RuleContext context = RuleContext.withFact("transaction",
    Map.of("amount", 1500));

RuleResult result = ruleService.testRule(rule, context);

System.out.println("Matched: " + result.isMatched());
System.out.println("Execution time: " + result.getExecutionTimeMs() + "ms");
```
