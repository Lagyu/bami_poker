import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MyPlayer {
    static Socket s;
    static InputStreamReader in;
    static BufferedReader br;
    static PrintWriter out;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("セブンティーンポーカーの世界へようこそ！");

        while (true) {
            System.out.println("0: ゲームを開始する    1: これまでのスコアランキングを確認する");

            if (sc.nextLine().equals("0")) {
                System.out.println("それではゲームを開始します。");
                System.out.println();
                System.out.println("あなたのお名前を入力してください。");
                String playerName = sc.nextLine();
                System.out.println("対戦相手とのマッチングを開始します。");
                System.out.println();

                s = null;

                try {
                    s = new Socket("localhost", 8080);
                    in = new InputStreamReader(s.getInputStream());
                    br = new BufferedReader(in);
                    out = new PrintWriter(s.getOutputStream(), true);

                    out.println(playerName);

                    ReceivingThread rt = new ReceivingThread(s, in, br, out); // サーバーからのメッセージを受信するためのスレッドを作成
                    rt.start(); // 受信用スレッドを動かす

                    SendingThread st = new SendingThread(s, in, br, out); // サーバーにメッセージを送信するためのスレッドを作成
                    st.start(); // 送信用スレッドを動かす
                } catch (UnknownHostException e) {
                    System.err.println("ホストのIPアドレスが判定できません: " + e);
                } catch (IOException e) {
                    System.err.println("エラーが発生しました: " + e);
                }

                break;
            } else if (sc.nextLine().equals("1")) {
                System.out.println("これまでスコアランキングを表示します。");
                System.out.println();

                try {
                    File file = new File("C:\\Users\\Kanae\\IdeaProjects\\情報理工学実験B_ソフトウェア制作2\\ranking.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));

                    String str;
                    String[][] str_split = new String[10][];
                    int i = 0;

                    while ((str = br.readLine()) != null) {
                        str_split[i] = str.split(",");
                        i++;
                    }

                    for (i = 0; i < 10; i++) {
                        if (!(str_split[i][1].equals(""))) {
                            System.out.println("第" + str_split[i][0] + "位: " +
                                    str_split[i][1] + "    " + str_split[i][2] + "枚");
                        }
                    }

                    br.close();
                } catch (IOException e) {
                    System.err.println("エラーが発生しました: " + e);
                }
                break;
            } else {
                System.out.println("無効な文字列が入力されました。もう一度正しく入力してください。");
            }
        }
    }
}

class ReceivingThread extends Thread { // 受信用スレッド
    static Socket rtS;
    static InputStreamReader rtIn;
    static BufferedReader rtBr;
    static PrintWriter rtOut;

    public ReceivingThread(Socket s, InputStreamReader in, BufferedReader br, PrintWriter out) {
        rtS = s;
        rtIn = in;
        rtBr = br;
        rtOut = out;
    }

    public void run() {
        try {
            while (true) {
                String str = rtBr.readLine();
                if (str != null) {
                    System.out.println(str);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("エラーが発生しました: " + e);
        }
    }
}

class SendingThread extends Thread { // 送信用スレッド
    static Socket stS;
    static InputStreamReader stIn;
    static BufferedReader stBr;
    static PrintWriter stOut;
    Scanner sc = new Scanner(System.in);
    String str;

    public SendingThread(Socket s, InputStreamReader in, BufferedReader br, PrintWriter out) {
        stS = s;
        stIn = in;
        stBr = br;
        stOut = out;
    }

    public void run() {
        while (true) {
            str = sc.nextLine();

            if (str != null) {
                stOut.println(str);
                stOut.flush();
            } else {
                break;
            }
        }
    }
}