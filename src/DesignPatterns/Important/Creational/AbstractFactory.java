package DesignPatterns.Important.Creational;

interface Button{
    void render();
    void onClick();
}

interface Checkbox{
    void render();
    void onCheck();
}

// Windows Family
class WindowsButton implements Button{
    @Override public void render()   { System.out.println("Rendering Windows Button [ OK ]"); }
    @Override public void onClick()  { System.out.println("Windows Button clicked"); }
}

class WindowsCheckbox implements Checkbox {
    @Override public void render()   { System.out.println("Rendering Windows Checkbox [x]"); }
    @Override public void onCheck()  { System.out.println("Windows Checkbox checked"); }
}

// Mac family
class MacButton implements Button {
    @Override public void render()   { System.out.println("Rendering Mac Button (rounded)"); }
    @Override public void onClick()  { System.out.println("Mac Button clicked"); }
}

class MacCheckbox implements Checkbox {
    @Override public void render()   { System.out.println("Rendering Mac Checkbox ◉"); }
    @Override public void onCheck()  { System.out.println("Mac Checkbox checked"); }
}

interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

class WindowsFactory implements UIFactory {
    @Override
    public Button createButton()     { return new WindowsButton(); }

    @Override
    public Checkbox createCheckbox() { return new WindowsCheckbox(); }
}

class MacFactory implements UIFactory {
    @Override
    public Button createButton()     { return new MacButton(); }

    @Override
    public Checkbox createCheckbox() { return new MacCheckbox(); }
}

class Application {
    private final Button button;
    private final Checkbox checkbox;

    // Receives factory via constructor — Dependency Injection
    public Application(UIFactory factory) {
        this.button   = factory.createButton();
        this.checkbox = factory.createCheckbox();
    }

    public void renderUI() {
        button.render();
        checkbox.render();
    }
}

public class AbstractFactory {
    public static void main(String[] args) {
        UIFactory factory;

        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){
            factory = new WindowsFactory();
        }else{
            factory = new MacFactory();
        }

        Application app = new Application(factory);
        app.renderUI();
    }
}
