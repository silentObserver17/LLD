package DesignPatterns.Structural.Flyweight;
/*
* Flyweight is the structural design pattern that lets us support large number of objects by sharing common(intrinsic) state while keeping the unique(extrinsic) state separate.
* It minimizes the memory space when we have thousands or millions of similar objects.
* In a word processor, every character on screen is an object.
If we have a 100-page document → millions of characters.
We don’t create a new full object for every “a” with font, size, color, etc.
Instead:
Shared (intrinsic): the glyph “a” in Arial font
Unique (extrinsic): position x=120, y=450, color=red (passed at runtime)
*
* Goal: Minimize memory by sharing as much data as possible.
* */

/*
Key Concepts:

- Intrinsic state: shared, immutable, stored in the flyweight (e.g., product name, image, base price)
- Extrinsic state: unique, passed by client at runtime (e.g., quantity in cart, current discount, user-specific price)
- Flyweight Factory: ensures sharing (returns existing instance if possible)
*/

import java.util.HashMap;
import java.util.Map;

interface Product{
    void display(String userType, double userDiscountRate, int stockLevel);
}

// 2. Concrete Flyweight – holds only intrinsic (shared) state
class CatalogProduct implements Product{
    private final String sku;
    private final String name;
    private final String imageUrl;
    private final double basePrice;
    private final String description;

    public CatalogProduct(String sku, String name, String imageUrl, double basePrice, String description) {
        this.sku = sku;
        this.name = name;
        this.imageUrl = imageUrl;
        this.basePrice = basePrice;
        this.description = description;
        System.out.println("   [Flyweight] Created shared product: " + sku); // happens only once per SKU
    }

    @Override
    public void display(String userType, double userDiscountRate, int stockLevel) {
        double finalPrice = basePrice * (1 - userDiscountRate);
        String availability = stockLevel > 0 ? "In Stock" : "Out of Stock";

        System.out.printf("Product: %s | Price: ₹%.2f (for %s user) | %s | %s%n",
                name, finalPrice, userType, availability, imageUrl);
    }

    public String getSku() {
        return sku;
    }
}

class ProductFactory{
    private static final Map<String, Product> products = new HashMap<>();

    public static Product getProduct(String sku){
        Product product = products.get(sku);
        if(product == null){
            product = createProduct(sku);
            products.put(sku, product);
        }
        return product;
    }

    private static Product createProduct(String sku) {
        return switch (sku) {
            case "TSHIRT001" -> new CatalogProduct("TSHIRT001", "Cool T-Shirt", "img/tshirt.jpg", 999.00, "Cotton tee");
            case "JEANS001"  -> new CatalogProduct("JEANS001", "Denim Jeans", "img/jeans.jpg", 2999.00, "Slim fit");
            case "SHOES001"  -> new CatalogProduct("SHOES001", "Running Shoes", "img/shoes.jpg", 4999.00, "Lightweight");
            default -> throw new IllegalArgumentException("Unknown product: " + sku);
        };
    }

    public static int getTotalProductsCreated(){
        return products.size();
    }
}

public class Flyweight{
    public static void main(String[] args) {
// Simulate rendering 1 million product listings for different users
        String[] skus = {"TSHIRT001", "JEANS001", "SHOES001"};
        String[] userTypes = {"REGULAR", "PREMIUM", "VIP"};
        double[] discounts = {0.0, 0.10, 0.20}; // 0%, 10%, 20%

        for (int i = 0; i < 20; i++) {  // In real app: millions
            String sku = skus[i % skus.length];
            String userType = userTypes[i % userTypes.length];
            double discount = discounts[i % userTypes.length];
            int stock = (i % 5 == 0) ? 0 : 50 + i;

            Product product = ProductFactory.getProduct(sku);  // ← shared!
            product.display(userType, discount, stock);
        }

        System.out.println("\nTotal unique Product objects created: " + ProductFactory.getTotalProductsCreated());
        System.out.println("(Even if we displayed millions of listings!)");
    }
}
