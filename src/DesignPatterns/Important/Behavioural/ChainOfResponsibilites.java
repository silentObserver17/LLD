package DesignPatterns.Important.Behavioural;

abstract class ApprovalHandler {
    // Next handler in the chain
    private ApprovalHandler next;

    // Fluent setter — enables chain building
    public ApprovalHandler setNext(ApprovalHandler next) {
        this.next = next;
        return next; // return next so we can chain .setNext().setNext()
    }

    // Template for all handlers — process or forward
    public final void handle(Expense expense) {
        if (canApprove(expense)) {
            approve(expense);
        } else if (next != null) {
            System.out.println(getApproverName()
                    + ": Amount ₹" + expense.getAmount()
                    + " exceeds my limit. Escalating...");
            next.handle(expense);
        } else {
            System.out.println("No handler could approve expense #"
                    + expense.getId() + ". Rejected.");
        }
    }

    // Each handler defines its own approval limit
    protected abstract boolean canApprove(Expense expense);

    // Each handler defines its own approval action
    protected abstract void approve(Expense expense);

    // For logging
    protected abstract String getApproverName();
}

class CoordinatorHandler extends ApprovalHandler {

    private static final double LIMIT = 5_000.0;

    @Override
    protected boolean canApprove(Expense expense) {
        return expense.getAmount() < LIMIT;
    }

    @Override
    protected void approve(Expense expense) {
        expense.setStatus("APPROVED");
        System.out.println("✓ Coordinator approved expense #"
                + expense.getId()
                + " of ₹" + expense.getAmount());
    }

    @Override
    protected String getApproverName() { return "Coordinator"; }
}


class HODHandler extends ApprovalHandler {

    private static final double LIMIT = 20_000.0;

    @Override
    protected boolean canApprove(Expense expense) {
        return expense.getAmount() < LIMIT;
    }

    @Override
    protected void approve(Expense expense) {
        expense.setStatus("APPROVED");
        System.out.println("✓ HOD approved expense #"
                + expense.getId()
                + " of ₹" + expense.getAmount());
    }

    @Override
    protected String getApproverName() { return "HOD"; }
}

class FinanceHandler extends ApprovalHandler {

    private static final double LIMIT = 1_00_000.0;

    @Override
    protected boolean canApprove(Expense expense) {
        return expense.getAmount() < LIMIT;
    }

    @Override
    protected void approve(Expense expense) {
        expense.setStatus("APPROVED");
        System.out.println("✓ Finance approved expense #"
                + expense.getId()
                + " of ₹" + expense.getAmount());
    }

    @Override
    protected String getApproverName() { return "Finance"; }
}

class CEOHandler extends ApprovalHandler {

    @Override
    protected boolean canApprove(Expense expense) {
        return true; // CEO approves everything that reaches here
    }

    @Override
    protected void approve(Expense expense) {
        expense.setStatus("APPROVED");
        System.out.println("✓ CEO approved expense #"
                + expense.getId()
                + " of ₹" + expense.getAmount());
    }

    @Override
    protected String getApproverName() { return "CEO"; }
}

class ApprovalChainFactory {

    public static ApprovalHandler buildChain() {
        ApprovalHandler coordinator = new CoordinatorHandler();
        ApprovalHandler hod         = new HODHandler();
        ApprovalHandler finance     = new FinanceHandler();
        ApprovalHandler ceo         = new CEOHandler();

        // Wire the chain — setNext returns next so we can chain fluently
        coordinator
                .setNext(hod)
                .setNext(finance)
                .setNext(ceo);

        return coordinator; // return the head of the chain
    }
}

public class ChainOfResponsibilites {
    public static void main(String[] args) {
        ApprovalHandler chain = ApprovalChainFactory.buildChain();

        Expense e1 = new Expense(1, 101, "FOOD",      3_500.0);
        Expense e2 = new Expense(2, 102, "TRAVEL",   15_000.0);
        Expense e3 = new Expense(3, 103, "EQUIPMENT", 75_000.0);
        Expense e4 = new Expense(4, 104, "TRAVEL",  1_50_000.0);

        chain.handle(e1);
        System.out.println();
        chain.handle(e2);
        System.out.println();
        chain.handle(e3);
        System.out.println();
        chain.handle(e4);

        // Output:
        // ✓ Coordinator approved expense #1 of ₹3500.0

        // Coordinator: Amount ₹15000.0 exceeds my limit. Escalating...
        // ✓ HOD approved expense #2 of ₹15000.0

        // Coordinator: Amount ₹75000.0 exceeds my limit. Escalating...
        // HOD: Amount ₹75000.0 exceeds my limit. Escalating...
        // ✓ Finance approved expense #3 of ₹75000.0

        // Coordinator: Amount ₹150000.0 exceeds my limit. Escalating...
        // HOD: Amount ₹150000.0 exceeds my limit. Escalating...
        // Finance: Amount ₹150000.0 exceeds my limit. Escalating...
        // ✓ CEO approved expense #4 of ₹150000.0

    }
}
