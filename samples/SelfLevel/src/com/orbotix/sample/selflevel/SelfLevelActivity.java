package com.orbotix.sample.selflevel;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import orbotix.robot.base.*;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;

public class SelfLevelActivity extends Activity {

    private SpheroConnectionView mSpheroConnectionView;

    /** Robot to from which we are streaming */
    private Sphero mRobot = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Find Sphero Connection View from layout file
        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        // This event listener will notify you when these events occur, it is up to you what you want to do during them
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected(Robot sphero) {
                mRobot = (Sphero) sphero;
                // Hide the connection view. Comment this code if you want to connect to multiple robots
                mSpheroConnectionView.setVisibility(View.INVISIBLE);
                // Set the AsyncDataListener that will process self level complete async responses
                DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener);
            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                // let the SpheroConnectionView handle or hide it and do something here...
            }

            @Override
            public void onDisconnected(Robot sphero) {
                mSpheroConnectionView.startDiscovery();
            }

        });
    }

    /** Called when the user comes back to this app */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list of Spheros
        mSpheroConnectionView.startDiscovery();
    }

    /** Called when the user presses the back or home button */
    @Override
    protected void onPause() {
        super.onPause();
        // Remove the AsyncDataListener that will process self level complete async responses
        DeviceMessenger.getInstance().removeAsyncDataListener(mRobot, mDataListener);
        // Disconnect Robot properly
        RobotProvider.getDefaultProvider().disconnectControlledRobots();
    }

    /**
     * When self level button is pressed, this method is called
     *
     * @param v
     */
    public void selfLevelPressed(View v) {

        if (mRobot == null) return;

        int angleLimit = 0;   /**         0: uses the default value in the config block
         1 to 90: the max angle for completion (in degrees) */

        int timeout = 0;      /**         0: uses the default value in the config block
         1 to 255: the max seconds to run the routine */

        int accuracy = 0;     /**        0: uses the default value in the config block
         1 to 255: the accuracy for the routine ot 10*Accuracy (in ms) */

        // Try parsing the integer values from the edit text boxes, if not, use zeros, the default values
        try {
            angleLimit = Integer.parseInt(((EditText) findViewById(R.id.txtbox_angle_limit)).getText().toString());
        } catch (NumberFormatException e) {
        }

        try {
            timeout = Integer.parseInt(((EditText) findViewById(R.id.txtbox_timeout)).getText().toString());
        } catch (NumberFormatException e) {
        }

        try {
            accuracy = Integer.parseInt(((EditText) findViewById(R.id.txtbox_accuracy)).getText().toString());
        } catch (NumberFormatException e) {
        }

        // Create options flag with the bit on telling self level to start
        int options = SelfLevelCommand.OPTION_START;

        // Logical OR the bit for returning to heading before self level was called after the command completes
        // If check box is checked, we will return Sphero to heading
        if (((CheckBox) findViewById(R.id.checkbox_heading)).isChecked())
            options |= SelfLevelCommand.OPTION_KEEP_HEADING;

        // Logical OR the bit for falling to sleep after self level
        // If check box is checked, Sphero will sleep after
        if (((CheckBox) findViewById(R.id.checkbox_sleep)).isChecked())
            options |= SelfLevelCommand.OPTION_SLEEP_AFTER;

        // Logical OR the bit for the control system state after the self level
        // If check box is checked, the control system will be on after
        if (((CheckBox) findViewById(R.id.checkbox_control_system)).isChecked())
            options |= SelfLevelCommand.OPTION_CONTROL_SYSTEM_ON;

        // Send the Self Level Command with parameters from the UI
        SelfLevelCommand.sendCommand(mRobot, options, angleLimit, timeout, accuracy);

        // Let the user know the ball is self leveling
        ((TextView) findViewById(R.id.txt_self_level)).setText("Self Leveling...");
    }

    /**
     * When abort button is pressed this method is called
     *
     * @param v
     */
    public void abortPressed(View v) {
        // Easy command to abort self level
        SelfLevelCommand.sendCommandAbortSelfLevel(mRobot);

        // Let the user know the ball is aborting self level
        ((TextView) findViewById(R.id.txt_self_level)).setText("Aborting...");
    }


    /** AsyncDataListener that will be assigned to the DeviceMessager, listen for streaming data, and print result */
    private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
        @Override
        public void onDataReceived(DeviceAsyncData data) {

            if (data instanceof SelfLevelCompleteAsyncData) {

                // Cast the self level complete object
                SelfLevelCompleteAsyncData resultCode = (SelfLevelCompleteAsyncData) data;

                // Print result to text view at the top of the screen
                TextView resultTextView = (TextView) SelfLevelActivity.this.findViewById(R.id.txt_self_level);

                // Print result code in human readable format
                switch (resultCode.getResultCode()) {

                    case SelfLevelCompleteAsyncData.RESULT_CODE_UKNOWN:
                        resultTextView.setText("Unknown Error");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_TIMEOUT:
                        resultTextView.setText("Timeout");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_SENSORS_ERROR:
                        resultTextView.setText("Sensors Error");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_SELF_LEVEL_DISABLED:
                        resultTextView.setText("Self Level Disabled");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_ABORTED:
                        resultTextView.setText("Aborted");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_CHARGER_NOT_FOUND:
                        resultTextView.setText("Charger Not Found");
                        break;

                    case SelfLevelCompleteAsyncData.RESULT_CODE_SUCCESS:
                        resultTextView.setText("Success");
                        break;

                }
            }
        }
    };
}
