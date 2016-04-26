package br.ufmg.dcc.verlab.maglogger;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScrollingActivity extends AppCompatActivity {

    private final String TAG = ScrollingActivity.class.getSimpleName();
    private static UsbSerialPort sPort = null;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;
    private TextView scrollTextView;
    private NestedScrollView scrollView;
    private ReadingParser rp = new ReadingParser();
    private List<String> dataBuffer = new LinkedList<String>();
    private String logFileName = "";

    void writeToTextView(TextView tView, NestedScrollView sView, String msg){
        if(tView == null) return;

        tView.append(msg);

        int max_line = 40;

        int excess = tView.getLineCount() - max_line;
        if(excess > 0){
            Log.i(TAG, "Cleaning view... ex:" + excess + " lineC:" + tView.getLineCount());
            int eolIndex = 0;
            CharSequence charSequence = tView.getText();
            for(int i=0; i < excess; i++) {
                while(eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n'){
                    eolIndex++;
                }
            }
            Log.i(TAG, "EolIndex:" + eolIndex + "charseq:" + charSequence.length());
            if (eolIndex < charSequence.length()) {
                tView.getEditableText().delete(0, eolIndex+1);
            }
            else {
                Log.i(TAG, "Set text empty");
                tView.setText("");
            }
        }

        sView.fullScroll(View.FOCUS_DOWN);
    }

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    ScrollingActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScrollingActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scrollTextView = (TextView) findViewById(R.id.scrollTextView);
        scrollView = (NestedScrollView) findViewById(R.id.scrollView);

        writeToTextView(scrollTextView, scrollView, "Starting...\n");

        java.util.Date date= new java.util.Date();
        this.logFileName = "magnetic_log_" + (new Timestamp(date.getTime())).toString() + ".txt";

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        try {
            String dataStr = new String(data, "UTF-8");
            rp.updateData(dataStr);
        } catch(Exception e){
            writeToTextView(scrollTextView, scrollView, e.getMessage());
        }

        if(rp.isDataReady()){
            float[] fdata = rp.getData();
            String msg = "";
            msg += "X:";
            msg += fdata[0];
            msg += " Y:";
            msg += fdata[1];
            msg += " Z:";
            msg += fdata[2];
            msg += " T:";
            msg += fdata[3];
            msg += " t:";
            msg += fdata[4];
            msg += "\n";

            dataBuffer.add(msg);
            if(dataBuffer.size() > 10){
                writeBufferToFile();
            }

            writeToTextView(scrollTextView, scrollView, msg);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        connect();
        onDeviceStateChange();
    }

    void connect(){
        UsbManager manager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            writeToTextView(scrollTextView, scrollView, "Error opening the driver, no available drivers.");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            writeToTextView(scrollTextView, scrollView, "Error opening the driver, connection is null.");
            return;
        }

        sPort = driver.getPorts().get(0);
        try {
            sPort.open(connection);
            sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            writeToTextView(scrollTextView, scrollView, "CD  - Carrier Detect" + sPort.getCD() + "\n");
            writeToTextView(scrollTextView, scrollView, "CTS - Clear To Send" + sPort.getCTS() + "\n");
            writeToTextView(scrollTextView, scrollView, "DSR - Data Set Ready" + sPort.getDSR() + "\n");
            writeToTextView(scrollTextView, scrollView, "DTR - Data Terminal Ready" + sPort.getDTR() + "\n");
            writeToTextView(scrollTextView, scrollView, "DSR - Data Set Ready" + sPort.getDSR() + "\n");
            writeToTextView(scrollTextView, scrollView, "RI  - Ring Indicator" + sPort.getRI() + "\n");
            writeToTextView(scrollTextView, scrollView, "RTS - Request To Send" + sPort.getRTS() + "\n");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            writeToTextView(scrollTextView, scrollView, "Error opening device: " + e.getMessage() + "\n");

            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            sPort = null;
        }
    }

    private void writeBufferToFile() {
        try {
//            File path = this.getApplicationContext().getFilesDir();
//            .getAbsolutePath()
            File path = Environment.getExternalStorageDirectory();
            File file = new File(path, this.logFileName);

            Log.e("Exception", "Writting to file...." + path + "/" +  this.logFileName);

            FileOutputStream stream = new FileOutputStream(file, true);
            try {
                for(String msg: this.dataBuffer){
                    stream.write((msg).getBytes());
                }
                this.dataBuffer.clear();
                writeToTextView(scrollTextView, scrollView, "\n\nWrote to file!\n\n");
            } finally {
                stream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

}
