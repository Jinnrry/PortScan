package cn.xjiangwei.portscan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    private String old_ip;
    private String old_port;

    private EditText ipEditText;
    private EditText portEditText;

    private TextView tv;

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.sp = getSharedPreferences("spConfig", Context.MODE_PRIVATE);
        this.old_ip = this.sp.getString("ip", "172.201.*.*");
        this.old_port = this.sp.getString("port", "7866");

        this.ipEditText = (EditText) findViewById(R.id.ip);
        this.portEditText = (EditText) findViewById(R.id.port);

        this.tv = (TextView) findViewById(R.id.show);

        this.ipEditText.setText(this.old_ip);
        this.portEditText.setText(this.old_port);
    }


    /**
     * 测试telnet 机器端口的连通性
     *
     * @param hostname
     * @param port
     * @param timeout
     * @return
     */
    public static boolean telnet(String hostname, int port, int timeout) {
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            socket.connect(new InetSocketAddress(hostname, port), timeout); // 建立连接
            isConnected = socket.isConnected(); // 通过现有方法查看连通状态
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();   // 关闭连接
            } catch (IOException ignored) {
            }
        }
        return isConnected;
    }

    public void start(View view) {


        String ip = this.ipEditText.getText().toString();
        String port = this.portEditText.getText().toString();

        SharedPreferences.Editor editor = this.sp.edit();
        editor.putString("ip", ip);
        editor.putString("port", port);
        editor.apply();


        ConcurrentLinkedQueue<String> ipTodo = new ConcurrentLinkedQueue<String>();
        Vector<String> findIp = new Vector<String>();


        String[] t = ip.split("\\.");
        if ("*".equals(t[2])) {
            for (int i = 0; i < 255; i++) {
                String addIp = "" + t[0] + "." + t[1] + "." + i + ".";
                if ("*".equals(t[3])) {
                    for (int j = 0; j < 255; j++) {
                        ipTodo.add(addIp + j);
                    }
                }
            }
        } else {
            String addIp = "" + t[0] + "." + t[1] + "." + t[2] + ".";
            if ("*".equals(t[3])) {
                for (int j = 0; j < 255; j++) {
                    ipTodo.add(addIp + j);
                }
            } else {
                ipTodo.add(addIp + t[3]);
            }
        }


        if (ipTodo.size() > 10) {
            for (int l = 0; l < 10; l++) {
                new Thread() {
                    @Override
                    public void run() {
                        while (!ipTodo.isEmpty()) {
                            String todo = ipTodo.poll();
                            if (todo == null) break;

                            System.out.println(todo);
                            if (telnet(todo, Integer.parseInt(port), 200)) {
                                findIp.add(todo);
                                runOnUiThread(() -> {
                                    String tvshow = tv.getText().toString();
                                    tvshow += ("\n" + todo);
                                    tv.setText(tvshow);
                                });
                            }
                        }
                    }
                }.start();

            }
        }
        new Thread() {
            @Override
            public void run() {
                while (!ipTodo.isEmpty()) {
                    String todo = ipTodo.poll();
                    System.out.println(todo);
                    if (todo == null) break;
                    if (telnet(todo, Integer.parseInt(port), 200)) {
                        findIp.add(todo);
                        runOnUiThread(() -> {
                            String tvshow = tv.getText().toString();
                            tvshow += ("\n" + todo);
                            tv.setText(tvshow);
                        });
                    }
                }

                System.out.println(findIp);


            }
        }.start();

    }
}