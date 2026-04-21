package com.gamilha.services;

import com.gamilha.entity.ChatMessage;
import com.gamilha.entity.User;
import com.gamilha.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    Connection con;

    public ChatService(){

        con = DatabaseConnection.getConnection();

    }



    public void sendMessage(ChatMessage msg){

        String sql =
                "INSERT INTO chat_message (content,sender_id,recipient_id,created_at) " +
                        "VALUES (?,?,?,?)";

        try {

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1,msg.getContent());

            ps.setInt(2,msg.getSender().getId());

            ps.setInt(3,msg.getRecipient().getId());

            ps.setTimestamp(
                    4,
                    Timestamp.valueOf(
                            msg.getCreatedAt()==null ?
                                    LocalDateTime.now()
                                    :
                                    msg.getCreatedAt()
                    )
            );

            ps.executeUpdate();

        }
        catch (Exception e){

            System.out.println(e.getMessage());
        }

    }



    public List<ChatMessage> getConversation(User u1, User u2){

        List<ChatMessage> messages = new ArrayList<>();

        String sql =

                "SELECT * FROM chat_message " +

                        "WHERE " +

                        "(sender_id=? AND recipient_id=?) " +

                        "OR " +

                        "(sender_id=? AND recipient_id=?) " +

                        "ORDER BY created_at";

        try {

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1,u1.getId());
            ps.setInt(2,u2.getId());

            ps.setInt(3,u2.getId());
            ps.setInt(4,u1.getId());

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                ChatMessage m = new ChatMessage();

                m.setId(rs.getInt("id"));

                m.setContent(rs.getString("content"));

                m.setCreatedAt(
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                // sender
                User sender = new User();
                sender.setId(rs.getInt("sender_id"));

                // recipient
                User recipient = new User();
                recipient.setId(rs.getInt("recipient_id"));

                m.setSender(sender);

                m.setRecipient(recipient);

                messages.add(m);

            }

        }
        catch (Exception e){

            System.out.println(e.getMessage());
        }

        return messages;

    }



    public List<ChatMessage> getSentMessages(User user){

        List<ChatMessage> messages = new ArrayList<>();

        String sql =
                "SELECT * FROM chat_message WHERE sender_id=?";

        try {

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1,user.getId());

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                ChatMessage m = new ChatMessage();

                m.setId(rs.getInt("id"));

                m.setContent(rs.getString("content"));

                messages.add(m);

            }

        }
        catch (Exception e){

            System.out.println(e.getMessage());
        }

        return messages;

    }


    public void deleteMessage(int id){

        String sql="DELETE FROM chat_message WHERE id=?";

        try{

            PreparedStatement ps=con.prepareStatement(sql);

            ps.setInt(1,id);

            ps.executeUpdate();

        }
        catch(Exception e){

            System.out.println(e.getMessage());
        }

    }



    public void updateMessage(ChatMessage msg){

        String sql="UPDATE chat_message SET content=? WHERE id=?";

        try{

            PreparedStatement ps=con.prepareStatement(sql);

            ps.setString(1,msg.getContent());

            ps.setInt(2,msg.getId());

            ps.executeUpdate();

        }
        catch(Exception e){

            System.out.println(e.getMessage());
        }

    }
    public List<ChatMessage> getReceivedMessages(User user){

        List<ChatMessage> messages = new ArrayList<>();

        String sql =
                "SELECT * FROM chat_message WHERE recipient_id=?";

        try {

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1,user.getId());

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                ChatMessage m = new ChatMessage();

                m.setId(rs.getInt("id"));

                m.setContent(rs.getString("content"));

                messages.add(m);

            }

        }
        catch (Exception e){

            System.out.println(e.getMessage());
        }

        return messages;

    }

}