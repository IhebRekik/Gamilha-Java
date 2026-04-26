package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.PasswordHasher;
import com.gamilha.utils.SessionContext;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection conn =
            DBConnection.getInstance();



    // ───────────── CRUD ─────────────

    public void add(User user){

        String sql =
                "INSERT INTO user " +
                        "(email,password,name,profile_image,roles,reports,is_active,created_at) " +
                        "VALUES (?,?,?,?,?,?,?,?)";

        try(

                PreparedStatement ps =
                        conn.prepareStatement(

                                sql,

                                Statement.RETURN_GENERATED_KEYS

                        )

        ){

            ps.setString(

                    1,

                    user.getEmail()

            );


            ps.setString(

                    2,

                    PasswordHasher.hash(

                            user.getPassword()

                    )

            );


            ps.setString(

                    3,

                    user.getName()

            );


            ps.setString(

                    4,

                    user.getProfileImage()

            );


            ps.setString(

                    5,

                    user.getRoles() != null

                            ? user.getRoles()

                            : "[\"ROLE_USER\"]"

            );


            ps.setInt(

                    6,

                    user.getReports()

            );


            ps.setBoolean(

                    7,

                    user.isActive()

            );


            ps.setTimestamp(

                    8,

                    user.getCreatedAt() != null

                            ? user.getCreatedAt()

                            : new Timestamp(

                            System.currentTimeMillis()

                    )

            );


            ps.executeUpdate();


            ResultSet rs =
                    ps.getGeneratedKeys();


            if(rs.next())

                user.setId(

                        rs.getInt(1)

                );

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }



    public void update(User user){

        boolean changePassword =

                user.getPassword()!=null &&

                        !user.getPassword().isEmpty();


        String sql =
                "UPDATE user SET " +
                        "email=?, name=?, profile_image=?, roles=?, reports=?, is_active=?" +

                        (changePassword ? ", password=?" : "") +

                        " WHERE id=?";


        try(

                PreparedStatement ps =
                        conn.prepareStatement(sql)

        ){

            ps.setString(

                    1,

                    user.getEmail()

            );


            ps.setString(

                    2,

                    user.getName()

            );


            ps.setString(

                    3,

                    user.getProfileImage()

            );


            ps.setString(

                    4,

                    user.getRoles()

            );


            ps.setInt(

                    5,

                    user.getReports()

            );


            ps.setBoolean(

                    6,

                    user.isActive()

            );


            if(changePassword){

                String pwd =
                        user.getPassword();


                if(!isBcryptHash(pwd))

                    pwd =
                            PasswordHasher.hash(pwd);


                ps.setString(

                        7,

                        pwd

                );


                ps.setInt(

                        8,

                        user.getId()

                );

            }

            else{

                ps.setInt(

                        7,

                        user.getId()

                );

            }


            ps.executeUpdate();

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }



    public void delete(int id){

        try(

                PreparedStatement ps =
                        conn.prepareStatement(

                                "DELETE FROM user WHERE id=?"

                        )

        ){

            ps.setInt(

                    1,

                    id

            );


            ps.executeUpdate();

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }



    public User findByEmail(String email){

        String sql =
                "SELECT * FROM user WHERE email=?";


        try(

                PreparedStatement ps =
                        conn.prepareStatement(sql)

        ){

            ps.setString(

                    1,

                    email

            );


            ResultSet rs =
                    ps.executeQuery();


            if(rs.next())

                return mapFull(rs);

        }

        catch(Exception e){

            e.printStackTrace();

        }

        return null;

    }



    public List<User> findAll(){

        List<User> list =
                new ArrayList<>();


        try(

                PreparedStatement ps =
                        conn.prepareStatement(

                                "SELECT * FROM user"

                        );


                ResultSet rs =
                        ps.executeQuery()

        ){

            while(rs.next())

                list.add(

                        mapFull(rs)

                );

        }

        catch(Exception e){

            e.printStackTrace();

        }


        return list;

    }



    // ───────────── LOGIN ─────────────

    public User login(

            String email,

            String plainPassword

    ){

        String sql =
                "SELECT * FROM user WHERE email=? LIMIT 1";


        try(

                PreparedStatement ps =
                        conn.prepareStatement(sql)

        ){

            ps.setString(

                    1,

                    email

            );


            ResultSet rs =
                    ps.executeQuery();


            if(!rs.next())

                return null;


            String storedPassword =
                    rs.getString("password");


            if(

                    !isPasswordValid(

                            plainPassword,

                            storedPassword

                    )

            )

                return null;


            return mapFull(rs);

        }

        catch(Exception e){

            throw new RuntimeException(e);

        }

    }



    // ───────────── AMIS ─────────────

    public List<User> getAmis(){

        List<User> list =
                new ArrayList<>();


        String sql =
                "SELECT * FROM friend WHERE user_id=?";


        try(

                PreparedStatement ps =
                        conn.prepareStatement(sql)

        ){

            User current =
                    SessionContext.getCurrentUser();


            ps.setInt(

                    1,

                    current.getId()

            );


            ResultSet rs =
                    ps.executeQuery();


            List<User> all =
                    findAll();


            while(rs.next()){

                int friendId =
                        rs.getInt("friend_id");


                all.stream()

                        .filter(u->u.getId()==friendId)

                        .findFirst()

                        .ifPresent(list::add);

            }

        }

        catch(Exception e){

            throw new RuntimeException(e);

        }


        return list;

    }



    // ───────────── MAPPING ─────────────

    private User mapFull(ResultSet rs) throws SQLException{

        User u =
                new User();


        u.setId(

                rs.getInt("id")

        );


        u.setEmail(

                rs.getString("email")

        );


        u.setName(

                rs.getString("name")

        );


        u.setPassword(

                rs.getString("password")

        );


        u.setProfileImage(

                rs.getString("profile_image")

        );


        u.setRoles(

                rs.getString("roles")

        );


        u.setReports(

                rs.getInt("reports")

        );


        u.setActive(

                rs.getBoolean("is_active")

        );


        u.setCreatedAt(

                rs.getTimestamp("created_at")

        );


        return u;

    }



    // ───────────── PASSWORD ─────────────

    private boolean isPasswordValid(

            String plain,

            String stored

    ){

        if(stored==null)

            return false;


        String normalized =
                stored.startsWith("$2y$")

                        ? "$2a$"+stored.substring(4)

                        : stored;


        if(isBcryptHash(normalized))

            return BCrypt.checkpw(

                    plain,

                    normalized

            );


        return plain.equals(stored);

    }



    private boolean isBcryptHash(String s){

        return s!=null &&

                (

                        s.startsWith("$2a$") ||

                                s.startsWith("$2b$") ||

                                s.startsWith("$2y$")

                );

    }

}