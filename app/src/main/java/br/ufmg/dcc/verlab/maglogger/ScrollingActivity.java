package br.ufmg.dcc.verlab.maglogger;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    //private List<String> dataBuffer = new LinkedList<String>();
    //private List<String> dataBuffer = Collections.synchronizedList(new ArrayList<String>());
    private Queue<String> dataBuffer = new ConcurrentLinkedQueue<String>();


    private TextView mx, my, mz;
    private TextView mxc, myc, mzc;
    private TextView mT, mt;
    private TextView txtlon, txtlat;
    private TextView gx, gy, gz;
    private TextView ax, ay, az;
    private TextView max, may, maz;

    private TextView txtfile;

    private String logFileName = "";
    private GpsHelper gpsHelper = null;
    private SensorHelper sensorHelper = null;
    private Button con;

    private ProgressBar pb;

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
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        scrollTextView = (TextView) findViewById(R.id.scrollTextView);
        scrollView = (NestedScrollView) findViewById(R.id.scrollView);

        //Initialize text view datas from Magnetometer, IMU and GPS
        mx = (TextView) findViewById(R.id.txt_mx);
        mx.setTextSize(8);
        my = (TextView) findViewById(R.id.txt_my);
        my.setTextSize(8);
        mz = (TextView) findViewById(R.id.txt_mz);
        mz.setTextSize(8);
        mxc = (TextView) findViewById(R.id.txt_mxc);
        mxc.setTextSize(8);
        myc = (TextView) findViewById(R.id.txt_myc);
        myc.setTextSize(8);
        mzc = (TextView) findViewById(R.id.txt_mzc);
        mzc.setTextSize(8);
        mT = (TextView) findViewById(R.id.txt_mT);
        mT.setTextSize(8);
        mt = (TextView) findViewById(R.id.txt_mt);
        mt.setTextSize(8);

        txtlat = (TextView) findViewById(R.id.txt_lat);
        txtlat.setTextSize(8);
        txtlon = (TextView) findViewById(R.id.txt_lon);
        txtlon.setTextSize(8);

        gx = (TextView) findViewById(R.id.txt_gx);
        gx.setTextSize(8);
        gy = (TextView) findViewById(R.id.txt_gy);
        gy.setTextSize(8);
        gz = (TextView) findViewById(R.id.txt_gz);
        gz.setTextSize(8);

        ax = (TextView) findViewById(R.id.txt_ax);
        ax.setTextSize(8);
        ay = (TextView) findViewById(R.id.txt_ay);
        ay.setTextSize(8);
        az = (TextView) findViewById(R.id.txt_az);
        az.setTextSize(8);

        max = (TextView) findViewById(R.id.txt_imx);
        max.setTextSize(8);
        may = (TextView) findViewById(R.id.txt_imy);
        may.setTextSize(8);
        maz = (TextView) findViewById(R.id.txt_imz);
        maz.setTextSize(8);

        txtfile = (TextView) findViewById(R.id.txt_file);
        txtfile.setTextSize(8);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        // Initialize GPS
        gpsHelper = new GpsHelper(this);

        // Initialize IMU
        sensorHelper = new SensorHelper(this);

        writeToTextView(scrollTextView, scrollView, "Starting...\n");

        java.util.Date date = new java.util.Date();
        this.logFileName = "magnetic_log_" + (new Timestamp(date.getTime())).toString() + ".txt";
        txtfile.setText("magnetic_log_" + (new Timestamp(date.getTime())).toString() + ".txt");
        con = (Button) findViewById(R.id.btn_start);
        con.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
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
            StringBuilder sb = new StringBuilder();


            // Get magnetometer data

            mx.setText("x: "+fdata[0]);
            my.setText("y: "+fdata[1]);
            mz.setText("z: " + fdata[2]);
            // Calibrated
            mxc.setText("xc: " + fdata[0] * 1.247);
            myc.setText("yc: " + fdata[1] * 0.894);
            mzc.setText("zc: " + fdata[2] * 0.924);

            mT.setText("T: " + fdata[3]);
            mt.setText("t: " + fdata[4]);

            java.util.Date date = new java.util.Date();
            sb.append("Timestamp:" + (new Timestamp(date.getTime())).toString());

            sb.append(", XCal:");
            sb.append(fdata[0]*1.247);
            sb.append(", YCal:");
            sb.append(fdata[1]*0.894);
            sb.append(", ZCal:");
            sb.append(fdata[2]*0.924);

            sb.append(", X:");
            sb.append(fdata[0]);
            sb.append(", Y:");
            sb.append(fdata[1]);
            sb.append(", Z:");
            sb.append(fdata[2]);

            sb.append(", T:");
            sb.append(fdata[3]);
            sb.append(", t:");
            sb.append(fdata[4]);

            // Get GPS data
            gpsHelper.getMyLocation();
            double lat = 0;
            double lng = 0;
            if(gpsHelper.isGPSenabled()){
                lat = gpsHelper.getLatitude();
                lng = gpsHelper.getLongitude();
            }

            sb.append(", Lat:");
            sb.append(String.valueOf(lat));
            txtlat.setText("Lat: " + lat);
            sb.append(", Lng:");
            sb.append(String.valueOf(lng));
            txtlon.setText("Lon: " + lng);

            // Get IMU data
            // Accelerometer
            sb.append(", Accx:");
            sb.append(String.valueOf(sensorHelper.getAccx()));
            ax.setText("ax: " + sensorHelper.getAccx());
            sb.append(", Accy:");
            sb.append(String.valueOf(sensorHelper.getAccy()));
            ay.setText("ay: " + sensorHelper.getAccy());
            sb.append(", Accz:");
            sb.append(String.valueOf(sensorHelper.getAccz()));
            az.setText("az: " + sensorHelper.getAccz());
            // Gyroscopy
            sb.append(", Gyx:");
            sb.append(String.valueOf(sensorHelper.getGyx()));
            gx.setText("gx: " + sensorHelper.getGyx());
            sb.append(", Gyy:");
            sb.append(String.valueOf(sensorHelper.getGyy()));
            gy.setText("gy: " + sensorHelper.getGyy());
            sb.append(", Gyz:");
            sb.append(String.valueOf(sensorHelper.getGyz()));
            gz.setText("gz: " + sensorHelper.getGyz());
            // Magnetometer
            sb.append(" Magx:");
            sb.append(String.valueOf(sensorHelper.getMagx()));
            max.setText("mx: " + sensorHelper.getMagx());
            sb.append(" Magy:");
            sb.append(String.valueOf(sensorHelper.getMagy()));
            may.setText("my: " + sensorHelper.getMagy());
            sb.append(" Magz:");
            sb.append(String.valueOf(sensorHelper.getMagz()));
            maz.setText("mz: " + sensorHelper.getMagz());

            sb.append("\n");

            // Write data in buffer
            String msg = sb.toString();
            dataBuffer.add(msg);
            if(dataBuffer.size() > 100){
                writeBufferToFile();
            }
            pb.setProgress(dataBuffer.size());
            // Write data in file
            //writeToTextView(scrollTextView, scrollView, msg);
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
        if(this.dataBuffer.size() > 0){
            (new WriteLogToFileTask()).execute();
            txtfile.setText("Writting to file...." + Environment.getExternalStorageDirectory() + "/" + logFileName);
        }
    }

    private class WriteLogToFileTask extends AsyncTask<Void, Void, Boolean> {

        String error = "";
        private Queue<String> buffer = new ConcurrentLinkedQueue<String>();
        WriteLogToFileTask(){
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                File path = Environment.getExternalStorageDirectory();
                File file = new File(path, logFileName);
                Log.e("Debug", "Writting to file...." + path + "/" + logFileName);
                FileOutputStream stream = new FileOutputStream(file, true);
                try {
                    for(int i = 0; i < 100 && !dataBuffer.isEmpty(); i++){
                        String msg = dataBuffer.poll();
                        stream.write((msg).getBytes());
                    }
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
                error = e.toString();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if(b){
                File path = Environment.getExternalStorageDirectory();
                writeToTextView(scrollTextView, scrollView, "\n\nWrote to:" + path + "/" +  logFileName+"\n\n");
                txtfile.setText("Wrote to:" + path + "/" +  logFileName);
            } else {
                writeToTextView(scrollTextView, scrollView, "\n\nError writing to file..."+this.error+"\n\n");
                txtfile.setText("Error writing to file..." + this.error);
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


}
