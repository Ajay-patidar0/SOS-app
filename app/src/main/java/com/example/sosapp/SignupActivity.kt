package com.example.sosapp
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
 import com.example.sosapp.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class SignupActivity : AppCompatActivity() {
    private val binding: ActivitySignupBinding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("14960865303-ec6r96bsmk7i0inr6hjgtnhbslrge965.apps.googleusercontent.com")
            .requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        binding.loginbtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.register.setOnClickListener {
            // get text from edit text field
            val email = binding.mail.text.toString()
            val name = binding.name.text.toString()
            val pass = binding.password.text.toString()
            val confpass = binding.confpass.text.toString()

            // check if any field is blank
            if (email.isEmpty() || name.isEmpty() || pass.isEmpty() || confpass.isEmpty()) {
                Toast.makeText(this, "Please Fill all the details !", Toast.LENGTH_LONG).show()
            } else if (pass != confpass) {
                Toast.makeText(this, "Confirm password must be same as password", Toast.LENGTH_LONG)
                    .show()
            } else {
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registration Succesful", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Registration Failed ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
            }
        }
        binding.google.setOnClickListener {
            val signInClient = googleSignInClient.signInIntent
            launcher.launch(signInClient)

        }
    }
    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    val account: GoogleSignInAccount? = task.result
                    val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Login Successful 😁", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            Toast.makeText(this, "Failed!", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } else {
                Toast.makeText(this, "Failed!", Toast.LENGTH_LONG).show()
            }

        }
}
   /* private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (task.isSuccessful) {
                    val account: GoogleSignInAccount? = task.result
                    if (account != null) {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                Toast.makeText(this, "Login Successful 😁", Toast.LENGTH_LONG)
                                    .show()
                                startActivity(Intent(this, MainActivity::class.java))
                            } else {
                                Toast.makeText(
                                    this,
                                    "Login Failed: ${signInTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Account information is null", Toast.LENGTH_LONG)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Google Sign-In task unsuccessful", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_LONG).show()
            }
        }
}
*/



/*
package com.example.firebaselogin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaselogin.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class SignupActivity : AppCompatActivity() {
    private val binding: ActivitySignupBinding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("14960865303-ec6r96bsmk7i0inr6hjgtnhbslrge965.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginbtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.register.setOnClickListener {
            // get text from edit text field
            val email = binding.mail.text.toString()
            val name = binding.name.text.toString()
            val pass = binding.password.text.toString()
            val confpass = binding.confpass.text.toString()

            // check if any field is blank
            if (email.isEmpty() || name.isEmpty() || pass.isEmpty() || confpass.isEmpty()) {
                Toast.makeText(this, "Please Fill all the details !", Toast.LENGTH_LONG).show()
            } else if (pass != confpass) {
                Toast.makeText(this, "Confirm password must be same as password", Toast.LENGTH_LONG)
                    .show()
            } else {
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Log.e(TAG, "Registration Failed", task.exception)
                            Toast.makeText(this, "Registration Failed ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.google.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                } else {
                    Toast.makeText(this, "Google Sign In failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google Sign In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Google Sign In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user: FirebaseUser? = auth.currentUser
                    Toast.makeText(this, "Google Sign In successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val TAG = "SignupActivity"
    }
}
*/

