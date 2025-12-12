package DesignPatterns.Structural.Composition;
/*
*  Composite is a structural design pattern that lets us treat individual objects and compositions of objects uniformly.
* We can work with single object and whole tree of object using exactly same code.
* Real-world analogy:
A file system

- A File is a leaf (no children)
- A Folder can contain files and other folders
- We call getSize() on a file → returns its size
- We call getSize() on a folder → returns sum of everything inside (recursively)
* */

import java.util.ArrayList;
import java.util.List;

// 1. Component – common interface
interface FileSystemComponent{
    long getSize();
    String getName();
    void display(String indent);
}

// 2. Leaf - cannot have children.
class File implements FileSystemComponent{
    private final String name;
    private final long size;

    public File(String name, long size){
        this.name = name;
        this.size = size;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "File: " + name + " (" + size + " KB)");
    }
}

// 3. Composite - can have children.
class Directory implements FileSystemComponent {
    private final String name;
    private final List<FileSystemComponent> children = new ArrayList<>();

    public Directory(String name){
        this.name = name;
    }

    public void add(FileSystemComponent component) {
        children.add(component);
    }

    public void remove(FileSystemComponent component) {
        children.remove(component);
    }

    @Override
    public long getSize() {
        long total = 0;
        for (FileSystemComponent child : children) {
            total += child.getSize();
        }
        return total;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "Directory: " + name + " [Size: " + getSize() + " KB]");
        for (FileSystemComponent child : children) {
            child.display(indent + "  ");
        }
    }
}

public class Composition {
    static void printSize(FileSystemComponent component) {
        System.out.println(component.getName() + " size = " + component.getSize() + " KB");
    }

    public static void main(String[] args){
        // Create files
        FileSystemComponent report = new File("report.pdf", 2048);
        FileSystemComponent photo = new File("vacation.jpg", 5120);
        FileSystemComponent code = new File("app.java", 120);

        // Create folders
        Directory docs = new Directory("Documents");
        Directory pics = new Directory("Pictures");
        Directory src = new Directory("Source");

        // Build tree
        docs.add(report);
        pics.add(photo);
        src.add(code);

        Directory home = new Directory("Home");
        home.add(docs);
        home.add(pics);
        home.add(src);

        // Client uses same method everywhere!
        System.out.println("Total size of Home: " + home.getSize() + " KB");
        System.out.println("\nTree structure:");
        home.display("");

        printSize(home);
        printSize(report);
    }
}
