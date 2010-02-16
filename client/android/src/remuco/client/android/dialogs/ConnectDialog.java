package remuco.client.android.dialogs;

import remuco.client.android.PlayerAdapter;
import remuco.client.android.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


public class ConnectDialog extends Dialog implements OnClickListener{

	PlayerAdapter player;
	
	Button okButton;
	Button cancelButton;
	
	EditText hostnameTV;
	
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
	
		hostnameTV = (EditText) findViewById(R.id.connect_dialog_hostname);

		// setup listener
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
	}

	public void setHostname(String hostname){
		hostnameTV.setText(hostname);
	}

	public String getSelectedHostname(){
		return hostnameTV.getText().toString();
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
	
}
