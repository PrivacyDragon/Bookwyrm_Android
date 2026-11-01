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
import java.security.SecureRandom;

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
                //Grab the key and initiate the encryption process stuff with a freshly generated GCM IV.
                Key DragonLikeKey = keyStore.getKey("BookWyrm", null);
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);
                c.init(Cipher.ENCRYPT_MODE, DragonLikeKey, new GCMParameterSpec(128, iv));
                String storedIv = encodeStoredGcmIv(iv);
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
                editorMagic.putString(getString(R.string.q), storedIv);
                editorMagic.apply();
                //Once all that has been done, Bookwyrm can be opened and such!
                startActivity(new Intent(MainActivity.this, nl.privacydragon.bookwyrm.StartActivity.class));
            }
        }

        static String encodeStoredGcmIv(byte[] iv) {
            if (iv.length != 12) {
                throw new IllegalArgumentException("GCM IV must contain 12 bytes");
            }
            StringBuilder encodedIv = new StringBuilder("gcm1:");
            for (byte value : iv) {
                encodedIv.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                encodedIv.append(Character.forDigit(value & 0x0f, 16));
            }
            return encodedIv.toString();
        }

        static byte[] decodeStoredGcmIv(String storedIv) {
            if (!storedIv.startsWith("gcm1:")) {
                return storedIv.getBytes();
            }
            if (storedIv.length() != 29) {
                throw new IllegalArgumentException("Stored GCM IV must contain 12 hexadecimal bytes");
            }
            byte[] iv = new byte[12];
            for (int i = 0; i < iv.length; i++) {
                int high = Character.digit(storedIv.charAt(5 + (i * 2)), 16);
                int low = Character.digit(storedIv.charAt(6 + (i * 2)), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("Stored GCM IV must contain hexadecimal bytes");
                }
                iv[i] = (byte) ((high << 4) | low);
            }
            return iv;
        }
    }