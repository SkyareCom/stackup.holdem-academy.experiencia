package com.skyare.stackupacademy;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

final class BillingManager implements PurchasesUpdatedListener {
    interface JavascriptEmitter {
        void emit(String javascript);
    }

    private static final String TAG = "StackUpAcademy";
    static final String PRODUCT_ID = "academy_access";
    static final String BASE_MONTHLY = "monthly";
    static final String BASE_SIX_MONTH = "six-month";
    static final String BASE_ANNUAL = "annual";
    private static final String PREFS = "stackup_android_shell";
    private static final String PLAN_PREF = "billing_plan";

    private final FragmentActivity activity;
    private final JavascriptEmitter emitter;
    private BillingClient client;
    private boolean connecting;
    private String queuedPlan;

    BillingManager(FragmentActivity activity, JavascriptEmitter emitter) {
        this.activity = activity;
        this.emitter = emitter;
        init();
    }

    private void init() {
        try {
            client = BillingClient.newBuilder(activity)
                    .setListener(this)
                    .enablePendingPurchases(
                            PendingPurchasesParams.newBuilder()
                                    .enableOneTimeProducts()
                                    .build())
                    .enableAutoServiceReconnection()
                    .build();
            connect();
        } catch (Throwable error) {
            Log.e(TAG, "BILLING_INIT_FAILED", error);
        }
    }

    void onResume() {
        if (client != null && !client.isReady()) connect();
    }

    void destroy() {
        if (client != null) {
            try {
                client.endConnection();
            } catch (Throwable ignored) {
            }
            client = null;
        }
    }

    void requestSubscription(String requestedPlan) {
        String plan = normalizePlan(requestedPlan);
        if (plan == null) {
            message("Plano de assinatura inválido.");
            return;
        }
        prefs().edit().putString(PLAN_PREF, plan).apply();
        queuedPlan = plan;
        activity.runOnUiThread(() -> {
            if (client == null || !client.isReady()) {
                connect();
            } else {
                queuedPlan = null;
                launch(plan);
            }
        });
    }

    void restoreSubscriptions() {
        if (client == null || !client.isReady()) {
            connect();
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        client.queryPurchasesAsync(params, (result, purchases) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "BILLING_RESTORE_FAILED code=" + result.getResponseCode());
                return;
            }
            for (Purchase purchase : purchases) {
                if (isAcademySubscription(purchase)
                        && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    handlePurchase(purchase);
                    return;
                }
            }
            entitlement("free", false, "inactive");
        });
    }

    private void connect() {
        if (client == null || client.isReady() || connecting) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                connecting = false;
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "BILLING_READY");
                    String plan = queuedPlan;
                    if (plan != null) {
                        queuedPlan = null;
                        launch(plan);
                    } else {
                        restoreSubscriptions();
                    }
                } else {
                    Log.w(TAG, "BILLING_SETUP_FAILED code=" + result.getResponseCode());
                    message("Não foi possível conectar à Google Play. Tente novamente.");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting = false;
                Log.w(TAG, "BILLING_DISCONNECTED");
            }
        });
    }

    private void launch(String plan) {
        if (client == null || !client.isReady()) {
            queuedPlan = plan;
            connect();
            return;
        }
        String basePlanId = basePlanId(plan);
        if (basePlanId == null) {
            message("Plano de assinatura inválido.");
            return;
        }

        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build();
        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build();

        client.queryProductDetailsAsync(params, (result, queryResult) -> {
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || queryResult.getProductDetailsList().isEmpty()) {
                Log.w(TAG, "BILLING_PRODUCT_UNAVAILABLE code=" + result.getResponseCode());
                message("Assinatura ainda não disponível na Google Play.");
                return;
            }

            ProductDetails details = queryResult.getProductDetailsList().get(0);
            ProductDetails.SubscriptionOfferDetails offer =
                    findOffer(details.getSubscriptionOfferDetails(), basePlanId);
            if (offer == null) {
                Log.w(TAG, "BILLING_BASE_PLAN_NOT_FOUND plan=" + basePlanId);
                message("Este plano ainda não foi ativado no Google Play.");
                return;
            }

            BillingFlowParams.ProductDetailsParams detailsParams =
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offer.getOfferToken())
                            .build();

            QueryPurchasesParams ownedParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();
            client.queryPurchasesAsync(ownedParams, (ownedResult, purchases) -> {
                BillingFlowParams.Builder flow = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Collections.singletonList(detailsParams));

                if (ownedResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {
                        if (isAcademySubscription(purchase)
                                && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            flow.setSubscriptionUpdateParams(
                                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                            .setOldPurchaseToken(purchase.getPurchaseToken())
                                            .setSubscriptionReplacementMode(
                                                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
                                            .build());
                            break;
                        }
                    }
                }

                activity.runOnUiThread(() -> {
                    BillingResult launchResult = client.launchBillingFlow(activity, flow.build());
                    if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "BILLING_FLOW_NOT_STARTED code="
                                + launchResult.getResponseCode());
                        message("Não foi possível abrir a compra na Google Play.");
                    }
                });
            });
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) handlePurchase(purchase);
            return;
        }
        if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            message("Assinatura cancelada.");
            return;
        }
        Log.w(TAG, "BILLING_PURCHASE_UPDATE code=" + result.getResponseCode());
        message("A Google Play não concluiu a assinatura.");
    }

    private void handlePurchase(Purchase purchase) {
        if (!isAcademySubscription(purchase)) return;
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            message("Pagamento pendente na Google Play.");
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

        String plan = normalizePlan(prefs().getString(PLAN_PREF, "mensal"));
        if (plan == null) plan = "mensal";
        final String grantedPlan = plan;

        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledge = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            client.acknowledgePurchase(acknowledge, result -> {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    entitlement(grantedPlan, true, "active");
                } else {
                    Log.w(TAG, "BILLING_ACK_FAILED code=" + result.getResponseCode());
                    message("Pagamento recebido; aguardando confirmação da Google Play.");
                }
            });
        } else {
            entitlement(grantedPlan, true, "active");
        }
    }

    private boolean isAcademySubscription(Purchase purchase) {
        return purchase != null && purchase.getProducts().contains(PRODUCT_ID);
    }

    private ProductDetails.SubscriptionOfferDetails findOffer(
            List<ProductDetails.SubscriptionOfferDetails> offers, String basePlanId) {
        if (offers == null) return null;
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (basePlanId.equals(offer.getBasePlanId()) && offer.getOfferId() == null) {
                return offer;
            }
        }
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (basePlanId.equals(offer.getBasePlanId())) return offer;
        }
        return null;
    }

    private String normalizePlan(String plan) {
        if ("mensal".equals(plan) || "semestral".equals(plan) || "anual".equals(plan)) {
            return plan;
        }
        return null;
    }

    private String basePlanId(String plan) {
        if ("mensal".equals(plan)) return BASE_MONTHLY;
        if ("semestral".equals(plan)) return BASE_SIX_MONTH;
        if ("anual".equals(plan)) return BASE_ANNUAL;
        return null;
    }

    private SharedPreferences prefs() {
        return activity.getSharedPreferences(PREFS, FragmentActivity.MODE_PRIVATE);
    }

    private void entitlement(String plan, boolean active, String status) {
        emitter.emit("window.StackUpBilling&&window.StackUpBilling.onEntitlement("
                + JSONObject.quote(plan) + ","
                + (active ? "true" : "false") + ","
                + JSONObject.quote(status) + ");");
    }

    private void message(String value) {
        emitter.emit("window.StackUpBilling&&window.StackUpBilling.onMessage("
                + JSONObject.quote(value == null ? "" : value) + ");");
    }
}
