package education.storefront.snapshot;

import education.storefront.domain.CommerceSnapshot;
import java.nio.charset.StandardCharsets;

public final class SnapshotDocument {
    public String objectKey(CommerceSnapshot snapshot) {
        return "nightly/" + snapshot.businessDate() + "/commerce.json";
    }

    public byte[] encode(CommerceSnapshot snapshot) {
        CommerceSnapshot.Checkout c = snapshot.checkout();
        CommerceSnapshot.Fulfillment f = snapshot.fulfillment();
        CommerceSnapshot.Receipt r = snapshot.receipt();
        CommerceSnapshot.CustomerOrderUpdate u = snapshot.orderUpdate();
        String json = "{\"business_date\":\"" + snapshot.businessDate() + "\"," +
                "\"checkout\":{\"checkout_id\":\"" + escape(c.checkoutId) + "\",\"sku\":\"" + escape(c.sku) +
                "\",\"amount_minor\":" + c.amountMinor + "}," +
                "\"fulfillment\":{\"order_id\":\"" + escape(f.orderId) + "\",\"state\":\"" + escape(f.state) + "\"}," +
                "\"receipt\":{\"receipt_id\":\"" + escape(r.receiptId) + "\",\"currency\":\"" + escape(r.currency) +
                "\",\"amount_minor\":" + r.amountMinor + "}," +
                "\"customer_order_update\":{\"customer_id\":\"" + escape(u.customerId) + "\",\"order_id\":\"" +
                escape(u.orderId) + "\",\"state\":\"" + escape(u.state) + "\"}}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
