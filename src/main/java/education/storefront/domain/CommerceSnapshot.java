package education.storefront.domain;

import java.time.LocalDate;

public final class CommerceSnapshot {
    private final LocalDate businessDate;
    private final Checkout checkout;
    private final Fulfillment fulfillment;
    private final Receipt receipt;
    private final CustomerOrderUpdate orderUpdate;

    public CommerceSnapshot(LocalDate businessDate, Checkout checkout, Fulfillment fulfillment,
                            Receipt receipt, CustomerOrderUpdate orderUpdate) {
        this.businessDate = businessDate;
        this.checkout = checkout;
        this.fulfillment = fulfillment;
        this.receipt = receipt;
        this.orderUpdate = orderUpdate;
    }

    public static CommerceSnapshot lessonOrder(LocalDate date) {
        return new CommerceSnapshot(date,
                new Checkout("checkout-1042", "course-java-21", 7900),
                new Fulfillment("order-1042", "DIGITAL_ACCESS_GRANTED"),
                new Receipt("receipt-1042", "CNY", 7900),
                new CustomerOrderUpdate("learner-28", "order-1042", "COMPLETED"));
    }

    public LocalDate businessDate() { return businessDate; }
    public Checkout checkout() { return checkout; }
    public Fulfillment fulfillment() { return fulfillment; }
    public Receipt receipt() { return receipt; }
    public CustomerOrderUpdate orderUpdate() { return orderUpdate; }

    public static final class Checkout {
        public final String checkoutId;
        public final String sku;
        public final int amountMinor;
        public Checkout(String checkoutId, String sku, int amountMinor) {
            this.checkoutId = checkoutId; this.sku = sku; this.amountMinor = amountMinor;
        }
    }

    public static final class Fulfillment {
        public final String orderId;
        public final String state;
        public Fulfillment(String orderId, String state) { this.orderId = orderId; this.state = state; }
    }

    public static final class Receipt {
        public final String receiptId;
        public final String currency;
        public final int amountMinor;
        public Receipt(String receiptId, String currency, int amountMinor) {
            this.receiptId = receiptId; this.currency = currency; this.amountMinor = amountMinor;
        }
    }

    public static final class CustomerOrderUpdate {
        public final String customerId;
        public final String orderId;
        public final String state;
        public CustomerOrderUpdate(String customerId, String orderId, String state) {
            this.customerId = customerId; this.orderId = orderId; this.state = state;
        }
    }
}
