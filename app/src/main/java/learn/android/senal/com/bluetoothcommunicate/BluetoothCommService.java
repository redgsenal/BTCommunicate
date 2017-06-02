package learn.android.senal.com.bluetoothcommunicate;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by reggie.senal on 2/6/2017.
 */

public class BluetoothCommService {
    private static final String TAG = "BluetoothCommService";
    private static final String appName = "MyBTApp";

    private static final UUID MY_UUID_INSECURE = UUID.fromString("71c68c96-3232-4eb5-8247-4b148fa3f07e");
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    private ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    private EditText etMsgs;

    public BluetoothCommService(Context mContext, EditText msg) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = mContext;
        etMsgs = msg;
        start();
    }

    private class AcceptThread extends Thread {
        BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "Accept Thread: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, " AcceptThread IOException " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(TAG, "run Accept Thread: ");
            BluetoothSocket socket = null;
            try {
                Log.d(TAG, "run RFCOM server socket start....");
                socket = mmServerSocket.accept();
            }catch (IOException e){
                Log.e(TAG, " AcceptThread run IOException " + e.getMessage());
            }

            if(socket != null){
                connected(socket, mmDevice);
            }

            Log.i(TAG, "END Accet Thread");
        }

        public void cancel(){
            Log.d(TAG, "cancel: Cancel AcceptThread");
            try{
                mmServerSocket.close();
            }catch (IOException e){
                Log.e(TAG, "cancel: Close of AcceptThread fail " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectedThread started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        @Override
        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN connect thread ");
            try {
                Log.d(TAG, "Connect thread using UUID " + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "Unable to create InsecureRFCommSocket " + e.getMessage());
            }
            mmSocket = tmp;

            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d(TAG, "Connect success ");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "Connect closed");
                } catch (IOException e1) {
                    Log.e(TAG, "Connect unable to close properly " + e1.getMessage());
                }
                Log.d(TAG, "Unable to connect to UUID " + MY_UUID_INSECURE);

            }
            connected(mmSocket, mmDevice);
        }

        public void cancel(){
            Log.d(TAG, "cancel: ConnectThread ");
            try{
                mmSocket.close();
            }catch (IOException e){
                Log.e(TAG, "cancel: close fail " + e.getMessage());
            }
        }
    }

    public synchronized void start(){
        Log.d(TAG, "start");

        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "start client : started ");

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: starting ");

            mmSocket = socket;
            InputStream tmpin = null;
            OutputStream tmpout = null;

            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException ne){
            }

            try {
                tmpin = mmSocket.getInputStream();
                tmpout = mmSocket.getOutputStream();
            }catch(IOException ioe){
                Log.e(TAG, "io stream exception " + ioe.getMessage());
            }

            mmInStream = tmpin;
            mmOutStream = tmpout;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    etMsgs.setText(incomingMessage);
                    Log.e(TAG, "InputStream: " + incomingMessage);
                }catch (IOException ioe){
                    ioe.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write output stream " + e.getMessage());
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException ioe){
            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        Log.d(TAG, "connected: starting ");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out){
        ConnectedThread r;
        Log.d(TAG, "write : write called");
        mConnectedThread.write(out);;
    }
}
