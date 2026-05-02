package DesignPatterns.Important.Behavioural;

import java.util.List;

interface ApprovalStrategy {
    void approve(Expense expense);
    boolean canHandle(Expense expense); // helps with strategy selection
}

class Expense {
    private final int id;
    private final int userId;
    private final String type;
    private final double amount;
    private String status;

    public Expense(int id, int userId, String type, double amount) {
        this.id     = id;
        this.userId = userId;
        this.type   = type;
        this.amount = amount;
        this.status = "PENDING";
    }

    // getters + setters
    public int getId()          { return id; }
    public int getUserId()      { return userId; }
    public String getType()     { return type; }
    public double getAmount()   { return amount; }
    public String getStatus()   { return status; }
    public void setStatus(String status) { this.status = status; }
}

class TravelApprovalStrategy implements ApprovalStrategy {

    @Override
    public void approve(Expense expense) {
        System.out.println("Travel Strategy: Processing expense #" + expense.getId());

        if (expense.getAmount() < 5000) {
            System.out.println("  → Auto-approved by Coordinator (amount < ₹5000)");
            expense.setStatus("APPROVED_BY_COORDINATOR");

        } else if (expense.getAmount() < 20000) {
            System.out.println("  → Escalated to HOD (₹5000–₹20000)");
            expense.setStatus("PENDING_HOD_APPROVAL");

        } else {
            System.out.println("  → Escalated to Finance (amount > ₹20000)");
            expense.setStatus("PENDING_FINANCE_APPROVAL");
        }
    }

    @Override
    public boolean canHandle(Expense expense) {
        return expense.getType().equals("TRAVEL");
    }
}

class FoodApprovalStrategy implements ApprovalStrategy {

    @Override
    public void approve(Expense expense) {
        System.out.println("Food Strategy: Processing expense #" + expense.getId());

        if (expense.getAmount() < 1000) {
            System.out.println("  → Auto-approved (food under ₹1000)");
            expense.setStatus("AUTO_APPROVED");
        } else {
            System.out.println("  → Sent to Coordinator for approval");
            expense.setStatus("PENDING_COORDINATOR_APPROVAL");
        }
    }

    @Override
    public boolean canHandle(Expense expense) {
        return expense.getType().equals("FOOD");
    }
}

class EquipmentApprovalStrategy implements ApprovalStrategy {

    @Override
    public void approve(Expense expense) {
        System.out.println("Equipment Strategy: Processing expense #" + expense.getId());
        // Equipment always needs HOD + Finance regardless of amount
        System.out.println("  → Requires HOD approval first");
        System.out.println("  → Then Finance sign-off");
        expense.setStatus("PENDING_HOD_APPROVAL");
    }

    @Override
    public boolean canHandle(Expense expense) {
        return expense.getType().equals("EQUIPMENT");
    }
}

class ExpenseService {

    private ApprovalStrategy strategy;

    // Strategy injected — context doesn't pick it
    public ExpenseService(ApprovalStrategy strategy) {
        this.strategy = strategy;
    }

    // Can be swapped at runtime
    public void setStrategy(ApprovalStrategy strategy) {
        this.strategy = strategy;
    }

    public void processApproval(Expense expense) {
        System.out.println("\nProcessing: " + expense.getType()
                + " expense of ₹" + expense.getAmount());
        strategy.approve(expense);
        System.out.println("Status: " + expense.getStatus());
    }
}

class ApprovalStrategyResolver {

    private final List<ApprovalStrategy> strategies;

    public ApprovalStrategyResolver() {
        this.strategies = List.of(
                new TravelApprovalStrategy(),
                new FoodApprovalStrategy(),
                new EquipmentApprovalStrategy()
        );
    }

    public ApprovalStrategy resolve(Expense expense) {
        return strategies.stream()
                .filter(s -> s.canHandle(expense))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No strategy found for expense type: " + expense.getType()
                ));
    }
}


public class Strategy {
    public static void main(String[] args) {
        ApprovalStrategyResolver resolver = new ApprovalStrategyResolver();

        Expense travel = new Expense(1, 101, "TRAVEL", 3500.0);
        Expense food   = new Expense(2, 102, "FOOD",   800.0);
        Expense equip  = new Expense(3, 103, "EQUIPMENT", 45000.0);

        ExpenseService service = new ExpenseService(resolver.resolve(travel));
        service.processApproval(travel);

        service.setStrategy(resolver.resolve(food));
        service.processApproval(food);

        service.setStrategy(resolver.resolve(equip));
        service.processApproval(equip);

    }
}
