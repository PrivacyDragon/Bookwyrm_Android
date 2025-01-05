package nl.privacydragon.bookwyrm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            //Check whether there is something stored. Only if there is already something stored, proceed to BookWyrm.
            SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
            String defaultValue = "none";
            String server = sharedPref.getString(getString(R.string.server), defaultValue);
            if (!"none".equals(server)) {
                startActivity(new Intent(MainActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
            }
        }

        private static String getRandomString() //I just copied this from internet. Yes, I am lazy :). (https://stackoverflow.com/questions/12116092/android-random-string-generator#answer-12116194)
        {
            String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm!@#$%^&*()_+=][{}";
            final Random random=new Random();
            final StringBuilder sb=new StringBuilder(12);
            for(int i = 0; i< 12; ++i) {
                sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
            }
            return sb.toString();
        }

        public void LogIn(View view) throws IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, UnrecoverableKeyException, NoSuchPaddingException, InvalidKeyException {
            //Declaring some things needed. Getting the user input.
            EditText serverInput = (EditText) findViewById(R.id.Instance);
            String server = String.valueOf(serverInput.getText());
            EditText passInput = (EditText) findViewById(R.id.Password);
            String pass = String.valueOf(passInput.getText());
            EditText nameInput = (EditText) findViewById(R.id.Username);
            String name = String.valueOf(nameInput.getText());
            //All fields are required, so if one of them is empty, the user should see a warning.
            if (server.isEmpty() || pass.isEmpty() || name.isEmpty()) {
                TextView ErrorMessage = (TextView) findViewById(R.id.textView5);
                ErrorMessage.setTextColor(Color.YELLOW);
                ErrorMessage.setText("ERROR: All fields are required!");
            } else {
                //Likely this will be the first time the program is run. So create a new key thing in the android key store happening.
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (!keyStore.containsAlias("BookWyrm")) { //Actually, the new key is made here, if it does not exist already.
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                    keyGenerator.init(
                            new KeyGenParameterSpec.Builder("BookWyrm",
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                    .setRandomizedEncryptionRequired(false)
                                    .build());
                    keyGenerator.generateKey();
                }
                //Grab the key and initiate the encryption process stuff. For this, a random fixed IV code is generated.
                Key DragonLikeKey = keyStore.getKey("BookWyrm", null);
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                String magicCode = getRandomString();
                c.init(Cipher.ENCRYPT_MODE, DragonLikeKey, new GCMParameterSpec(128, magicCode.getBytes()));
                //And now do the encryption!
                byte[] passBytes = c.doFinal(pass.getBytes());
                String passUse = Base64.encodeToString(passBytes, Base64.DEFAULT);
                //And then all the things are stored in the shared preferences.
                //Therefore, first all the shared preferences objects are loaded.
                SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(getString(R.string.server), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefName = MainActivity.this.getSharedPreferences(getString(R.string.name), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefPass = MainActivity.this.getSharedPreferences(getString(R.string.pw), Context.MODE_PRIVATE);
                SharedPreferences sharedPrefMagic = MainActivity.this.getSharedPreferences(getString(R.string.q), Context.MODE_PRIVATE);
                //Then the 'edit' stuff are made for them.
                SharedPreferences.Editor editorName = sharedPrefName.edit();
                SharedPreferences.Editor editorPass = sharedPrefPass.edit();
                SharedPreferences.Editor editorMagic = sharedPrefMagic.edit();
                SharedPreferences.Editor editor = sharedPref.edit();
                //And finally, the values are written to them.
                editor.putString(getString(R.string.server), server);
                editor.apply();
                editorName.putString(getString(R.string.name), name);
                editorName.apply();
                editorPass.putString(getString(R.string.pw), passUse);
                editorPass.apply();
                editorMagic.putString(getString(R.string.q), magicCode);
                editorMagic.apply();
                //Once all that has been done, Bookwyrm can be opened and such!
                startActivity(new Intent(MainActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
            }
        }
    }