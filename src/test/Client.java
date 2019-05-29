package test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Client {
    public static Socket socket = null;
    public static List<Socket>sockets = new ArrayList<>();
    public static JSONObject jSonObject = new JSONObject();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(2551);
            new heartbeat().start();
            System.out.println("创建连接");
            while(true){
                socket = serverSocket.accept();
                sockets.add(socket);
                new Connect(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class Connect extends Thread {
    public Socket socket;
    public Connect(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        byte[] bytes = new byte[1024];
        int i = 0;
        int Type = 0;

        People_Number(1,"People",Client.sockets.size());
        try {

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = "";

            //while ((i = socket.getInputStream().read(bytes, 0, bytes.length)) > 0) {
            while ((line = bufferedReader.readLine()) != null) {
                String content = "";
                content = line;
                JSONObject jsonObject = JSON.parseObject(content);
                System.out.println(jsonObject);
                Type = (int)jsonObject.get("Type");
                if(Type == 3){
                    Login(jsonObject.getString("account_number"),jsonObject.getString("Password"));
                }else if (Type == 2){
                    System.out.println(jsonObject.getString("Messages"));
                    SendAllMassages(jsonObject.toJSONString());
                }else if(Type == 5){
                    register(jsonObject.getString("ReName"),jsonObject.getString("RePassword"));
                }
            }
        } catch (SocketException e) {
            try {
                Client.sockets.remove(socket);
                System.out.println(Client.sockets.size());
                People_Number(1,"People",Client.sockets.size());
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void  People_Number(Integer Type,String parameterType, Object parameter){
        Client.jSonObject.put("Type",Type);
        Client.jSonObject.put(parameterType,parameter);

        String content = Client.jSonObject.toString() + "\r\n";
        byte[] bytes1 = content.getBytes();
        for (Socket socket1 : Client.sockets){
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket1.getOutputStream());
                dataOutputStream.write(bytes1);
                dataOutputStream.flush();
                System.out.println(Client.jSonObject);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void SendMassages(String Conent) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedWriter.write(Conent + "\r\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void SendAllMassages(String Conent) {
        for (Socket socket1 : Client.sockets) {
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket1.getOutputStream()));
                bufferedWriter.write(Conent + "\r\n");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static String URL = "jdbc:mysql://localhost:3306/test";
    public static String URLName = "root";
    public static String URLPassword = "root";

    public void Login(String Name,String Password){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(URL,URLName,URLPassword);
            Statement statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("select * from test where name = '" + Name + "'");

            boolean state = false;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Type",4);

            while(rs.next()){
                if(rs.getString("name").equals(Name)){
                    if (rs.getString("password").equals(Password)){
                        jsonObject.put("state",true);
                        state = true;
                        break;
                    }
                }
            }

            if(!state){
                jsonObject.put("state",false);
            }

            SendMassages(jsonObject.toJSONString());
            Thread.sleep(100);
            People_Number(1,"People",Client.sockets.size());
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    public void register(String ReName,String RePassword){
        try {
            //BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            JSONObject jSonObject = new JSONObject();
            boolean Register = true;
            Connection connection = DriverManager.getConnection(URL,URLName,URLPassword);
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select name from test");
            while(rs.next()){
                System.out.println(rs.getString("name"));
                if(rs.getString("name").equals(ReName)){
                    Register = false;
                }
            }

            if(Register){
                String sqlIntset = "insert into test(name,password) values(?, ?)";
                PreparedStatement ps = connection.prepareStatement(sqlIntset);
                ps.setString(1,ReName);
                ps.setString(2,RePassword);
                ps.executeLargeUpdate();
                ps.close();

                jSonObject.put("Type",5);
                jSonObject.put("state",true);
            }else{
                jSonObject.put("Type",5);
                jSonObject.put("state",false);
            }
            SendMassages(jSonObject.toString());
        } catch (SQLException e) {

        }
    }


}


class heartbeat extends Thread{


    public void run(){
        while(true){
            try {
                for (int i = 0; i < Client.sockets.size(); i++) {
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Client.sockets.get(i).getOutputStream()));
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("Type", 007);
                        bufferedWriter.write(jsonObject.toString() + "\r\n");
                        bufferedWriter.flush();
                    } catch(SocketException e) {
                        try {
                            Client.sockets.get(i).close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        Client.sockets.remove(Client.sockets.get(i));
                        System.out.println(Client.sockets.size());
                        System.out.println("有用户断开");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
