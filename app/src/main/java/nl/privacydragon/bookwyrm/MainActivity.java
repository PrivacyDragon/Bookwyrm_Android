package nl.privacydragon.bookwyrm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            //Context context = getActivity();
            SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
            String defaultValue = "none";
            String server = sharedPref.getString(getString(R.string.server), defaultValue);
            if (server != "none") {
                startActivity(new Intent(MainActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
            }
        }


        public void LogIn(View view) {
            EditText serverInput = (EditText) findViewById(R.id.Instance);
            String server = String.valueOf(serverInput.getText());
            EditText passInput = (EditText) findViewById(R.id.Password);
            String pass = String.valueOf(passInput.getText());
            EditText nameInput = (EditText) findViewById(R.id.Username);
            String name = String.valueOf(nameInput.getText());
            if (server.isEmpty() || pass.isEmpty() || name.isEmpty()) {
                TextView ErrorMessage = (TextView) findViewById(R.id.textView5);
                ErrorMessage.setTextColor(Color.RED);
                ErrorMessage.setText("ERROR: All fields are required!");
            } else {
                SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefName = MainActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefPass = MainActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
                SharedPreferences.Editor editorName = sharedPrefName.edit();
                SharedPreferences.Editor editorPass = sharedPrefPass.edit();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.server), server);
                editor.apply();
                editorName.putString(getString(R.string.name), name);
                editorName.apply();
                editorPass.putString(getString(R.string.pw), pass);
                editorPass.apply();
                startActivity(new Intent(MainActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
            }
        }
    }