package DesignPatterns.Important.Behavioural;

import java.util.*;

interface CommandInteface{
    void execute();
}

class Light{
    public void turnOn() {
        System.out.println("Light is ON");
    }

    public void turnOff() {
        System.out.println("Light is OFF");
    }
}

class TurnOnLightCommand implements CommandInteface {
    private Light light;

    public TurnOnLightCommand(Light light) {
        this.light = light;
    }

    public void execute() {
        light.turnOn();
    }
}

class TurnOffLightCommand implements CommandInteface {
    private Light light;

    public TurnOffLightCommand(Light light) {
        this.light = light;
    }

    public void execute() {
        light.turnOff();
    }
}

class RemoteControl {
    private CommandInteface command;

    public void setCommand(CommandInteface command) {
        this.command = command;
    }

    public void pressButton(){
        command.execute();
    }
}

// XXXXXXXXXXXXXXXXXXXXXXXXXX SECOND COMMAND EXAMPLE XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

interface CCommand{
    public void execute();
    public void undo();
}

class EmailService {
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Email sent to " + to + " | Subject: " + subject);
    }

    public void cancelEmail(String to, String subject) {
        System.out.println("Email cancelled to " + to + " | Subject: " + subject);
    }
}

class InventoryService {
    private int stock = 100;

    public void reserveStock(int productId, int quantity) {
        stock -= quantity;
        System.out.println("Stock reserved: " + quantity
                + " units of product " + productId
                + " | Remaining: " + stock);
    }

    public void releaseStock(int productId, int quantity) {
        stock += quantity;
        System.out.println("Stock released: " + quantity
                + " units of product " + productId
                + " | Remaining: " + stock);
    }
}

class InvoiceService {
    public void generateInvoice(int orderId) {
        System.out.println("Invoice generated for order #" + orderId);
    }

    public void cancelInvoice(int orderId) {
        System.out.println("Invoice cancelled for order #" + orderId);
    }
}

class SendEmailCommand implements CCommand {
    private final EmailService emailService; // receiver
    private final String to;
    private final String subject;
    private final String body;

    public SendEmailCommand(EmailService emailService, String to, String subject, String body) {
        this.emailService = emailService;
        this.to      = to;
        this.subject = subject;
        this.body    = body;
    }

    @Override
    public void execute() {
        emailService.sendEmail(to, subject, body);
    }

    @Override
    public void undo() {
        emailService.cancelEmail(to, subject);
    }
}


class ReserveStockCommand implements CCommand {
    private final InventoryService inventoryService;
    private final int productId;
    private final int quantity;

    public ReserveStockCommand(InventoryService inventoryService,
                               int productId, int quantity) {
        this.inventoryService = inventoryService;
        this.productId = productId;
        this.quantity  = quantity;
    }

    @Override
    public void execute() {
        inventoryService.releaseStock(productId, quantity);
    }

    @Override
    public void undo() {
        inventoryService.releaseStock(productId, quantity);
    }
}

class GenerateInvoiceCommand implements CCommand {

    private final InvoiceService invoiceService;
    private final int orderId;

    public GenerateInvoiceCommand(InvoiceService invoiceService, int orderId) {
        this.invoiceService = invoiceService;
        this.orderId = orderId;
    }

    @Override
    public void execute() {
        invoiceService.generateInvoice(orderId);
    }

    @Override
    public void undo() {
        invoiceService.cancelInvoice(orderId);
    }
}

class CommandInvoker {
    private final Deque<CCommand> history = new ArrayDeque<>();

    public void execute(CCommand command) {
        command.execute();
        history.push(command);
    }

    public void undo(){
        if (history.isEmpty()) {
            System.out.println("Nothing to undo");
            return;
        }
        CCommand last = history.pop();
        last.undo();
    }

    public void undoAll() {
        System.out.println("Undoing all " + history.size() + " commands...");
        while (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}

//Step 5 — Macro Command — compose multiple commands into one:
class MacroCommand implements CCommand {

    private final List<CCommand> commands;

    public MacroCommand(List<CCommand> commands) {
        this.commands = new ArrayList<>(commands);
    }

    @Override
    public void execute() {
        commands.forEach(CCommand::execute);
    }

    @Override
    public void undo() {
        // Undo in reverse order
        ListIterator<CCommand> it = commands.listIterator(commands.size());
        while (it.hasPrevious()) {
            it.previous().undo();
        }
    }
}

public class Command {
    public static void main(String[] args) {
        Light light = new Light();

        CommandInteface onCommand = new TurnOnLightCommand(light);
        CommandInteface offCommand = new TurnOffLightCommand(light);

        RemoteControl remote = new RemoteControl();

        remote.setCommand(onCommand);
        remote.pressButton();

        remote.setCommand(offCommand);
        remote.pressButton();

//        --------------------------------------------------------------------
        // Receivers
        EmailService emailService         = new EmailService();
        InventoryService inventoryService = new InventoryService();
        InvoiceService invoiceService     = new InvoiceService();

        // Invoker
        CommandInvoker invoker = new CommandInvoker();

        // Build commands
        CCommand sendEmail     = new SendEmailCommand(emailService,
                "user@example.com", "Order Confirmed", "Your order #7823...");
        CCommand reserveStock  = new ReserveStockCommand(inventoryService, 42, 3);
        CCommand genInvoice    = new GenerateInvoiceCommand(invoiceService, 7823);


        // Execute individually
        System.out.println("── Executing commands ──");
        invoker.execute(sendEmail);
        invoker.execute(reserveStock);
        invoker.execute(genInvoice);

        // Output:
        // Email sent to user@example.com | Subject: Order Confirmed
        // Stock reserved: 3 units of product 42 | Remaining: 97
        // Invoice generated for order #7823

        System.out.println("\n── Undoing last command ──");
        invoker.undo();
        // Invoice cancelled for order #7823

        System.out.println("\n── Undoing all ──");
        invoker.undoAll();
        // Stock released: 3 units of product 42 | Remaining: 100
        // Email cancelled to user@example.com | Subject: Order Confirmed

        // ── Macro Command ──
        System.out.println("\n── Order placement macro ──");
        CCommand orderMacro = new MacroCommand(List.of(
                new ReserveStockCommand(inventoryService, 42, 2),
                new SendEmailCommand(emailService, "jane@example.com", "Order placed", "..."),
                new GenerateInvoiceCommand(invoiceService, 7824)
        ));

        invoker.execute(orderMacro);
        // Stock reserved: 2 units of product 42 | Remaining: 98
        // Email sent to jane@example.com | Subject: Order placed
        // Invoice generated for order #7824

        System.out.println("\n── Undoing macro ──");
        invoker.undo(); // undoes entire macro in reverse
        // Invoice cancelled for order #7824
        // Email cancelled to jane@example.com | Subject: Order placed
        // Stock released: 2 units of product 42 | Remaining: 100

    }
}


