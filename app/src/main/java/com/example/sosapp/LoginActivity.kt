package com.example.sosapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.sosapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy{
        ActivityLoginBinding.inflate(layoutInflater)
    }
    lateinit var auth: FirebaseAuth
    override fun onStart() {
        super.onStart()
        // Check if user already Logged in
        val currentuser:FirebaseUser? = auth.currentUser
        if(currentuser!=null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // Initialize Firebase Auth
        auth= FirebaseAuth.getInstance()


        binding.loginbtn.setOnClickListener {
            val email= binding.mail.text.toString()
            val pass= binding.password.text.toString()
            if(email.isEmpty()||pass.isEmpty()){
                Toast.makeText(this, "Please Fill the details!", Toast.LENGTH_SHORT).show()
            }
            else{
                auth.signInWithEmailAndPassword(email,pass)
                    .addOnCompleteListener { task->
                        if(task.isSuccessful){
                            Toast.makeText(this, "Login Succesful✌️", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this,MainActivity::class.java))
                            finish()
                        }
                        else{
                            Toast.makeText(this,"Login Failed ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        binding.signupbtn.setOnClickListener {
            startActivity(Intent(this,SignupActivity::class.java))
            finish()
        }
    }
}