package ir.mza.tash;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Bridge for in-app billing.
 * Bazaar uses Poolakey. Myket will use its own client later.
 * Call from web: TashBilling.purchase(sku)
 */
public class BillingBridge {

    private final MainActivity activity;

    public BillingBridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void purchase(String sku) {
        activity.runOnUiThread(() -> {
            String market = BuildConfig.MARKET;
            if ("bazaar".equals(market)) {
                // TODO: implement Poolakey purchase here
                // Example: payment.purchaseProduct(...)
                Toast.makeText(activity, "Bazaar purchase: " + sku, Toast.LENGTH_SHORT).show();
            } else if ("myket".equals(market)) {
                // TODO: implement Myket billing
                Toast.makeText(activity, "Myket purchase: " + sku, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Unknown market", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public String getMarket() {
        return BuildConfig.MARKET;
    }
}
