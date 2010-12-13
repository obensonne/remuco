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

import remuco.client.android.PlayerAdapter;
import remuco.client.android.R;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import net.technologichron.manacalc.NumberPicker;

import java.util.HashSet;
import java.util.Set;

public class ConnectDialog extends Dialog implements OnClickListener, OnCheckedChangeListener{

	PlayerAdapter player;
	
	Button okButton;
	Button cancelButton;

    RadioGroup typeTV;
	EditText hostnameTV;
    NumberPicker portTV;
    ListView bluedeviceTV;
    Set<BluetoothDevice> devices;
	
	public ConnectDialog(Context context, PlayerAdapter player) {
		super(context);
		this.player = player;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.connect_dialog);
		setTitle(R.string.connect_dialog_title);
	
		// get references
		okButton = (Button) findViewById(R.id.connect_dialog_ok_button);
		cancelButton = (Button) findViewById(R.id.connect_dialog_cancel_button);
	
        typeTV = (RadioGroup) findViewById(R.id.connect_dialog_type);
		hostnameTV = (EditText) findViewById(R.id.connect_dialog_hostname);
        portTV = (NumberPicker) findViewById(R.id.connect_dialog_port);
        bluedeviceTV = (ListView) findViewById(R.id.connect_dialog_bluedevice);
        bluedeviceTV.setItemsCanFocus(true);
        bluedeviceTV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Get the local Bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_single_choice);

        // Find and set up the ListView for paired devices
        bluedeviceTV.setAdapter(mArrayAdapter);

        devices = new HashSet<BluetoothDevice>();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
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

		// setup listener
        typeTV.setOnCheckedChangeListener(this);
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
	}

	public void setType(int type){
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

	public int getSelectedType(){
		return typeTV.getCheckedRadioButtonId();
	}

	public void setHostname(String hostname){
		hostnameTV.setText(hostname);
	}

	public String getSelectedHostname(){
		return hostnameTV.getText().toString();
	}

	public void setPort(int port){
		portTV.setValue(port);
	}

	public int getSelectedPort(){
		return portTV.getValue();
	}

	public void setBluedevice(String bluedevice){
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

	public String getSelectedBluedevice(){
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
	
	@Override
	public void onClick(View v) {
		
		if(v == okButton){
			this.dismiss();
		}
		
		if(v == cancelButton){
			this.cancel();
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
}
