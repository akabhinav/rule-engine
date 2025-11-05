package com.ruleengine.demo;

import com.ruleengine.model.Action;
import com.ruleengine.model.Condition;
import com.ruleengine.model.Rule;
import com.ruleengine.service.RuleService;
import com.ruleengine.demo.DemoUseCase.DemoScenario;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing demo use cases
 */
@Service
public class DemoUseCaseService {

    private final RuleService ruleService;
    private final Map<String, DemoUseCase> useCases;

    public DemoUseCaseService(RuleService ruleService) {
        this.ruleService = ruleService;
        this.useCases = initializeUseCases();
    }

    public List<DemoUseCase> getAllUseCases() {
        return new ArrayList<>(useCases.values());
    }

    public DemoUseCase getUseCase(String id) {
        return useCases.get(id);
    }

    public List<Rule> loadUseCaseRules(String useCaseId) {
        DemoUseCase useCase = useCases.get(useCaseId);
        if (useCase == null) {
            throw new IllegalArgumentException("Use case not found: " + useCaseId);
        }

        List<Rule> rules = createRulesForUseCase(useCaseId);
        rules.forEach(ruleService::createRule);
        return rules;
    }

    private Map<String, DemoUseCase> initializeUseCases() {
        Map<String, DemoUseCase> cases = new LinkedHashMap<>();

        // Complex Use Cases
        cases.put("fraud-detection", createFraudDetectionUseCase());
        cases.put("dynamic-pricing", createDynamicPricingUseCase());
        cases.put("insurance-premium", createInsurancePremiumUseCase());
        cases.put("loan-approval", createLoanApprovalUseCase());
        cases.put("supply-chain", createSupplyChainUseCase());

        // Medium Use Cases
        cases.put("ecommerce-discount", createEcommerceDiscountUseCase());
        cases.put("loyalty-program", createLoyaltyProgramUseCase());
        cases.put("sla-monitoring", createSLAMonitoringUseCase());

        return cases;
    }

