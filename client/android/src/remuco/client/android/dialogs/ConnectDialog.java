/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.client.android.dialogs;

import remuco.client.android.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Dialog that presents a new connection dialog.
 * The dialog calls back to its creator activity. This creator activity should
 * implement {@link ConnectRequestHandler} (This is as described in 
 *  http://android-developers.blogspot.in/2012/05/using-dialogfragments.html)
 */
public class ConnectDialog extends DialogFragment implements OnClickListener, OnCheckedChangeListener{

    //These static strings are used for key names
    private static String arg_type = "type";
    private static String arg_hostname = "hostname";
    private static String arg_port = "port";
    private static String arg_bluedevice = "bluedevice";
        
    Button okButton;
    Button cancelButton;

    RadioGroup typeTV;
    EditText hostnameTV;
    EditText portTV;
    ListView bluedeviceTV;
    Set<BluetoothDevice> devices;
    
    
    /**
     * Interface that the creating activity should implement.
     */
    public interface ConnectRequestHandler {
        public void connectWifi(String hostname, int port);
        public void connectBluetooth(String device);
    }
    
    
    public static ConnectDialog newInstance(int type, String hostname, int port, String bluedevice) {
        ConnectDialog connectdialog = new ConnectDialog();
        
        Bundle args = new Bundle();
        args.putInt(arg_type, type);
        args.putString(arg_hostname, hostname);
        args.putInt(arg_port, port);
        args.putString(arg_bluedevice, bluedevice);
        connectdialog.setArguments(args);
        
        return connectdialog;
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.connect_dialog, container);
        getDialog().setTitle(R.string.connect_dialog_title);
    
        // get references
        okButton = (Button) view.findViewById(R.id.connect_dialog_ok_button);
        cancelButton = (Button) view.findViewById(R.id.connect_dialog_cancel_button);
    
        typeTV = (RadioGroup) view.findViewById(R.id.connect_dialog_type);
        hostnameTV = (EditText) view.findViewById(R.id.connect_dialog_hostname);
        portTV = (EditText) view.findViewById(R.id.connect_dialog_port);
        portTV.setFilters(new InputFilter[]{ new InputFilterMinMax(0, 65535)});
        bluedeviceTV = (ListView) view.findViewById(R.id.connect_dialog_bluedevice);
        bluedeviceTV.setItemsCanFocus(true);
        bluedeviceTV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Find and set up the ListView for paired devices
        ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_single_choice);
        bluedeviceTV.setAdapter(mArrayAdapter);

        // Get the local Bluetooth adapter and paired devices
        devices = new HashSet<BluetoothDevice>();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                BluetoothClass bluetoothClass = device.getBluetoothClass();
                if (bluetoothClass != null &&
                    bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName());
                    devices.add(device);
                }
            }
        }
        
        // Set initial values
        setType(getArguments().getInt(arg_type));
        setPort(getArguments().getInt(arg_port));
        setHostname(getArguments().getString(arg_hostname));
        setBluedevice(getArguments().getString(arg_bluedevice));

        // setup listener
        typeTV.setOnCheckedChangeListener(this);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        
        return view;
    }

    private void setType(int type){
        typeTV.check(type);

        if (type == R.id.connect_dialog_bluetooth) {
            showBluetooth();
            hideWifi();
        }
        if (type == R.id.connect_dialog_wifi) {
            showWifi();
            hideBluetooth();
        }
    }

    private int getSelectedType(){
        return typeTV.getCheckedRadioButtonId();
    }

    private void setHostname(String hostname){
        hostnameTV.setText(hostname);
    }

    private String getSelectedHostname(){
        return hostnameTV.getText().toString();
    }

    private void setPort(int port){
        portTV.setText(""+port);
    }

    private int getSelectedPort(){
        return Integer.parseInt(portTV.getText().toString());
    }

    private void setBluedevice(String bluedevice){
        int pos = -1;
        int i = 0;
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equals(bluedevice)) {
                pos = i;
                break;
            }
            i++;
        }

        if (pos == -1) return;
        bluedeviceTV.setItemChecked(pos, true);
    }

    private String getSelectedBluedevice(){
        int pos = bluedeviceTV.getCheckedItemPosition();
        if (pos == ListView.INVALID_POSITION)
            return "";
        int i = 0;
        for (BluetoothDevice device : devices) {
            if (i == pos) {
                return device.getAddress();
            }
            i++;
        }
        return "";
    }

    private void showBluetooth() {
        bluedeviceTV.setVisibility(View.VISIBLE);
    }

    private void hideBluetooth() {
        bluedeviceTV.setVisibility(View.GONE);
    }

    private void showWifi() {
        hostnameTV.setVisibility(View.VISIBLE);
        portTV.setVisibility(View.VISIBLE);
    }

    private void hideWifi() {
        hostnameTV.setVisibility(View.GONE);
        portTV.setVisibility(View.GONE);
    }
    
    @Override
    public void onClick(View v) {
        
        if(v == okButton){
            //Connect button was hit, let the activity connect.
            //Casting as hinted in android-developers.blogspot.
            ConnectRequestHandler activity = (ConnectRequestHandler) getActivity();
            
            if(getSelectedType() == R.id.connect_dialog_bluetooth) {
                activity.connectBluetooth(getSelectedBluedevice());
            } else if (getSelectedType() == R.id.connect_dialog_wifi) {
                activity.connectWifi(getSelectedHostname(), getSelectedPort());
            }
            
            this.dismiss();
        }
        
        if(v == cancelButton){
            //cancel button was hit, no actions to perform.
            this.dismiss();
        }
    }
    
    @Override
    public void onCheckedChanged(RadioGroup group, int type) {
        
        if (type == R.id.connect_dialog_bluetooth) {
            showBluetooth();
            hideWifi();
        }
        if (type == R.id.connect_dialog_wifi) {
            showWifi();
            hideBluetooth();
        }
    }
    
    
    /**
     * Simple class that filters the number in the number input field.
     * TODO: Can make this a bit nicer?
     */
    private class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {   
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }     
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }
}
