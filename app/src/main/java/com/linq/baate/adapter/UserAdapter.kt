package com.linq.baate.adapter

import android.content.Context
import android.content.Intent
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.linq.baate.R
import com.linq.baate.activity.ChatActivity
import com.linq.baate.model.User
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_users.*

class UserAdapter(private val context: Context, private val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.txtUserName.text = user.userName

        if(user.profileImage.isNotEmpty()) {
            Picasso.with(context).load(user.profileImage).placeholder(R.drawable.profile_image)
                .into(holder.imgUser)
        }
        holder.layoutUser.setOnClickListener {
            ChatAdapter.lastimage=""
            val intent = Intent(context,ChatActivity::class.java)
            intent.putExtra("userId",user.userId)
            intent.putExtra("userName",user.userName)
            intent.putExtra("image",user.profileImage)

            intent.putExtra("phone",user.phone)
            context.startActivity(intent)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtUserName:TextView = view.findViewById(R.id.userName)
        val txtTemp:TextView = view.findViewById(R.id.temp)
        val imgUser:CircleImageView = view.findViewById(R.id.userImage)
        val layoutUser:LinearLayout = view.findViewById(R.id.layoutUser)
    }
}