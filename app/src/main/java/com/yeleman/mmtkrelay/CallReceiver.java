package com.yeleman.mmtkrelay;

import android.content.Context;

import java.util.Date;

public class CallReceiver extends PhoneCallReceiver {

    @Override
    protected void onIncomingCallReceived(Context context, String number, Date start)
    {
        IncomingTextProcessor.startProcessingCall(context, number, start.getTime());
    }

    @Override
    protected void onIncomingCallAnswered(Context context, String number, Date start){}

    @Override
    protected void onIncomingCallEnded(Context context, String number, Date start, Date end) {}

    @Override
    protected void onOutgoingCallStarted(Context context, String number, Date start) {}

    @Override
    protected void onOutgoingCallEnded(Context context, String number, Date start, Date end) {}

    @Override
    protected void onMissedCall(Context context, String number, Date start) {}
}