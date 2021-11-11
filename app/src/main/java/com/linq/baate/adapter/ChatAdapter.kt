package com.linq.baate.adapter

import android.content.Context
import android.content.Intent
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.linq.baate.R
import com.linq.baate.activity.ChatActivity
import com.linq.baate.model.Chat
import com.linq.baate.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.linq.baate.model.CurrentUser
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(private val context: Context, private val chatList: ArrayList<Chat>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val MESSAGE_TYPE_LEFT = 0
    private val MESSAGE_TYPE_RIGHT = 1
    var firebaseUser: FirebaseUser? = null
    companion object{
        var lastimage=""
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == MESSAGE_TYPE_RIGHT) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_right, parent, false)
            return ViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_left, parent, false)
            return ViewHolder(view)
        }

    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        if(chat.messageType==null || chat.messageType=="text")
              holder.txtUserName.text = chat.message
        else
        {
            holder.txtUserName.visibility=View.GONE
            holder.chatimg.visibility=View.VISIBLE
            Picasso.with(context).load(chat.message).into(holder.chatimg)

        }
        if (chatList[position].senderId == firebaseUser!!.uid  ) {
//            if(lastimage=="" || lastimage!= chatList[position].senderId )
//            {
                if(CurrentUser.profileImage.isNotEmpty())
                    Picasso.with(context).load(CurrentUser.profileImage).placeholder(R.drawable.profile_image).into(holder.imgUser)
                else
                {
                    holder.imgUser.setImageResource(R.drawable.profile_image)
                }
//            }
//            lastimage=chatList[position].senderId

        } else if( ChatActivity.currentUserImage!!.isNotEmpty()){
//            if(lastimage=="" || lastimage!= chatList[position].receiverId )
//            {
                Picasso.with(context).load(ChatActivity.currentUserImage).placeholder(R.drawable.profile_image).into(holder.imgUser)
                lastimage=chatList[position].receiverId
//            }
        }
        else{
            holder.imgUser.visibility=View.GONE
        }

        //Glide.with(context).load(user.profileImage).placeholder(R.drawable.profile_image).into(holder.imgUser)

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtUserName: TextView = view.findViewById(R.id.tvMessage)
        val imgUser: CircleImageView = view.findViewById(R.id.userImage)
        val chatimg:ImageView=view.findViewById(R.id.chatimg)
    }

    override fun getItemViewType(position: Int): Int {
        firebaseUser = FirebaseAuth.getInstance().currentUser
        if (chatList[position].senderId == firebaseUser!!.uid) {
            return MESSAGE_TYPE_RIGHT
        } else {
            return MESSAGE_TYPE_LEFT
        }

    }
}