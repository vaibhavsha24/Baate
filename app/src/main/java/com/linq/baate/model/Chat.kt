package com.linq.baate.model

data class Chat(var senderId:String = "", var receiverId:String = "", var message:String = "")
{
    var messageType:String?="text"

}