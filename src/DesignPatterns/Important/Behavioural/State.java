package DesignPatterns.Important.Behavioural;

interface ExpenseState {
    void submit(ExpenseContext context);
    void approve(ExpenseContext context, String approverRole);
    void reject(ExpenseContext context, String reason);
    void resubmit(ExpenseContext context);
    String getStateName();
}

class ExpenseContext {
    private final int expenseId;
    private final double amount;
    private ExpenseState currentState;
    private String rejectionReason;

    public ExpenseContext(int expenseId, double amount) {
        this.expenseId    = expenseId;
        this.amount       = amount;
        this.currentState = new PendingState(); // initial state
    }

    // Delegates all behavior to current state
    public void submit() {
        currentState.submit(this);
    }

    public void approve(String approverRole) {
        currentState.approve(this, approverRole);
    }

    public void reject(String reason) {
        currentState.reject(this, reason);
    }

    public void resubmit() {
        currentState.resubmit(this);
    }

    // State objects call this to transition
    public void setState(ExpenseState state) {
        System.out.println("  State: " + currentState.getStateName()
                + " → " + state.getStateName());
        this.currentState = state;
    }

    public String getCurrentStateName() { return currentState.getStateName(); }
    public int getExpenseId()           { return expenseId; }
    public double getAmount()           { return amount; }

    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
    public String getRejectionReason()            { return rejectionReason; }
}

// ── PENDING ────────────────────────────────────────────────
class PendingState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("Expense #" + context.getExpenseId()
                + " submitted for coordinator review");
        context.setState(new CoordinatorReviewState());
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        System.out.println("✗ Cannot approve — expense not yet submitted");
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ Cannot reject — expense not yet submitted");
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("✗ Cannot resubmit — expense was never rejected");
    }

    @Override
    public String getStateName() { return "PENDING"; }
}

// ── COORDINATOR REVIEW ─────────────────────────────────────
class CoordinatorReviewState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("✗ Already submitted");
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        if (!approverRole.equals("COORDINATOR")) {
            System.out.println("✗ Only Coordinator can approve at this stage");
            return;
        }
        System.out.println("✓ Coordinator approved expense #" + context.getExpenseId());

        // Route based on amount — State drives the workflow
        if (context.getAmount() < 20_000) {
            System.out.println("  Amount under ₹20,000 — fully approved");
            context.setState(new ApprovedState());
        } else {
            System.out.println("  Amount over ₹20,000 — escalating to HOD");
            context.setState(new HODReviewState());
        }
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ Coordinator rejected expense #"
                + context.getExpenseId() + ". Reason: " + reason);
        context.setRejectionReason(reason);
        context.setState(new RejectedState());
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("✗ Cannot resubmit — expense is under review");
    }

    @Override
    public String getStateName() { return "COORDINATOR_REVIEW"; }
}

// ── HOD REVIEW ─────────────────────────────────────────────
class HODReviewState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("✗ Already submitted");
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        if (!approverRole.equals("HOD")) {
            System.out.println("✗ Only HOD can approve at this stage");
            return;
        }
        System.out.println("✓ HOD approved expense #" + context.getExpenseId());

        if (context.getAmount() < 1_00_000) {
            System.out.println("  Amount under ₹1,00,000 — fully approved");
            context.setState(new ApprovedState());
        } else {
            System.out.println("  Amount over ₹1,00,000 — escalating to Finance");
            context.setState(new FinanceReviewState());
        }
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ HOD rejected expense #"
                + context.getExpenseId() + ". Reason: " + reason);
        context.setRejectionReason(reason);
        context.setState(new RejectedState());
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("✗ Cannot resubmit — expense is under review");
    }

    @Override
    public String getStateName() { return "HOD_REVIEW"; }
}

// ── FINANCE REVIEW ─────────────────────────────────────────
class FinanceReviewState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("✗ Already submitted");
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        if (!approverRole.equals("FINANCE")) {
            System.out.println("✗ Only Finance can approve at this stage");
            return;
        }
        System.out.println("✓ Finance approved expense #"
                + context.getExpenseId() + " — fully approved!");
        context.setState(new ApprovedState());
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ Finance rejected expense #"
                + context.getExpenseId() + ". Reason: " + reason);
        context.setRejectionReason(reason);
        context.setState(new RejectedState());
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("✗ Cannot resubmit — expense is under review");
    }

    @Override
    public String getStateName() { return "FINANCE_REVIEW"; }
}

