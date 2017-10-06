package com.example.chanakafernando.messagepassing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    static final String SAVE_INSTANCE_STATUS = "textStatus";
    static final String SAVE_INSTANCE_INT_VALUE = "textIntValue";
    static final String SAVE_INSTANCE_STRING_VALUE = "textStrValue";

    static TextView mTextStatus, mTextIntValue, mTextStrValue;
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyService.MSG_SET_INT_VALUE:
                    mTextIntValue.setText("Int Message: " + msg.arg1);
                    break;
                case MyService.MSG_SET_STRING_VALUE:
                    String str1 = msg.getData().getString("str1");
                    mTextStrValue.setText("Str Message: " + str1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mTextStatus.setText("Attached.");
            try {
                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            mTextStatus.setText("Disconnected.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextStatus = (TextView) findViewById(R.id.activity_main_status_text);
        mTextIntValue = (TextView) findViewById(R.id.activity_main_int_value_text);
        mTextStrValue = (TextView) findViewById(R.id.activity_main_value_string_text);

        restoreMe(savedInstanceState);

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString( SAVE_INSTANCE_STATUS, mTextStatus.getText().toString());
        outState.putString( SAVE_INSTANCE_INT_VALUE, mTextIntValue.getText().toString());
        outState.putString( SAVE_INSTANCE_STRING_VALUE, mTextStrValue.getText().toString());
    }

    private void restoreMe(Bundle state) {
        if (state != null) {
            mTextStatus.setText(state.getString(SAVE_INSTANCE_STATUS));
            mTextIntValue.setText(state.getString(SAVE_INSTANCE_INT_VALUE));
            mTextStrValue.setText(state.getString(SAVE_INSTANCE_STRING_VALUE));
        }
    }

    public void onClickBtnStart(View view) {
        startService(new Intent(MainActivity.this, MyService.class));
    }

    public void onClickBtnStop(View view) {
        doUnbindService();
        stopService(new Intent(MainActivity.this, MyService.class));
    }

    public void onClickBtnBind(View view) {
        doBindService();
    }

    public void onClickBtnUnbind(View view) {
        doUnbindService();
    }

    public void onClickBtnUpBy1(View view) {
        sendMessageToService(1);
    }

    public void onClickBtnUpBy10(View view) {
        sendMessageToService(10);
    }

    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MyService.isRunning()) {
            doBindService();
        }
    }

    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_SET_INT_VALUE, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    void doBindService() {
        bindService(new Intent(this, MyService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mTextStatus.setText("Binding.");
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, MyService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mServiceConnection);
            mIsBound = false;
            mTextStatus.setText("Unbinding.");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
    }

}
