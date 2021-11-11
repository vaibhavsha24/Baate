package com.linq.baate.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.linq.baate.R
import com.linq.baate.RetrofitInstance
import com.linq.baate.adapter.ChatAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.linq.baate.model.*
import com.squareup.picasso.Picasso
import com.vanniktech.emoji.EmojiPopup
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_chat.imgBack
import kotlinx.android.synthetic.main.activtiy_profile.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class ChatActivity : AppCompatActivity() {
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    var reference: DatabaseReference? = null
    var chatList = ArrayList<Chat>()
    var topic = ""
    var phonecurrentChat=""
    private var filePath: Uri? = null

    private val PICK_IMAGE_REQUEST: Int = 2020
    var userId=""
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    companion object
    {
        var currentUserImage:String?=""
        var currentChatno=""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        databaseReference =
            FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.uid)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        var intent = getIntent()
         userId = intent.getStringExtra("userId")!!
        var userName = intent.getStringExtra("userName")
        currentUserImage=intent.getStringExtra("image")
        tvUserName.text = userName
        currentChatno=intent.getStringExtra("phone").toString()
        phonecurrentChat= currentChatno
        imgBack.setOnClickListener {
            onBackPressed()
        }
        if(!currentUserImage.isNullOrEmpty())
        {
            Picasso.with(this@ChatActivity).load(currentUserImage).into(imgProfile)
        }

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId!!)


        var emoji=EmojiPopup.Builder.fromRootView(findViewById(R.id.root_view)).build(etMessage)
        emoji_button.setOnClickListener {
            emoji.toggle()
        }

        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(snapshot: DataSnapshot) {

                val user = snapshot.getValue(User::class.java)

                if (user != null) {
                    if (user.profileImage == "") {
                        imgProfile.setImageResource(R.drawable.profile_image)
                    } else {
                        Picasso.with(this@ChatActivity).load(user.profileImage).into(imgProfile)
                    }
                }
            }
        })

        btnSendMessage.setOnClickListener {
            var message: String = etMessage.text.toString()

            if (message.isEmpty()) {
                etMessage.setText("")
            } else {
                sendMessage(firebaseUser!!.uid, userId, message)
                etMessage.setText("")
                topic = "/topics/$userId"
                PushNotification(NotificationData( CurrentUser.phone!!,message),
                topic).also {
                    sendNotification(it)
                }

            }
        }

        readMessage(firebaseUser!!.uid, userId)
    }

    private fun sendMessage(senderId: String, receiverId: String, message: String) {
        var reference: DatabaseReference? = FirebaseDatabase.getInstance().getReference()


        var chat= Chat()
        chat.message=message
        chat.receiverId=receiverId
        chat.senderId=senderId
            reference!!.child("UserChat").child(senderId).child(receiverId).child("Chat").push().setValue(chat)
        reference!!.child("UserChat").child(receiverId).child(senderId).child("Chat").push().setValue(chat)

    }


    fun readMessage(senderId: String, receiverId: String) {
        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("UserChat").child(senderId).child(receiverId).child("Chat")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (dataSnapShot in snapshot.children) {
                    val chat = dataSnapShot.getValue(Chat::class.java)

                    if (chat!!.senderId.equals(senderId) && chat!!.receiverId.equals(receiverId) ||
                        chat!!.senderId.equals(receiverId) && chat!!.receiverId.equals(senderId)
                    ) {
                        chatList.add(chat)
                    }
                }

                val chatAdapter = ChatAdapter(this@ChatActivity, chatList)

                chatRecyclerView.adapter = chatAdapter
                chatRecyclerView.scrollToPosition(chatList.size-1)
            }
        })
        gallery.setOnClickListener {
            chooseImage()
        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d("TAG", "Response: ${Gson().toJson(response)}")
            } else {
                Log.e("TAG", response.errorBody()!!.string())
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
        }
    }

    override fun onDestroy() {
        currentChatno=""
        super.onDestroy()

    }

    override fun onPause() {
        currentChatno=""
        super.onPause()
    }

    override fun onResume() {
        currentChatno=phonecurrentChat
        super.onResume()
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
                uploadImage(firebaseUser!!.uid, userId)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage(senderId: String, receiverId: String) {
        if (filePath != null) {

            var ref: StorageReference = storageRef.child("chatimage/" + FirebaseAuth.getInstance().currentUser?.uid.toString()+ System.currentTimeMillis())
            ref.putFile(filePath!!)
                .addOnSuccessListener {

                    val downloadUri: Task<Uri> = it.storage.downloadUrl
                    downloadUri.addOnCompleteListener{
                        var generatedFilePath = downloadUri.getResult().toString();


                        var reference: DatabaseReference? = FirebaseDatabase.getInstance().getReference()

                        var hashMap: HashMap<String, String> = HashMap()
                        hashMap.put("senderId", senderId)
                        hashMap.put("receiverId", receiverId)
                        hashMap.put("message", generatedFilePath)
                        hashMap.put("messageType","img")
                        reference!!.child("UserChat").child(senderId).child(receiverId).child("Chat").push().setValue(hashMap)
                        reference!!.child("UserChat").child(receiverId).child(senderId).child("Chat").push().setValue(hashMap)


                    }

                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                        .show()

                }

        }
    }

}