// ── APPROVED ───────────────────────────────────────────────
class ApprovedState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("✗ Already approved");
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        System.out.println("✗ Already approved");
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ Cannot reject an already approved expense");
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("✗ Cannot resubmit an approved expense");
    }

    @Override
    public String getStateName() { return "APPROVED"; }
}

// ── REJECTED ───────────────────────────────────────────────
class RejectedState implements ExpenseState {

    @Override
    public void submit(ExpenseContext context) {
        System.out.println("✗ Cannot submit a rejected expense — resubmit instead");
    }

    @Override
    public void approve(ExpenseContext context, String approverRole) {
        System.out.println("✗ Cannot approve a rejected expense");
    }

    @Override
    public void reject(ExpenseContext context, String reason) {
        System.out.println("✗ Already rejected");
    }

    @Override
    public void resubmit(ExpenseContext context) {
        System.out.println("Expense #" + context.getExpenseId()
                + " resubmitted after rejection");
        context.setState(new PendingState());
    }

    @Override
    public String getStateName() { return "REJECTED"; }
}

public class State {
    public static void main(String[] args){
        System.out.println("═══ Scenario 1: Small Expense (₹3,500) ═══");
        ExpenseContext expense1 = new ExpenseContext(1, 3_500.0);
        expense1.submit();
        expense1.approve("COORDINATOR");

        // Output:
        // Expense #1 submitted for coordinator review
        //   State: PENDING → COORDINATOR_REVIEW
        // ✓ Coordinator approved expense #1
        //   Amount under ₹20,000 — fully approved
        //   State: COORDINATOR_REVIEW → APPROVED

        // ── Scenario 2: Large expense — full chain
        System.out.println("\n═══ Scenario 2: Large Expense (₹1,50,000) ═══");
        ExpenseContext expense2 = new ExpenseContext(2, 1_50_000.0);
        expense2.submit();
        expense2.approve("COORDINATOR");
        expense2.approve("HOD");
        expense2.approve("FINANCE");

        // Output:
        // Expense #2 submitted for coordinator review
        //   State: PENDING → COORDINATOR_REVIEW
        // ✓ Coordinator approved expense #2
        //   Amount over ₹20,000 — escalating to HOD
        //   State: COORDINATOR_REVIEW → HOD_REVIEW
        // ✓ HOD approved expense #2
        //   Amount over ₹1,00,000 — escalating to Finance
        //   State: HOD_REVIEW → FINANCE_REVIEW
        // ✓ Finance approved expense #2 — fully approved!
        //   State: FINANCE_REVIEW → APPROVED

        // ── Scenario 3: Rejected then resubmitted
        System.out.println("\n═══ Scenario 3: Rejection and Resubmit ═══");
        ExpenseContext expense3 = new ExpenseContext(3, 8_000.0);
        expense3.submit();
        expense3.reject("Missing receipts");
        expense3.resubmit();
        expense3.submit();
        expense3.approve("COORDINATOR");

        // Output:
        // Expense #3 submitted for coordinator review
        //   State: PENDING → COORDINATOR_REVIEW
        // ✗ Coordinator rejected expense #3. Reason: Missing receipts
        //   State: COORDINATOR_REVIEW → REJECTED
        // Expense #3 resubmitted after rejection
        //   State: REJECTED → PENDING
        // Expense #3 submitted for coordinator review
        //   State: PENDING → COORDINATOR_REVIEW
        // ✓ Coordinator approved expense #3
        //   Amount under ₹20,000 — fully approved
        //   State: COORDINATOR_REVIEW → APPROVED


        // ── Scenario 4: Invalid transitions
        System.out.println("\n═══ Scenario 4: Invalid transitions ═══");
        ExpenseContext expense4 = new ExpenseContext(4, 5_000.0);
        expense4.approve("COORDINATOR"); // not submitted yet
        expense4.submit();
        expense4.approve("HOD");         // wrong role for this stage
        expense4.approve("COORDINATOR"); // correct role

        // Output:
        // ✗ Cannot approve — expense not yet submitted
        // Expense #4 submitted for coordinator review
        //   State: PENDING → COORDINATOR_REVIEW
        // ✗ Only Coordinator can approve at this stage
        // ✓ Coordinator approved expense #4
        //   Amount under ₹20,000 — fully approved
        //   State: COORDINATOR_REVIEW → APPROVED
    }
}
