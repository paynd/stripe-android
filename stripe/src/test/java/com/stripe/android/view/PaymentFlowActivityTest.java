package com.stripe.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.stripe.android.BuildConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.R;
import com.stripe.android.exception.APIException;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_CONFIG;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_SHIPPING_INFO_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentFlowActivityTest {

    private ActivityController<PaymentFlowActivity> mActivityController;
    private ShadowActivity mShadowActivity;
    private ShippingInfoWidget mShippingInfoWidget;
    @Mock
    EphemeralKeyProvider mEphemeralKeyProvider;

    @Mock BroadcastReceiver mBroadcastReceiver;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(RuntimeEnvironment.application);
        localBroadcastManager.registerReceiver(mBroadcastReceiver, new IntentFilter(PaymentFlowActivity.EVENT_SHIPPING_INFO_SUBMITTED));
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider);
    }

    @After
    public void tearDown() {
        LocalBroadcastManager.getInstance(RuntimeEnvironment.application).unregisterReceiver(mBroadcastReceiver);
    }

    @Test
    public void launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        PaymentConfiguration.init("FAKE PUBLISHABLE KEY");
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        assertNull(mActivityController.get().findViewById(R.id.shipping_info_widget));
        assertNotNull(mActivityController.get().findViewById(R.id.select_shipping_method_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
    }

    @Test
    public void onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        mShadowActivity = shadowOf(mActivityController.get());
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        assertFalse(mShadowActivity.isFinishing());
        assertNotNull(mActivityController.get().findViewById(R.id.shipping_info_widget));
    }

    @Test
    public void onShippingInfoSave_whenShippingPopulated_sendsCorrectIntent() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        mShippingInfoWidget = mActivityController.get().findViewById(R.id.shipping_info_widget);
        assertNotNull(mShippingInfoWidget);
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        ArgumentCaptor<Intent> intentArgumentCaptor =
                ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(any(Context.class), intentArgumentCaptor.capture());
        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertEquals(captured.getParcelableExtra(EXTRA_SHIPPING_INFO_DATA), getExampleShippingInfo());
    }

    @Test
    public void onErrorBroadcast_displaysAlertDialog() {
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();

        StripeActivity.AlertMessageListener mockListener =
                mock(StripeActivity.AlertMessageListener.class);
        mActivityController.get().setAlertMessageListener(mockListener);

        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, new APIException("Something's wrong", "ID123", 400, null));
        Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(mActivityController.get())
                .sendBroadcast(errorIntent);

        verify(mockListener).onAlertMessageDisplayed("Something's wrong");
    }

    @Test
    public void onShippingInfoProcessed_whenShippingInfoSubmitted_rendersCorrectly() {
        Intent intent = new Intent(RuntimeEnvironment.application, PaymentFlowActivity.class);
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        PaymentSessionData paymentSessionData = Mockito.mock(PaymentSessionData.class);
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, paymentSessionData);
        mActivityController = Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        PaymentFlowActivity paymentFlowActivity = mActivityController.get();
        paymentFlowActivity.onActionSave();
        assertEquals(paymentFlowActivity.mProgressBar.getVisibility(), View.VISIBLE);
        Intent onShippingInfoProcessedInvalid = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
        onShippingInfoProcessedInvalid.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
        LocalBroadcastManager.getInstance(RuntimeEnvironment.application).sendBroadcast(onShippingInfoProcessedInvalid);
        assertEquals(paymentFlowActivity.mProgressBar.getVisibility(), View.GONE);
    }

    private ShippingInformation getExampleShippingInfo() {
        Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "6504604645");
    }
}