    // ========== Complex Use Case #1: Fraud Detection ==========
    private DemoUseCase createFraudDetectionUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Normal Transaction",
                "Regular purchase from known location",
                Map.of(
                    "transaction", Map.of(
                        "amount", 45.99,
                        "merchantCategory", "retail",
                        "country", "US"
                    ),
                    "customer", Map.of(
                        "accountAge", 730,
                        "averageTransaction", 50.0,
                        "location", "US",
                        "riskScore", 0.15
                    ),
                    "device", Map.of(
                        "fingerprint", "known-device-123",
                        "ipAddress", "192.168.1.1"
                    )
                ),
                "Transaction approved - Low risk"
            ),
            new DemoScenario(
                "High-Risk Transaction",
                "Large amount from new location with high velocity",
                Map.of(
                    "transaction", Map.of(
                        "amount", 5000.0,
                        "merchantCategory", "electronics",
                        "country", "NG"
                    ),
                    "customer", Map.of(
                        "accountAge", 10,
                        "averageTransaction", 30.0,
                        "location", "US",
                        "riskScore", 0.85,
                        "transactionsLast24h", 12
                    ),
                    "device", Map.of(
                        "fingerprint", "unknown-device-xyz",
                        "ipAddress", "41.20.45.67"
                    )
                ),
                "Transaction blocked - High fraud risk detected"
            ),
            new DemoScenario(
                "Suspicious Velocity",
                "Multiple transactions in short time",
                Map.of(
                    "transaction", Map.of(
                        "amount", 99.99,
                        "merchantCategory", "retail",
                        "country", "US"
                    ),
                    "customer", Map.of(
                        "accountAge", 365,
                        "averageTransaction", 50.0,
                        "location", "US",
                        "riskScore", 0.35,
                        "transactionsLast24h", 15
                    ),
                    "device", Map.of(
                        "fingerprint", "known-device-456",
                        "ipAddress", "192.168.1.50"
                    )
                ),
                "Transaction flagged for review - Unusual velocity"
            )
        );

        return new DemoUseCase(
            "fraud-detection",
            "Fraud Detection System",
            "Multi-layered fraud detection with ML risk scoring, velocity checks, geolocation analysis, and device fingerprinting",
            "complex",
            "🛡️",
            "#e74c3c",
            "fraud-detection",
            scenarios,
            Map.of(
                "transaction.amount", "Transaction amount in USD",
                "customer.riskScore", "ML-based risk score (0-1)",
                "customer.transactionsLast24h", "Number of transactions in last 24 hours",
                "device.fingerprint", "Unique device identifier"
            )
        );
    }

    // ========== Complex Use Case #2: Dynamic Pricing ==========
    private DemoUseCase createDynamicPricingUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Peak Demand Pricing",
                "High demand with low inventory",
                Map.of(
                    "product", Map.of(
                        "basePrice", 100.0,
                        "category", "electronics",
                        "inventoryLevel", 5
                    ),
                    "market", Map.of(
                        "demand", 0.95,
                        "competitorPrice", 115.0,
                        "seasonalFactor", 1.2
                    ),
                    "customer", Map.of(
                        "segment", "premium",
                        "lifetimeValue", 5000.0,
                        "pricesSensitivity", 0.3
                    )
                ),
                "Price increased to $125 - High demand premium"
            ),
            new DemoScenario(
                "Clearance Pricing",
                "High inventory with low demand",
                Map.of(
                    "product", Map.of(
                        "basePrice", 100.0,
                        "category", "clothing",
                        "inventoryLevel", 500
                    ),
                    "market", Map.of(
                        "demand", 0.2,
                        "competitorPrice", 75.0,
                        "seasonalFactor", 0.7
                    ),
                    "customer", Map.of(
                        "segment", "bargain",
                        "lifetimeValue", 500.0,
                        "priceSensitivity", 0.8
                    )
                ),
                "Price reduced to $65 - Clearance discount"
            ),
            new DemoScenario(
                "Premium Customer Pricing",
                "VIP customer with moderate demand",
                Map.of(
                    "product", Map.of(
                        "basePrice", 100.0,
                        "category", "electronics",
                        "inventoryLevel", 50
                    ),
                    "market", Map.of(
                        "demand", 0.6,
                        "competitorPrice", 98.0,
                        "seasonalFactor", 1.0
                    ),
                    "customer", Map.of(
                        "segment", "vip",
                        "lifetimeValue", 15000.0,
                        "priceSensitivity", 0.2
                    )
                ),
                "Price set to $92 - VIP discount applied"
            )
        );

        return new DemoUseCase(
            "dynamic-pricing",
            "Dynamic Pricing Engine",
            "Real-time pricing optimization based on demand, inventory, competition, customer segments, and market conditions",
            "complex",
            "💰",
            "#f39c12",
            "dynamic-pricing",
            scenarios,
            Map.of(
                "market.demand", "Market demand score (0-1)",
                "product.inventoryLevel", "Current stock level",
                "customer.segment", "Customer segment (premium/regular/bargain/vip)",
                "market.competitorPrice", "Competitor pricing"
            )
        );
    }

    // ========== Complex Use Case #3: Insurance Premium ==========
    private DemoUseCase createInsurancePremiumUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Low-Risk Driver",
                "Experienced driver with clean record",
                Map.of(
                    "driver", Map.of(
                        "age", 35,
                        "yearsLicensed", 15,
                        "accidents", 0,
                        "violations", 0,
                        "creditScore", 750
                    ),
                    "vehicle", Map.of(
                        "value", 25000.0,
                        "safetyRating", 5,
                        "theftRating", "low",
                        "ageYears", 2
                    ),
                    "location", Map.of(
                        "zipCode", "12345",
                        "crimeRate", 0.05,
                        "region", "suburban"
                    )
                ),
                "Premium: $650/year - Excellent driver discount"
            ),
            new DemoScenario(
                "High-Risk Driver",
                "Young driver with accidents",
                Map.of(
                    "driver", Map.of(
                        "age", 19,
                        "yearsLicensed", 1,
                        "accidents", 2,
                        "violations", 3,
                        "creditScore", 620
                    ),
                    "vehicle", Map.of(
                        "value", 40000.0,
                        "safetyRating", 3,
                        "theftRating", "high",
                        "ageYears", 0
                    ),
                    "location", Map.of(
                        "zipCode", "90210",
                        "crimeRate", 0.25,
                        "region", "urban"
                    )
                ),
                "Premium: $3200/year - High risk surcharge"
            ),
            new DemoScenario(
                "Moderate-Risk Driver",
                "Middle-aged driver with minor violation",
                Map.of(
                    "driver", Map.of(
                        "age", 45,
                        "yearsLicensed", 25,
                        "accidents", 1,
                        "violations", 1,
                        "creditScore", 700
                    ),
                    "vehicle", Map.of(
                        "value", 30000.0,
                        "safetyRating", 4,
                        "theftRating", "medium",
                        "ageYears", 3
                    ),
                    "location", Map.of(
                        "zipCode", "60614",
                        "crimeRate", 0.12,
                        "region", "urban"
                    )
                ),
                "Premium: $1400/year - Standard rate"
            )
        );

        return new DemoUseCase(
            "insurance-premium",
            "Insurance Premium Calculator",
            "Comprehensive risk assessment for auto insurance with driver history, vehicle safety, location factors, and claims prediction",
            "complex",
            "🚗",
            "#9b59b6",
            "insurance-premium",
            scenarios,
            Map.of(
                "driver.age", "Driver age in years",
                "driver.accidents", "Number of accidents in last 5 years",
                "vehicle.safetyRating", "NHTSA safety rating (1-5)",
                "location.crimeRate", "Area crime rate (0-1)"
            )
        );
    }

    // ========== Complex Use Case #4: Loan Approval ==========
    private DemoUseCase createLoanApprovalUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Excellent Candidate",
                "High income, excellent credit, low DTI",
                Map.of(
                    "applicant", Map.of(
                        "creditScore", 780,
                        "annualIncome", 120000.0,
                        "employmentYears", 8,
                        "age", 35
                    ),
                    "loan", Map.of(
                        "amount", 250000.0,
                        "term", 30,
                        "purpose", "home-purchase"
                    ),
                    "financial", Map.of(
                        "debtToIncome", 0.25,
                        "savings", 80000.0,
                        "monthlyDebts", 2500.0
                    )
                ),
                "Approved - Premium rate 3.5%"
            ),
            new DemoScenario(
                "Denied Application",
                "Poor credit and high debt",
                Map.of(
                    "applicant", Map.of(
                        "creditScore", 580,
                        "annualIncome", 45000.0,
                        "employmentYears", 1,
                        "age", 25
                    ),
                    "loan", Map.of(
                        "amount", 300000.0,
                        "term", 30,
                        "purpose", "home-purchase"
                    ),
                    "financial", Map.of(
                        "debtToIncome", 0.55,
                        "savings", 5000.0,
                        "monthlyDebts", 2000.0
                    )
                ),
                "Denied - Credit score and DTI too high"
            ),
            new DemoScenario(
                "Conditional Approval",
                "Good credit but higher DTI",
                Map.of(
                    "applicant", Map.of(
                        "creditScore", 720,
                        "annualIncome", 85000.0,
                        "employmentYears", 5,
                        "age", 40
                    ),
                    "loan", Map.of(
                        "amount", 350000.0,
                        "term", 30,
                        "purpose", "home-purchase"
                    ),
                    "financial", Map.of(
                        "debtToIncome", 0.42,
                        "savings", 40000.0,
                        "monthlyDebts", 3000.0
                    )
                ),
                "Conditional approval - Rate 4.25%, requires additional documentation"
            )
        );

        return new DemoUseCase(
            "loan-approval",
            "Loan Approval Workflow",
            "Automated loan decisioning with credit scoring, income verification, debt-to-income analysis, and risk assessment",
            "complex",
            "🏦",
            "#27ae60",
            "loan-approval",
            scenarios,
            Map.of(
                "applicant.creditScore", "FICO credit score (300-850)",
                "financial.debtToIncome", "Debt-to-income ratio (0-1)",
                "loan.amount", "Requested loan amount",
                "applicant.employmentYears", "Years at current employer"
            )
        );
    }

    // ========== Complex Use Case #5: Supply Chain ==========
    private DemoUseCase createSupplyChainUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Low Inventory Alert",
                "Stock running low with high demand",
                Map.of(
                    "inventory", Map.of(
                        "currentLevel", 50,
                        "reorderPoint", 100,
                        "averageDailyDemand", 15.0,
                        "leadTimeDays", 7
                    ),
                    "supplier", Map.of(
                        "reliability", 0.95,
                        "deliveryTime", 5,
                        "costPerUnit", 10.0,
                        "rating", 4.5
                    ),
                    "demand", Map.of(
                        "forecast7Days", 120,
                        "growthRate", 0.15,
                        "seasonality", 1.2
                    )
                ),
                "Action: Urgent reorder 200 units - Stock critically low"
            ),
            new DemoScenario(
                "Supplier Switch Recommendation",
                "Current supplier underperforming",
                Map.of(
                    "inventory", Map.of(
                        "currentLevel", 200,
                        "reorderPoint", 100,
                        "averageDailyDemand", 10.0,
                        "leadTimeDays", 14
                    ),
                    "supplier", Map.of(
                        "reliability", 0.65,
                        "deliveryTime", 15,
                        "costPerUnit", 12.0,
                        "rating", 2.5,
                        "lateDeliveries", 5
                    ),
                    "demand", Map.of(
                        "forecast7Days", 75,
                        "growthRate", 0.05,
                        "seasonality", 1.0
                    )
                ),
                "Action: Switch to alternate supplier - Current supplier unreliable"
            ),
            new DemoScenario(
                "Optimal Inventory",
                "Well-stocked with reliable supplier",
                Map.of(
                    "inventory", Map.of(
                        "currentLevel", 500,
                        "reorderPoint", 150,
                        "averageDailyDemand", 20.0,
                        "leadTimeDays", 5
                    ),
                    "supplier", Map.of(
                        "reliability", 0.98,
                        "deliveryTime", 4,
                        "costPerUnit", 9.5,
                        "rating", 4.8
                    ),
                    "demand", Map.of(
                        "forecast7Days", 140,
                        "growthRate", 0.08,
                        "seasonality", 1.0
                    )
                ),
                "Status: Inventory optimal - No action needed"
            )
        );

        return new DemoUseCase(
            "supply-chain",
            "Supply Chain Optimization",
            "Intelligent inventory management with demand forecasting, supplier performance tracking, and automated reordering",
            "complex",
            "📦",
            "#3498db",
            "supply-chain",
            scenarios,
            Map.of(
                "inventory.currentLevel", "Current stock level",
                "demand.forecast7Days", "Predicted demand for next 7 days",
                "supplier.reliability", "Supplier reliability score (0-1)",
                "inventory.leadTimeDays", "Days to receive new stock"
            )
        );
    }

    // ========== Medium Use Case #6: E-commerce Discount ==========
    private DemoUseCase createEcommerceDiscountUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "First-time Customer",
                "New customer with cart above threshold",
                Map.of(
                    "cart", Map.of(
                        "total", 75.0,
                        "itemCount", 3,
                        "category", "electronics"
                    ),
                    "customer", Map.of(
                        "firstPurchase", true,
                        "membershipTier", "none",
                        "totalSpent", 0.0
                    ),
                    "promotion", Map.of(
                        "code", "WELCOME20",
                        "active", true
                    )
                ),
                "15% first-time customer discount + free shipping"
            ),
            new DemoScenario(
                "Bulk Purchase",
                "Large order with multiple items",
                Map.of(
                    "cart", Map.of(
                        "total", 500.0,
                        "itemCount", 12,
                        "category", "office-supplies"
                    ),
                    "customer", Map.of(
                        "firstPurchase", false,
                        "membershipTier", "gold",
                        "totalSpent", 2500.0
                    ),
                    "promotion", Map.of(
                        "code", "BULK10",
                        "active", true
                    )
                ),
                "20% bulk discount + 10% loyalty discount"
            ),
            new DemoScenario(
                "Regular Purchase",
                "Standard order under promotion threshold",
                Map.of(
                    "cart", Map.of(
                        "total", 35.0,
                        "itemCount", 2,
                        "category", "books"
                    ),
                    "customer", Map.of(
                        "firstPurchase", false,
                        "membershipTier", "silver",
                        "totalSpent", 500.0
                    ),
                    "promotion", Map.of(
                        "code", "",
                        "active", false
                    )
                ),
                "5% loyalty discount applied"
            )
        );

        return new DemoUseCase(
            "ecommerce-discount",
            "E-commerce Discount Engine",
            "Cart-based promotional rules with loyalty tiers, seasonal campaigns, and dynamic coupon validation",
            "medium",
            "🛒",
            "#16a085",
            "ecommerce-discount",
            scenarios,
            Map.of(
                "cart.total", "Cart total amount",
                "customer.membershipTier", "Loyalty tier (none/silver/gold/platinum)",
                "promotion.code", "Promo code applied",
                "cart.itemCount", "Number of items in cart"
            )
        );
    }

    // ========== Medium Use Case #7: Loyalty Program ==========
    private DemoUseCase createLoyaltyProgramUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Tier Upgrade",
                "Customer reaching gold tier threshold",
                Map.of(
                    "customer", Map.of(
                        "currentTier", "silver",
                        "points", 5500,
                        "yearlySpending", 5000.0,
                        "transactionCount", 25
                    ),
                    "transaction", Map.of(
                        "amount", 150.0,
                        "category", "premium"
                    )
                ),
                "Upgraded to Gold tier - 200 bonus points awarded"
            ),
            new DemoScenario(
                "Points Multiplier Event",
                "Premium category during bonus period",
                Map.of(
                    "customer", Map.of(
                        "currentTier", "platinum",
                        "points", 12000,
                        "yearlySpending", 15000.0,
                        "transactionCount", 50
                    ),
                    "transaction", Map.of(
                        "amount", 200.0,
                        "category", "premium",
                        "bonusEvent", true
                    )
                ),
                "Earned 800 points (4x multiplier for platinum + bonus event)"
            ),
            new DemoScenario(
                "Standard Points Accrual",
                "Regular transaction for silver member",
                Map.of(
                    "customer", Map.of(
                        "currentTier", "silver",
                        "points", 2000,
                        "yearlySpending", 2000.0,
                        "transactionCount", 15
                    ),
                    "transaction", Map.of(
                        "amount", 50.0,
                        "category", "regular"
                    )
                ),
                "Earned 100 points (2x multiplier for silver)"
            )
        );

        return new DemoUseCase(
            "loyalty-program",
            "Customer Loyalty Tier System",
            "Points-based loyalty program with automatic tier upgrades, spending rewards, and special event bonuses",
            "medium",
            "⭐",
            "#e67e22",
            "loyalty-program",
            scenarios,
            Map.of(
                "customer.currentTier", "Current loyalty tier",
                "customer.points", "Accumulated loyalty points",
                "customer.yearlySpending", "Total spending this year",
                "transaction.category", "Transaction category (regular/premium)"
            )
        );
    }

    // ========== Medium Use Case #8: SLA Monitoring ==========
    private DemoUseCase createSLAMonitoringUseCase() {
        var scenarios = List.of(
            new DemoScenario(
                "Critical SLA Breach",
                "P1 incident exceeding response time",
                Map.of(
                    "incident", Map.of(
                        "priority", "P1",
                        "responseTimeMinutes", 25,
                        "age", 90,
                        "status", "open"
                    ),
                    "sla", Map.of(
                        "responseTimeTarget", 15,
                        "resolutionTimeTarget", 60,
                        "tier", "enterprise"
                    ),
                    "customer", Map.of(
                        "accountValue", 500000.0,
                        "contractType", "enterprise"
                    )
                ),
                "CRITICAL: SLA breached - Escalate to VP, notify customer"
            ),
            new DemoScenario(
                "Warning Threshold",
                "P2 incident approaching SLA limit",
                Map.of(
                    "incident", Map.of(
                        "priority", "P2",
                        "responseTimeMinutes", 35,
                        "age", 180,
                        "status", "in-progress"
                    ),
                    "sla", Map.of(
                        "responseTimeTarget", 60,
                        "resolutionTimeTarget", 240,
                        "tier", "professional"
                    ),
                    "customer", Map.of(
                        "accountValue", 50000.0,
                        "contractType", "professional"
                    )
                ),
                "WARNING: 75% of SLA time used - Assign senior engineer"
            ),
            new DemoScenario(
                "Within SLA",
                "P3 incident with good progress",
                Map.of(
                    "incident", Map.of(
                        "priority", "P3",
                        "responseTimeMinutes", 60,
                        "age", 300,
                        "status", "in-progress"
                    ),
                    "sla", Map.of(
                        "responseTimeTarget", 240,
                        "resolutionTimeTarget", 480,
                        "tier", "standard"
                    ),
                    "customer", Map.of(
                        "accountValue", 10000.0,
                        "contractType", "standard"
                    )
                ),
                "OK: Within SLA - Continue normal processing"
            )
        );

        return new DemoUseCase(
            "sla-monitoring",
            "SLA Monitoring & Alerts",
            "Service level agreement tracking with automated escalations, priority-based routing, and breach prevention",
            "medium",
            "⏱️",
            "#c0392b",
            "sla-monitoring",
            scenarios,
            Map.of(
                "incident.priority", "Incident priority (P1/P2/P3/P4)",
                "incident.responseTimeMinutes", "Time since incident created",
                "sla.responseTimeTarget", "SLA response time target in minutes",
                "customer.contractType", "Customer contract tier"
            )
        );
    }

    // ========== Rule Creation Methods ==========

    private List<Rule> createRulesForUseCase(String useCaseId) {
        return switch (useCaseId) {
            case "fraud-detection" -> createFraudDetectionRules();
            case "dynamic-pricing" -> createDynamicPricingRules();
            case "insurance-premium" -> createInsurancePremiumRules();
            case "loan-approval" -> createLoanApprovalRules();
            case "supply-chain" -> createSupplyChainRules();
            case "ecommerce-discount" -> createEcommerceDiscountRules();
            case "loyalty-program" -> createLoyaltyProgramRules();
            case "sla-monitoring" -> createSLAMonitoringRules();
            default -> List.of();
        };
    }

    private List<Rule> createFraudDetectionRules() {
        return List.of(
            Rule.builder()
                .id("fraud-1")
                .name("Block High-Risk Transactions")
                .description("Block transactions with ML risk score above 0.7")
                .group("fraud-detection")
                .priority(1)
                .condition(Condition.builder()
                    .expression("customer.riskScore > 0.7")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "BLOCKED"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "High fraud risk score"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("fraud-2")
                .name("Suspicious Velocity Check")
                .description("Flag accounts with excessive transactions")
                .group("fraud-detection")
                .priority(2)
                .condition(Condition.builder()
                    .expression("customer.transactionsLast24h > 10")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "REVIEW"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "Unusual transaction velocity"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("fraud-3")
                .name("New Account High Amount")
                .description("Review new accounts with large transactions")
                .group("fraud-detection")
                .priority(3)
                .condition(Condition.builder()
                    .expression("customer.accountAge < 30 && transaction.amount > 1000")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "REVIEW"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "New account with large transaction"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("fraud-4")
                .name("Geolocation Mismatch")
                .description("Flag transactions from different country")
                .group("fraud-detection")
                .priority(4)
                .condition(Condition.builder()
                    .expression("customer.location != transaction.country")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "geoMismatch",
                            "value", "true"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("fraud-5")
                .name("Approve Low-Risk Transactions")
                .description("Auto-approve low risk transactions")
                .group("fraud-detection")
                .priority(10)
                .condition(Condition.builder()
                    .expression("customer.riskScore < 0.3 && transaction.amount < 100")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "APPROVED"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "Low risk - auto approved"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createDynamicPricingRules() {
        return List.of(
            Rule.builder()
                .id("pricing-1")
                .name("Peak Demand Premium")
                .description("Increase price during high demand")
                .group("dynamic-pricing")
                .priority(1)
                .condition(Condition.builder()
                    .expression("market.demand > 0.8 && product.inventoryLevel < 20")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "priceMultiplier",
                            "value", "1.25"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pricingReason",
                            "value", "High demand premium"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("pricing-2")
                .name("Clearance Pricing")
                .description("Deep discount for excess inventory")
                .group("dynamic-pricing")
                .priority(2)
                .condition(Condition.builder()
                    .expression("product.inventoryLevel > 200 && market.demand < 0.3")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "priceMultiplier",
                            "value", "0.65"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pricingReason",
                            "value", "Clearance sale"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("pricing-3")
                .name("VIP Customer Discount")
                .description("Special pricing for VIP customers")
                .group("dynamic-pricing")
                .priority(3)
                .condition(Condition.builder()
                    .expression("customer.segment == 'vip' && customer.lifetimeValue > 10000")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "priceMultiplier",
                            "value", "0.92"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pricingReason",
                            "value", "VIP customer discount"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("pricing-4")
                .name("Competitive Pricing")
                .description("Match or beat competitor prices")
                .group("dynamic-pricing")
                .priority(4)
                .condition(Condition.builder()
                    .expression("market.competitorPrice < product.basePrice")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "matchCompetitor",
                            "value", "true"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("pricing-5")
                .name("Seasonal Adjustment")
                .description("Apply seasonal pricing factors")
                .group("dynamic-pricing")
                .priority(5)
                .condition(Condition.builder()
                    .expression("market.seasonalFactor != 1.0")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "seasonalAdjustment",
                            "value", "true"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createInsurancePremiumRules() {
        return List.of(
            Rule.builder()
                .id("insurance-1")
                .name("Excellent Driver Discount")
                .description("Discount for experienced drivers with clean record")
                .group("insurance-premium")
                .priority(1)
                .condition(Condition.builder()
                    .expression("driver.accidents == 0 && driver.violations == 0 && driver.yearsLicensed > 10")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "premiumMultiplier",
                            "value", "0.75"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "discount",
                            "value", "Excellent driver discount"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("insurance-2")
                .name("Young Driver Surcharge")
                .description("Higher premium for young inexperienced drivers")
                .group("insurance-premium")
                .priority(2)
                .condition(Condition.builder()
                    .expression("driver.age < 25 && driver.yearsLicensed < 3")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "premiumMultiplier",
                            "value", "2.5"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "surcharge",
                            "value", "Young driver surcharge"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("insurance-3")
                .name("Accident History Penalty")
                .description("Increase premium based on accidents")
                .group("insurance-premium")
                .priority(3)
                .condition(Condition.builder()
                    .expression("driver.accidents > 0")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "accidentPenalty",
                            "value", "true"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("insurance-4")
                .name("High-Risk Location")
                .description("Surcharge for high crime areas")
                .group("insurance-premium")
                .priority(4)
                .condition(Condition.builder()
                    .expression("location.crimeRate > 0.2")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "locationSurcharge",
                            "value", "1.3"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("insurance-5")
                .name("Safety Features Discount")
                .description("Discount for high safety rating vehicles")
                .group("insurance-premium")
                .priority(5)
                .condition(Condition.builder()
                    .expression("vehicle.safetyRating >= 4")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "safetyDiscount",
                            "value", "0.9"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createLoanApprovalRules() {
        return List.of(
            Rule.builder()
                .id("loan-1")
                .name("Auto-Approve Premium")
                .description("Auto approve excellent credit with low DTI")
                .group("loan-approval")
                .priority(1)
                .condition(Condition.builder()
                    .expression("applicant.creditScore >= 750 && financial.debtToIncome < 0.3 && applicant.employmentYears >= 5")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "APPROVED"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "rate",
                            "value", "3.5"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "tier",
                            "value", "Premium"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loan-2")
                .name("Deny Poor Credit")
                .description("Auto deny poor credit scores")
                .group("loan-approval")
                .priority(2)
                .condition(Condition.builder()
                    .expression("applicant.creditScore < 620")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "DENIED"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "Credit score below minimum"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loan-3")
                .name("Deny High DTI")
                .description("Deny applications with excessive debt")
                .group("loan-approval")
                .priority(3)
                .condition(Condition.builder()
                    .expression("financial.debtToIncome > 0.5")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "DENIED"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "Debt-to-income ratio too high"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loan-4")
                .name("Conditional Approval")
                .description("Conditional approval for moderate risk")
                .group("loan-approval")
                .priority(4)
                .condition(Condition.builder()
                    .expression("applicant.creditScore >= 680 && applicant.creditScore < 750 && financial.debtToIncome < 0.45")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "decision",
                            "value", "CONDITIONAL"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "rate",
                            "value", "4.25"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "conditions",
                            "value", "Additional documentation required"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loan-5")
                .name("Income Verification")
                .description("Flag for manual review if income ratio unusual")
                .group("loan-approval")
                .priority(5)
                .condition(Condition.builder()
                    .expression("loan.amount > (applicant.annualIncome * 4)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "requiresReview",
                            "value", "true"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reviewReason",
                            "value", "Loan amount high relative to income"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createSupplyChainRules() {
        return List.of(
            Rule.builder()
                .id("supply-1")
                .name("Critical Stock Alert")
                .description("Urgent reorder when stock critically low")
                .group("supply-chain")
                .priority(1)
                .condition(Condition.builder()
                    .expression("inventory.currentLevel < (demand.forecast7Days * 0.5)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "action",
                            "value", "URGENT_REORDER"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "priority",
                            "value", "CRITICAL"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("supply-2")
                .name("Reorder Point Trigger")
                .description("Standard reorder when below reorder point")
                .group("supply-chain")
                .priority(2)
                .condition(Condition.builder()
                    .expression("inventory.currentLevel <= inventory.reorderPoint")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "action",
                            "value", "REORDER"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("supply-3")
                .name("Unreliable Supplier Alert")
                .description("Recommend supplier switch for poor performance")
                .group("supply-chain")
                .priority(3)
                .condition(Condition.builder()
                    .expression("supplier.reliability < 0.8 || supplier.rating < 3.0")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "action",
                            "value", "SWITCH_SUPPLIER"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "reason",
                            "value", "Current supplier unreliable"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("supply-4")
                .name("Demand Surge Detection")
                .description("Increase order quantity for growing demand")
                .group("supply-chain")
                .priority(4)
                .condition(Condition.builder()
                    .expression("demand.growthRate > 0.1 && demand.seasonality > 1.1")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "orderMultiplier",
                            "value", "1.5"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("supply-5")
                .name("Optimal Inventory Status")
                .description("No action needed when inventory optimal")
                .group("supply-chain")
                .priority(10)
                .condition(Condition.builder()
                    .expression("inventory.currentLevel > (demand.forecast7Days * 2)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "status",
                            "value", "OPTIMAL"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createEcommerceDiscountRules() {
        return List.of(
            Rule.builder()
                .id("discount-1")
                .name("First Purchase Discount")
                .description("15% discount for first-time customers")
                .group("ecommerce-discount")
                .priority(1)
                .condition(Condition.builder()
                    .expression("customer.firstPurchase == true && cart.total > 50")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "discount",
                            "value", "15"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "discountReason",
                            "value", "First-time customer welcome"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("discount-2")
                .name("Bulk Purchase Discount")
                .description("20% discount for large orders")
                .group("ecommerce-discount")
                .priority(2)
                .condition(Condition.builder()
                    .expression("cart.itemCount >= 10 || cart.total > 300")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "discount",
                            "value", "20"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "discountReason",
                            "value", "Bulk purchase discount"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("discount-3")
                .name("Gold Member Discount")
                .description("10% discount for gold tier members")
                .group("ecommerce-discount")
                .priority(3)
                .condition(Condition.builder()
                    .expression("customer.membershipTier == 'gold'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "loyaltyDiscount",
                            "value", "10"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("discount-4")
                .name("Silver Member Discount")
                .description("5% discount for silver tier members")
                .group("ecommerce-discount")
                .priority(4)
                .condition(Condition.builder()
                    .expression("customer.membershipTier == 'silver'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "loyaltyDiscount",
                            "value", "5"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("discount-5")
                .name("Free Shipping")
                .description("Free shipping for orders over $75")
                .group("ecommerce-discount")
                .priority(5)
                .condition(Condition.builder()
                    .expression("cart.total >= 75")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "freeShipping",
                            "value", "true"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createLoyaltyProgramRules() {
        return List.of(
            Rule.builder()
                .id("loyalty-1")
                .name("Platinum Tier Points")
                .description("4x points for platinum members")
                .group("loyalty-program")
                .priority(1)
                .condition(Condition.builder()
                    .expression("customer.currentTier == 'platinum'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pointsMultiplier",
                            "value", "4"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loyalty-2")
                .name("Gold Tier Points")
                .description("3x points for gold members")
                .group("loyalty-program")
                .priority(2)
                .condition(Condition.builder()
                    .expression("customer.currentTier == 'gold'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pointsMultiplier",
                            "value", "3"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loyalty-3")
                .name("Silver Tier Points")
                .description("2x points for silver members")
                .group("loyalty-program")
                .priority(3)
                .condition(Condition.builder()
                    .expression("customer.currentTier == 'silver'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "pointsMultiplier",
                            "value", "2"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loyalty-4")
                .name("Upgrade to Gold")
                .description("Upgrade to gold tier at 5000 points")
                .group("loyalty-program")
                .priority(4)
                .condition(Condition.builder()
                    .expression("customer.points >= 5000 && customer.currentTier == 'silver'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "tierUpgrade",
                            "value", "gold"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "bonusPoints",
                            "value", "200"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("loyalty-5")
                .name("Bonus Event Multiplier")
                .description("Double points during bonus events")
                .group("loyalty-program")
                .priority(5)
                .condition(Condition.builder()
                    .expression("transaction.bonusEvent == true")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "bonusMultiplier",
                            "value", "2"
                        ))
                        .build()
                ))
                .build()
        );
    }

    private List<Rule> createSLAMonitoringRules() {
        return List.of(
            Rule.builder()
                .id("sla-1")
                .name("Critical P1 SLA Breach")
                .description("Escalate P1 incidents exceeding SLA")
                .group("sla-monitoring")
                .priority(1)
                .condition(Condition.builder()
                    .expression("incident.priority == 'P1' && incident.responseTimeMinutes > sla.responseTimeTarget")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "alert",
                            "value", "CRITICAL"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "escalateTo",
                            "value", "VP Engineering"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "notifyCustomer",
                            "value", "true"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("sla-2")
                .name("SLA Warning Threshold")
                .description("Warn when 75% of SLA time consumed")
                .group("sla-monitoring")
                .priority(2)
                .condition(Condition.builder()
                    .expression("incident.responseTimeMinutes > (sla.responseTimeTarget * 0.75)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "alert",
                            "value", "WARNING"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "action",
                            "value", "Assign senior engineer"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("sla-3")
                .name("Enterprise SLA Priority")
                .description("Prioritize enterprise customer incidents")
                .group("sla-monitoring")
                .priority(3)
                .condition(Condition.builder()
                    .expression("customer.contractType == 'enterprise' && incident.priority == 'P1'")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "priority",
                            "value", "HIGHEST"
                        ))
                        .build(),
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "assignTo",
                            "value", "Enterprise support team"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("sla-4")
                .name("Resolution Time Warning")
                .description("Alert when approaching resolution deadline")
                .group("sla-monitoring")
                .priority(4)
                .condition(Condition.builder()
                    .expression("incident.age > (sla.resolutionTimeTarget * 0.8)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "resolutionWarning",
                            "value", "true"
                        ))
                        .build()
                ))
                .build(),

            Rule.builder()
                .id("sla-5")
                .name("Within SLA")
                .description("Normal processing for incidents within SLA")
                .group("sla-monitoring")
                .priority(10)
                .condition(Condition.builder()
                    .expression("incident.responseTimeMinutes <= (sla.responseTimeTarget * 0.5)")
                    .build())
                .actions(List.of(
                    Action.builder()
                        .type("SET_VARIABLE")
                        .parameters(Map.of(
                            "name", "status",
                            "value", "OK"
                        ))
                        .build()
                ))
                .build()
        );
    }
}
