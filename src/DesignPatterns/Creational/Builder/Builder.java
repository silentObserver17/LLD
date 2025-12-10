package DesignPatterns.Creational.Builder;

/*
* The Builder Pattern is a creational design pattern that helps us to construct complex objects step by step.
* It especially shines when object has too many optional parameters or when we want to make our creation readable and immutable.
* */

/*
Benefits

- Immutable objects (thread-safe, safer)
- Very readable client code (fluent interface)
- Single responsibility: construction logic is separated
- Easy to enforce required fields
- Can add validation in build() method
*/

class Computer{
    // Required Parameters
    private final String CPU;
    private final String RAM;

    // Optional Parameters
    private final String storage;
    private final String GPU;
    private final boolean bluetooth;
    private final boolean wifi;

    private Computer(ComputerBuilder builder){
        this.CPU = builder.CPU;
        this.RAM = builder.RAM;
        this.storage = builder.storage;
        this.GPU = builder.GPU;
        this.bluetooth = builder.bluetooth;
        this.wifi = builder.wifi;
    }

    // Only Getters(Immutable Object)
    public String getCPU() {
        return CPU;
    }

    public boolean isWifi() {
        return wifi;
    }

    public boolean isBluetooth() {
        return bluetooth;
    }

    public String getGPU() {
        return GPU;
    }

    public String getStorage() {
        return storage;
    }

    public String getRAM() {
        return RAM;
    }

    public static class ComputerBuilder{
        // Required parameters
        private final String CPU;
        private final String RAM;

        // Optional parameters - initialized to defaults
        private String storage = "256GB SSD";
        private String GPU = "Integrated";
        private boolean bluetooth = true;
        private boolean wifi = true;

        public ComputerBuilder(String CPU, String RAM){
            this.CPU = CPU;
            this.RAM = RAM;
        }

        public ComputerBuilder storage(String storage){
            this.storage = storage;
            return this;
        }

        public ComputerBuilder GPU(String GPU){
            this.GPU = GPU;
            return this;
        }

        public ComputerBuilder bluetooth(boolean bluetooth){
            this.bluetooth = bluetooth;
            return this;
        }

        public ComputerBuilder wifi(boolean wifi){
            this.wifi = wifi;
            return this;
        }

        // Final build() method that creates the actual object
        public Computer build(){
            return new Computer(this);
        }
    }
}

public class Builder {
    public static void main(String[] args){
        Computer gamingPC = new Computer.ComputerBuilder("Intel i9", "32GB")
                .GPU("RTX 5090")
                .storage("2TB NVMe SSD")
                .bluetooth(false)
                .build();

        System.out.println(gamingPC.getCPU());
        System.out.println(gamingPC.getGPU());
    }
}
