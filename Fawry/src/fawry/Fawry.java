package fawry;
import java.util.*;


class ExpirableProduct extends Product {
    private Date expiryDate;

    public ExpirableProduct(String name, double price, int quantity, Date expiryDate) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean isExpired() {
        return new Date().after(expiryDate);
    }
}

class ShippableProduct extends Product implements Shippable {
    private double weight;

    public ShippableProduct(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

   
    public double getWeight() { return weight; }
}

class ExpirableShippableProduct extends ExpirableProduct implements Shippable {
    private double weight;

    public ExpirableShippableProduct(String name, double price, int quantity, Date expiryDate, double weight) {
        super(name, price, quantity, expiryDate);
        this.weight = weight;
    }

    @Override
    public double getWeight() { return weight; }
}


class CartItem {
    Product product;
    int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

class Cart {
    List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (!product.isAvailable(quantity)) {
            throw new IllegalArgumentException("Quantity not available for: " + product.getName());
        }
        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() { return items; }
    public boolean isEmpty() { return items.isEmpty(); }
    public double getSubtotal() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public List<Shippable> getShippableItems() {
        List<Shippable> result = new ArrayList<>();
        for (CartItem item : items) {
            if (item.product instanceof Shippable) {
                for (int i = 0; i < item.quantity; i++) {
                    result.add((Shippable) item.product);
                }
            }
        }
        return result;
    }
}


class Customer {
    String name;
    double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public boolean canAfford(double amount) {
        return balance >= amount;
    }

    public void pay(double amount) {
        this.balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}


class ShippingService {
    public static void ship(List<Shippable> items) {
        System.out.println("** Shipment notice **");
        Map<String, Double> shippedItems = new HashMap<>();
        for (Shippable item : items) {
            shippedItems.put(item.getName(), shippedItems.getOrDefault(item.getName(), 0.0) + item.getWeight());
        }
        double totalWeight = 0;
        for (Map.Entry<String, Double> entry : shippedItems.entrySet()) {
            System.out.println(entry.getKey() + "\t" + (int)(entry.getValue() * 1000) + "g");
            totalWeight += entry.getValue();
        }
        System.out.println("Total package weight " + totalWeight + "kg\n");
    }
}


public class Fawry {
    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) throw new RuntimeException("Cart is empty");

        for (CartItem item : cart.getItems()) {
            if (item.product.isExpired()) {
                throw new RuntimeException("Product expired: " + item.product.getName());
            }
        }

        double subtotal = cart.getSubtotal();
        double shipping = cart.getShippableItems().isEmpty() ? 0 : 30;
        double total = subtotal + shipping;

        if (!customer.canAfford(total)) {
            throw new RuntimeException("Customer balance insufficient");
        }

        for (CartItem item : cart.getItems()) {
            item.product.reduceQuantity(item.quantity);
        }

        customer.pay(total);

        if (!cart.getShippableItems().isEmpty()) {
            ShippingService.ship(cart.getShippableItems());
        }

        System.out.println("** Checkout receipt **");
        for (CartItem item : cart.getItems()) {
            System.out.println(item.quantity + "x " + item.product.getName() + "\t" + item.getTotalPrice());
        }
        System.out.println("----------------------");
        System.out.println("Subtotal\t\t" + subtotal);
        System.out.println("Shipping\t\t" + shipping);
        System.out.println("Amount\t\t" + total);
        System.out.println("Remaining balance\t" + customer.getBalance());
    }

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);

        Product cheese = new ExpirableShippableProduct("Cheese", 100, 5, cal.getTime(), 0.2);
        Product biscuits = new ExpirableShippableProduct("Biscuits", 150, 2, cal.getTime(), 0.7);
        Product tv = new ShippableProduct("TV", 300, 3, 5);
        Product scratchCard = new Product("Scratch Card", 50, 10) {};

        Customer customer = new Customer("Ahmed", 1000);
        Cart cart = new Cart();

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        checkout(customer, cart);
    }
}
