package com.david.recetapp.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.david.recetapp.MainActivity;
import com.david.recetapp.R;
import com.david.recetapp.negocio.servicios.CalendarioSrv;
import com.david.recetapp.negocio.servicios.RecetasSrv;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ProgressBar progressBar;
    private static final String TAG = "LoginActivity";

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    // Log detailed info to help diagnose ApiException 10 (DEVELOPER_ERROR)
                    Log.w(TAG, "Google sign-in failed, statusCode=" + e.getStatusCode(), e);
                    showLoading(false);
                    Toast.makeText(this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Intentar actualizar el proveedor de seguridad para evitar errores de conexión con GMS
        upgradeSecurityProvider();

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Auto-generado por google-services.json
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

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

    private void iniciarLoginGoogle() {
        showLoading(true);
        // Cerrar sesión de Google primero para forzar el selector
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            signInLauncher.launch(signInIntent);
        });
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
                    // Log Firebase auth failures for easier debugging
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
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}