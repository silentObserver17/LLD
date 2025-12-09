package SolidPrinciples;

// A class should have only one reason to change.
// In other words a class should have only one job and one responsibility

// 1. Only responsible for handling Employee Data.
class Employee{
    private String name;
    private double baseSalary;
    private int hoursWorked;

    public Employee(String name, double baseSalary, int hoursWorked){
        this.name = name;
        this.baseSalary = baseSalary;
        this.hoursWorked = hoursWorked;
    }

    public String getName(){return name;}
    public double getBaseSalary(){return baseSalary;}
    public int getHoursWorked(){return hoursWorked;}
}

// 2. Only responsible for payroll calculation
class PayrollCalculator{
    public double calculatePay(Employee employee){
        return employee.getBaseSalary() * employee.getHoursWorked();
    }
}

// 3. Only responsible for persistence
class EmployeeRepository{
    public void save(Employee employee){
        System.out.println("Saving" + employee.getName() + " to database...");
    }
}

//4. only responsible for Generating Report.
class EmployeeReportGenerator{
    public void generateReport(Employee employee, PayrollCalculator calculator){
        double pay = calculator.calculatePay(employee);
        System.out.println("Report: "+ employee.getName() + " earned $" + pay);
    }
}

public class SingleResponsibilityPrinciple {
    public static void main(String[] args){
        Employee emp = new Employee("Alice", 50.0, 40);

        PayrollCalculator payroll = new PayrollCalculator();
        EmployeeRepository repo = new EmployeeRepository();
        EmployeeReportGenerator reporter = new EmployeeReportGenerator();

        double pay = payroll.calculatePay(emp);
        repo.save(emp);
        reporter.generateReport(emp, payroll);
    }
}
