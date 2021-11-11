package com.linq.baate.activity

import android.R.attr
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.linq.baate.R
import kotlinx.android.synthetic.main.activity_login.*

import android.R.attr.phoneNumber
import android.transition.Visibility
import android.util.Log
import android.view.View
import androidx.constraintlayout.motion.widget.Key
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*

import java.util.concurrent.TimeUnit
import com.google.firebase.auth.PhoneAuthProvider

import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.FirebaseDatabase


class LoginActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var firebaseUser: FirebaseUser? = null
    var screen = "phone"
    private var phone = ""
    private var mVerificationId:String=""
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        if(auth!!.currentUser!=null)

            firebaseUser = auth!!.currentUser!!

        //check if user login then navigate to user screen
        if (firebaseUser != null) {
            val intent = Intent(
                this@LoginActivity,
                UsersActivity::class.java
            )
            startActivity(intent)
            finish()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d("TAG", "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w("TAG", "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d("TAG", "onCodeSent:$verificationId")
                mVerificationId=verificationId
            }
        }
        btnLogin.setOnClickListener {
            if (screen == "phone") {
                if (phone_edit.text.toString().length == 10) {
                    screen="otp"
                    phone = phone_edit.text.toString()
                    btnLogin.text = "Verify"
                    filledTextField.hint = "Enter OTP"
                    filledTextField.isErrorEnabled = false
                    authenticate(" +91$phone")
                    phone_edit.setText("")
                    resend.visibility= View.VISIBLE
                    changeno.visibility=View.VISIBLE
                } else {
                    filledTextField.error = "Enter Valid Number"
                    filledTextField.isErrorEnabled = true

                }

            } else {
                if(!phone_edit.text.toString().isNullOrBlank()) {
                    filledTextField.isErrorEnabled = false
                    verify(phone_edit.text.toString())
                }
                else {
                    filledTextField.error = "Enter Valid OTP"
                    filledTextField.isErrorEnabled = true
                }
            }


        }
        resend.setOnClickListener {
            authenticate("+91$phone")
        }
        changeno.setOnClickListener {
            screen="phone"
            filledTextField.hint = "Enter Your Phone Number"
            filledTextField.isErrorEnabled = false
            btnLogin.text="Get OTP"
            resend.visibility= View.INVISIBLE
            changeno.visibility=View.INVISIBLE
            phone_edit.setText("")

        }


    }

    fun authenticate(phoneNo:String)
    {
        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phoneNo) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }
    fun verify(otp: String) {
        verifyVerificationCode(otp)

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "signInWithCredential:success")
                    val user = task.result?.user
                    val userId:String = user!!.uid

                   var  databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

                    val creationTimestamp = user.metadata!!.creationTimestamp
                    val lastSignInTimestamp = user.metadata!!.lastSignInTimestamp
                    if (creationTimestamp == lastSignInTimestamp) {
                        val hashMap:HashMap<String,String> = HashMap()
                        hashMap.put("userId",userId)
                        hashMap.put("phone",phone)
                        hashMap.put("userName",phone)
                        hashMap.put("profileImage","")
                        databaseReference.setValue(hashMap)
                    }

                    val intent = Intent(
                        this@LoginActivity,
                        UsersActivity::class.java
                    )
                    startActivity(intent)
                    finish()

                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w("TAG", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                      Toast.makeText(this,"Wrong COde",Toast.LENGTH_LONG).show()
                    }
                    // Update UI
                }
            }

    }

    private fun verifyVerificationCode(otp: String) {
        //creating the credential
    try
    {
        val credential = PhoneAuthProvider.getCredential(mVerificationId, otp)

        //signing the user
        signInWithPhoneAuthCredential(credential)
    }
    catch (e:Exception)
    {
        Toast.makeText(this,"Wrong COde",Toast.LENGTH_LONG).show()
    }

    }

}