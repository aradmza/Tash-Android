package ir.mza.tash;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.github.cafebazaar.poolakey.Connection;
import com.github.cafebazaar.poolakey.Payment;
import com.github.cafebazaar.poolakey.PaymentConfiguration;
import com.github.cafebazaar.poolakey.SecurityCheck;
import com.github.cafebazaar.poolakey.model.PurchaseRequest;

import kotlin.Unit;

/**
 * Billing bridge for Cafe Bazaar (Poolakey).
 * Call from web: TashBilling.purchase("pup_plan")
 */
public class BillingBridge {

    private static final String RSA_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwChfUmHumFiUHlui1/ak2DaEtsamNMe+ySHUXBFMy16DCo0JoeABTVNjs67YFrYad6PLguSMZ+yteklzCD4wg1u/bApmaC6GJDsMyCm+pboVV1F8PEBwkoMNOcSi8AgOB3zJAltGUEqMed5XDo8rXWCl7ItquSoyNkYSAG+7DLmKCDl2M6AJMRbrd0Cq0J6TqrcEe1qym+W2isdrgGcMH7FkYzFUzB/4adALS7X3YkCAwEAAQ==";

    private final MainActivity activity;
    private Payment payment;
    private Connection paymentConnection;

    public BillingBridge(MainActivity activity) {
        this.activity = activity;
        initPayment();
    }

    private void initPayment() {
        if (!"bazaar".equals(BuildConfig.MARKET)) return;

        SecurityCheck securityCheck = new SecurityCheck.Enable(RSA_KEY);
        PaymentConfiguration config = new PaymentConfiguration(securityCheck);
        payment = new Payment(activity, config);

        paymentConnection = payment.connect(connectionCallback -> {
            connectionCallback.connectionSucceed(() -> {
                activity.runOnUiThread(() -> Toast.makeText(activity, "بازار وصل شد", Toast.LENGTH_SHORT).show());
                return Unit.INSTANCE;
            });
            connectionCallback.connectionFailed(throwable -> {
                activity.runOnUiThread(() -> Toast.makeText(activity, "خطا اتصال بازار", Toast.LENGTH_SHORT).show());
                return Unit.INSTANCE;
            });
            connectionCallback.disconnected(() -> Unit.INSTANCE);
            return Unit.INSTANCE;
        });
    }

    @JavascriptInterface
    public void purchase(String sku) {
        activity.runOnUiThread(() -> {
            if (!"bazaar".equals(BuildConfig.MARKET) || payment == null) {
                Toast.makeText(activity, "درگاه فقط برای بازار", Toast.LENGTH_SHORT).show();
                return;
            }
            PurchaseRequest request = new PurchaseRequest(sku, "tash_" + sku, null);
            payment.purchaseProduct(activity.getActivityResultRegistry(), request, purchaseCallback -> {
                purchaseCallback.purchaseFlowBegan(() -> Unit.INSTANCE);
                purchaseCallback.failedToBeginFlow(t -> {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "شروع خرید ناموفق", Toast.LENGTH_SHORT).show());
                    return Unit.INSTANCE;
                });
                purchaseCallback.purchaseSucceed(entity -> {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "خرید موفق: " + sku, Toast.LENGTH_LONG).show();
                        // TODO: notify web or backend to activate subscription
                        activity.webView.evaluateJavascript(
                            "window.onTashPurchaseSuccess && window.onTashPurchaseSuccess('" + sku + "','" + entity.getPurchaseToken() + "')",
                            null);
                    });
                    return Unit.INSTANCE;
                });
                purchaseCallback.purchaseCanceled(() -> {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "خرید لغو شد", Toast.LENGTH_SHORT).show());
                    return Unit.INSTANCE;
                });
                purchaseCallback.purchaseFailed(t -> {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "خرید ناموفق", Toast.LENGTH_SHORT).show());
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
        });
    }

    @JavascriptInterface
    public String getMarket() {
        return BuildConfig.MARKET;
    }

    public void disconnect() {
        if (paymentConnection != null) paymentConnection.disconnect();
    }
}
