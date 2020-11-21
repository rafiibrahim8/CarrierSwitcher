package ml.nerdsofku.carrierswitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    public static final int LEN_SHA256_CHAR=64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean biometricAvailable = isBiometricAvailable();

        if(is_first_login(biometricAvailable)){
            return;
        }

        if(biometricAvailable && isBioPrefer()){
            loginUsingBiometric();
        }else if(!biometricAvailable && isBioPrefer()){
            try {
                new File(getFilesDir(),getString(R.string.bio_file)).delete();
                is_first_login(false,true);
            } catch (Exception ex){}

        } else{
            setContentView(R.layout.activity_main);
            legacyLogin();
        }
    }

    private boolean isBiometricAvailable() {
        return BiometricManager.from(MainActivity.this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private boolean is_first_login(boolean bioAvail){
        return is_first_login(bioAvail,false);
    }
    private boolean is_first_login(boolean bioAvail, boolean force) {
        if(force || !new File(getFilesDir(),getString(R.string.pass_file)).exists()){
            setContentView(R.layout.activity_main);
            final View view = getLayoutInflater().inflate(R.layout.alert_dialog,null);
            view.findViewById(R.id.adOldPass).setVisibility(View.GONE);
            view.findViewById(R.id.adLeaveEmpty).setVisibility(View.VISIBLE);
            final EditText newPass1 = view.findViewById(R.id.adNewPass1);
            final EditText newPass2 = view.findViewById(R.id.adNewPass2);
            final TextView err = view.findViewById(R.id.adError);
            final CheckBox useBio = view.findViewById(R.id.use_bio);
            if(! bioAvail){
                useBio.setEnabled(false);
            }
            useBio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b){
                        newPass1.setEnabled(false);
                        newPass2.setEnabled(false);
                    }
                    else{
                        newPass1.setEnabled(true);
                        newPass2.setEnabled(true);
                    }
                }
            });
            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.setpass).setView(view).setPositiveButton(R.string.set, null).setCancelable(false).show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(useBio.isChecked()){
                        storedPass(true);
                        loginUsingBiometric();
                        alertDialog.dismiss();
                        return;
                    }
                    if(newPass1.getText().toString().equals(newPass2.getText().toString())){
                        storedPass(newPass1.getText().toString());
                        legacyLogin();
                        alertDialog.dismiss();
                    }
                    else {
                        showTvError(R.string.pass_missmatch,err);
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void storedPass(boolean b) {
        if(b){
            try {
                new FileOutputStream(new File(getFilesDir(),getString(R.string.bio_file))).write(0xFF);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,R.string.err_saving_file,Toast.LENGTH_SHORT).show();
            }
        }
        storedPass("");
    }

    private boolean isBioPrefer() {
        return new File(getFilesDir(),getString(R.string.bio_file)).exists();
    }

    private void loginUsingBiometric(){
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, ContextCompat.getMainExecutor(MainActivity.this), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Toast.makeText(MainActivity.this,"Authentication Error.",Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                startRadioInfo();
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(MainActivity.this,"Authentication Failed.",Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login for " + getResources().getString(R.string.app_name))
                .setSubtitle("Login using your device credential.")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void legacyLogin(){
            String sPass = storedPass();
            if(sPass.isEmpty() || getString(R.string.sha256empty).equals(sPass)){
                startRadioInfo();
                return;
            }

        Button go = findViewById(R.id.go);
        TextView chPass = findViewById(R.id.chPass);

        chPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getLayoutInflater().inflate(R.layout.alert_dialog,null);
                final EditText oldPass = view.findViewById(R.id.adOldPass);
                final EditText newPass1 = view.findViewById(R.id.adNewPass1);
                final EditText newPass2 = view.findViewById(R.id.adNewPass2);
                final TextView err = view.findViewById(R.id.adError);
                final CheckBox useBio = view.findViewById(R.id.use_bio);

                useBio.setEnabled(isBiometricAvailable());
                useBio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b){
                            newPass1.setEnabled(false);
                            newPass2.setEnabled(false);
                        }
                        else{
                            newPass1.setEnabled(true);
                            newPass2.setEnabled(true);
                        }
                    }
                });

                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.changePass).setPositiveButton(R.string.change, null).setNegativeButton(R.string.cancel,null).setView(view).setCancelable(false).show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(storedPass().equals(mkHash(oldPass.getText().toString()))){
                            if(useBio.isChecked()){
                                storedPass(true);
                                alertDialog.dismiss();
                                return;
                            }
                            if(newPass1.getText().toString().equals(newPass2.getText().toString())){
                                storedPass(newPass1.getText().toString());
                                Toast.makeText(MainActivity.this,R.string.pass_change_success,Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            }
                            else {
                                showTvError(R.string.pass_missmatch,err);
                            }
                        }
                        else {
                            showTvError(R.string.old_pass_error,err);
                        }
                    }
                });

            }
        });

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mkHash(((EditText)findViewById(R.id.password)).getText().toString()).equals(storedPass())){
                    startRadioInfo();
                }
                else {
                    Toast.makeText(MainActivity.this,R.string.incorrect_pass,Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String mkHash(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Toast.makeText(this,R.string.checksum_unsupported,Toast.LENGTH_LONG).show();
            return "";
        }
        byte[] digest = md.digest(s.getBytes());
        System.out.println(new String(digest));
        String checksum = "";
        for (byte b : digest) {
            String t = Integer.toHexString(b & 0xFF);
            checksum = checksum + (t.length() == 2 ? t : 0 + t);
        }
        return checksum;
    }

    private void showTvError(int msg, TextView err) {
        err.setText(msg);
        err.setVisibility(View.VISIBLE);
    }

    private void startRadioInfo() {
        Intent intent = new Intent(getString(R.string.action));
        intent.setClassName(getString(R.string.packageName),getString(R.string.className));
        startActivity(intent);
        finish();
    }

    private void storedPass(String pass) {
        try {
            new FileOutputStream(new File(getFilesDir(),getString(R.string.pass_file))).write(mkHash(pass).getBytes());
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,R.string.err_saving_file,Toast.LENGTH_SHORT).show();
        }
    }

    private String storedPass() {
        byte[] bytes = new byte[LEN_SHA256_CHAR];
        try {
            new FileInputStream(new File(getFilesDir(),getString(R.string.pass_file))).read(bytes);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,R.string.err_file_read,Toast.LENGTH_SHORT).show();
        }
        return new String(bytes);
    }
}
