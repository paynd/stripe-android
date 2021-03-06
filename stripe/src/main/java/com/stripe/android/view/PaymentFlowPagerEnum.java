package com.stripe.android.view;

import com.stripe.android.R;

enum PaymentFlowPagerEnum {

    ADDRESS(R.string.title_add_an_address, R.layout.activity_add_address),
    SHIPPING_METHOD(R.string.title_select_shipping_method, R.layout.activity_select_shipping_method);

    private int mTitleResId;
    private int mLayoutResId;

    PaymentFlowPagerEnum(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    int getTitleResId() {
        return mTitleResId;
    }

    int getLayoutResId() {
        return mLayoutResId;
    }

}
