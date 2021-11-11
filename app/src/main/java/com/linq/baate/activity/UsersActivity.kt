package com.linq.baate.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.linq.baate.R
import com.linq.baate.adapter.UserAdapter
import com.linq.baate.firebase.FirebaseService
import com.linq.baate.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.linq.baate.model.CurrentUser
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_users.*
import java.lang.Exception
import java.net.InetAddress
import android.net.ConnectivityManager
import com.vanniktech.emoji.google.GoogleEmojiProvider

import com.vanniktech.emoji.EmojiManager


class UsersActivity : AppCompatActivity() {
    var userList = ArrayList<User>()
    var PERMISSIONS_REQUEST_READ_CONTACTS = 100
    var builder = StringBuilder()

    companion object {
        var contacts: HashMap<String, String> = HashMap()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)
        contacts = HashMap()
        FirebaseService.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            FirebaseService.token = it.token
        }
        loadContacts()

        userRecyclerView.layoutManager = LinearLayoutManager(this)

        EmojiManager.install(GoogleEmojiProvider())


        imgProfile.setOnClickListener {
            val intent = Intent(
                this@UsersActivity,
                ProfileActivity::class.java
            )
            startActivity(intent)
        }
        getUsersList()
        if (supportActionBar != null)
            this.supportActionBar?.hide();
    }

    fun getUsersList() {
        val firebase: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

        var userid = firebase.uid
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/$userid")


        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("Users")


        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()


                for (dataSnapShot in snapshot.children) {
                    val user = dataSnapShot.getValue(User::class.java)
                    if (!user!!.userId.equals(firebase.uid)) {
                        if (contacts.containsKey(user.phone)) {
                            user.userName = contacts.get(user.phone).toString()
                            userList.add(user)
                        } else if (contacts.containsKey("+91${user.phone}")) {
                            user.userName = contacts.get("+91${user.phone}").toString()
                            userList.add(user)
                        }


                    } else {

                        CurrentUser.userId = user!!.userId
                        CurrentUser.userName = user!!.userName
                        CurrentUser.phone = user!!.phone
                        if (user!!.profileImage == "") {
                            imgProfile.setImageResource(R.drawable.profile_image)
                        } else {
                            CurrentUser.profileImage = user.profileImage
                            Picasso.with(this@UsersActivity).load(user.profileImage)
                                .into(imgProfile)
                        }
                    }
                }

                val userAdapter = UserAdapter(this@UsersActivity, userList)

                userRecyclerView.adapter = userAdapter
            }

        })
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                Toast.makeText(
                    this,
                    "Permission must be granted in order to display contacs informtaion",
                    Toast.LENGTH_LONG
                ).show()
                //  toast("Permission must be granted in order to display contacts information")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadContacts() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
            //callback onRequestPermissionsResult
        } else {
            builder = getContacts()
//            listContacts.text = builder.toString()
        }
    }

    @SuppressLint("Range")
    private fun getContacts(): StringBuilder {
        val builder = StringBuilder()
        val resolver: ContentResolver = contentResolver;
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, null, null, null,
            null
        )

        if (cursor != null && cursor!!.count > 0) {
            contacts.clear()
            while (cursor!!.moveToNext()) {
                val id = cursor.getString(cursor!!.getColumnIndex(ContactsContract.Contacts._ID))
                val name =
                    cursor.getString(cursor!!.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val phoneNumber = (cursor.getString(
                    cursor!!.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                )).toInt()

                if (phoneNumber > 0) {
                    val cursorPhone = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        arrayOf(id),
                        null
                    )

                    if (cursorPhone != null && cursorPhone!!.count > 0) {
                        while (cursorPhone.moveToNext()) {
                            val phoneNumValue = cursorPhone.getString(
                                cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            contacts.put(phoneNumValue, name)
                            builder.append("Contact: ").append(name).append(", Phone Number: ")
                                .append(
                                    phoneNumValue
                                ).append("\n\n")

                        }
                    }
                    cursorPhone!!.close()
                }
            }
        } else {
            //   toast("No contacts available!")
        }
        cursor!!.close()
        return builder
    }
}