package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.BuildConfig;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private static final String TAG = "LoginActivity";
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Intentar actualizar el proveedor de seguridad para evitar errores de conexión con GMS
        upgradeSecurityProvider();

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);
        credentialManager = CredentialManager.create(this);

        Button btnGoogle = findViewById(R.id.btnGoogleSignIn);
        btnGoogle.setOnClickListener(v -> iniciarLoginGoogle());
    }

    private void upgradeSecurityProvider() {
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
                Log.i(TAG, "Security provider installed successfully");
            }

            @Override
            public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
                Log.e(TAG, "Security provider installation failed, errorCode: " + errorCode);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya hay sesión activa, ir directamente a MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            aplicarUsuarioYNavegar(currentUser);
        }
    }

    @android.annotation.SuppressLint("CredentialManager")
    private void iniciarLoginGoogle() {
        showLoading(true);

        // Intentar primero con todas las cuentas para que el usuario pueda elegir
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Permite elegir cualquier cuenta de Google del dispositivo
                .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, executor, new androidx.credentials.CredentialManagerCallback<>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleSignIn(result.getCredential());
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                Log.e(TAG, "Credential Manager error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showLoading(false);
                    if (e instanceof NoCredentialException) {
                        Toast.makeText(LoginActivity.this, "No se encontraron cuentas de Google vinculadas.", Toast.LENGTH_LONG).show();
                    } else if (!(e instanceof androidx.credentials.exceptions.GetCredentialCancellationException)) {
                        Toast.makeText(LoginActivity.this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof CustomCredential && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } catch (Exception e) {
                Log.e(TAG, "Error parsing Google ID Token", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            Log.e(TAG, "Unexpected credential type: " + credential.getType());
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        aplicarUsuarioYNavegar(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase signInWithCredential failed", e);
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                });
    }

    private void aplicarUsuarioYNavegar(FirebaseUser user) {
        // Asignar userId a todos los servicios
        String uid = user.getUid();
        RecetasSrv.setUserId(uid);
        CalendarioSrv.setUserId(uid);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> progressBar.setVisibility(show ? View.VISIBLE : View.GONE));
    }
}
