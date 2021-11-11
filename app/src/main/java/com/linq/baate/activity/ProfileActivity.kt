package com.linq.baate.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.linq.baate.R
import com.linq.baate.model.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.activtiy_profile.*
import java.io.IOException
import java.util.*

class ProfileActivity : AppCompatActivity() {


    private lateinit var firebaseUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    var usernamechange=false
    private var filePath: Uri? = null

    private val PICK_IMAGE_REQUEST: Int = 2020

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtiy_profile)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        databaseReference =
            FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.uid)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if(!usernamechange)
                    btnSave.visibility=View.INVISIBLE
                if (user?.profileImage == "") {
                    userImage.setImageResource(R.drawable.profile_image)
                } else {
                    if(!this@ProfileActivity.isDestroyed)
                      Picasso.with(this@ProfileActivity).load(user?.profileImage).into(userImage)
                }
            }
        })

        imgBack.setOnClickListener {
            onBackPressed()
        }

        userImage.setOnClickListener {
            chooseImage()
        }

        btnSave.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            uploadImage()
            if(usernamechange)
                databaseReference.child("userName").setValue(etUserName.text.toString())

        }
        etUserName.addTextChangedListener(object:TextWatcher
        {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                btnSave.visibility = View.INVISIBLE
                usernamechange=true
            }
        })

    }

    private fun chooseImage() {
        val intent: Intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode != null && data!=null) {
            filePath = data!!.data
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                userImage.setImageBitmap(bitmap)
                uploadImage()
                progressBar.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        if (filePath != null) {

            var ref: StorageReference = storageRef.child("image/" + FirebaseAuth.getInstance().currentUser?.uid.toString())
            ref.putFile(filePath!!)
                .addOnSuccessListener {

                    val downloadUri: Task<Uri> = it.storage.downloadUrl
                    downloadUri.addOnCompleteListener{
                        var generatedFilePath = downloadUri.getResult().toString();


                        databaseReference.child("profileImage").setValue(generatedFilePath).addOnCompleteListener{
                            progressBar.visibility = View.GONE
                            Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                            btnSave.visibility = View.GONE
                        }
                    }

                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()

                }

        }else
        {
            progressBar.visibility = View.GONE
        }
    }


}