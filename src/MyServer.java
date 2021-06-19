import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class MyServer {
    static int maxPlayer = 2; // 対戦は2人で行われる
    static int myNum = 0;
    static ServerSocket server;
    static Socket[] mySocket = new Socket[maxPlayer];
    static InputStreamReader[] myIsr = new InputStreamReader[maxPlayer];
    static BufferedReader[] myIn = new BufferedReader[maxPlayer];
    static PrintWriter[] myOut = new PrintWriter[maxPlayer];
    static String[] myName = new String[maxPlayer]; //プレイヤーの名前
    static Game game;

    public static void main(String[] args) {
        server = null;

        for (int i = 0; i < maxPlayer; i++) {
            mySocket[i] = null;
            myIsr[i] = null;
            myIn[i] = null;
            myOut[i] = null;
        }

        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress("localhost", 8080));

            System.out.println("サーバが立ち上がりました。");

            while(true) {
                mySocket[myNum] = server.accept();
                myIsr[myNum] = new InputStreamReader(mySocket[myNum].getInputStream());
                myIn[myNum] = new BufferedReader(myIsr[myNum]);
                myOut[myNum] = new PrintWriter(mySocket[myNum].getOutputStream(), true);
                myName[myNum] = myIn[myNum].readLine();

                if (myNum == 1) {
                    MyServer.SendBoth("マッチングが完了しました。");
                    MyServer.SendBoth(myName[0] + "様 と " + myName[1] + "様 のゲームを開始します。");
                    MyServer.SendBoth("");
                    game = new Game(myName[0], myName[1]);
                    game.start();
                    break;
                }

                myNum++;
            }

            System.out.println("ゲームが終了しました。サーバーをシャットダウンします。");
        } catch (Exception e) {
            System.err.println("ソケット作成時にエラーが発生しました");
            e.printStackTrace();
        } finally {
            try {
                for (int i = 0; i < maxPlayer; i++) {
                    if (myIn[i] != null) {
                        myIn[i].close();
                    }

                    if (myOut[i] != null) {
                        myOut[i].close();
                    }

                    if (mySocket[i] != null) {
                        mySocket[i].close();
                    }
                }

                if (server != null) {
                    server.close();
                }

                System.out.println("サーバを終了します。");
            } catch (IOException e) {
                System.err.println("エラーが発生しました: " + e);
            }
        }
    }

    public static void SendBoth(String str) { // 両者に同一のメッセージを送信する
        for (int i = 0; i < 2; i++) {
            myOut[i].println(str);
            myOut[i].flush();
        }
    }
}

class Game {
    static int setCount = 10; // 1ゲームで10セット行う
    int setNumber;
    static int counter = 0;
    static String player1;
    static String player2;
    int better; // 先にベットした人
    String pl1;
    String pl2;
    int chip1; // プレイヤー1の所持チップ
    int chip2; // プレイヤー2の所持チップ
    int betting1; // プレイヤー1の現在の賭けチップ数
    int betting2; // プレイヤー2の現在の賭けチップ数
    int bettingChip = 0; // 賭けチップの合計
    String[] hand1 = new String[5];
    String[] hand2 = new String[5];
    String role1;
    String role2;
    Cards cards;


    Game(String name1, String name2) { // 1セット目のみ
        setNumber = counter;
        player1 = name1;
        player2 = name2;
        chip1 = 150; // 所持チップの初期値は150
        chip2 = 150; // 同上
        counter++;
    }

    Game(int c1, int c2) { // 2セット目以降
        setNumber = counter;
        chip1 = c1; // チップ数は前セットから引継ぎ
        chip2 = c2; // 同上
        counter++;
    }

    public void start() {
        cards = new Cards();

        try {
            MyServer.SendBoth("～第" + counter + "セット目～");
            MyServer.SendBoth("ディーラー: それではアンティとして両プレイヤー様よりチップ5枚を場に出していただきます。");
            MyServer.SendBoth(player1 + "様: " + chip1 + " -> " + (chip1 - 5) +
                    "    " + player2 + "様: " + chip2 + " -> " + (chip2 - 5));

            chip1 -= 5;
            chip2 -= 5;

            MyServer.SendBoth("ディーラー: これよりカードのシャッフルを行います。");
            MyServer.SendBoth("ディーラー: リフルシャッフルのちヒンズーシャッフルを行い、" +
                    "その後両プレイヤー様に『上から何番目か』を指定していただき、その位置でカットします。");
            MyServer.SendBoth("ディーラー: リフルシャッフルを行う回数は指定可能です。" +
                    "両プレイヤー様の希望回数が異なった場合はランダムで選ばれます。リフルシャッフル回数のご希望はございますか？");
            MyServer.SendBoth("");
            MyServer.SendBoth("リフルシャッフル希望回数を入力してください。(希望しない場合は0)");

            pl1 = MyServer.myIn[0].readLine();
            pl2 = MyServer.myIn[1].readLine();

            if ((pl1 != null) && (pl2 != null)) {
                MyServer.SendBoth("ディーラー: " + player1 + "様が" + pl1 + "回、" +
                        player2 + "様が" + pl2 + "回をご希望でしたので、");

                if (!(pl1.equals("0")) && !(pl2.equals("0"))) {
                    Random r = new Random();

                    if (r.nextInt(2) == 0) {
                        MyServer.SendBoth("ディーラー: 今回は" + pl1 + "回リフルシャッフルを行います。");
                        MyServer.SendBoth("");

                        cards.riffleShuffle(Integer.parseInt(pl1));
                    } else {
                        MyServer.SendBoth("ディーラー: 今回は" + pl2 + "回リフルシャッフルを行います。");
                        MyServer.SendBoth("");

                        cards.riffleShuffle(Integer.parseInt(pl2));
                    }
                } else if (pl1.equals("0")) {
                    MyServer.SendBoth("ディーラー: 今回は" + pl2 + "回リフルシャッフルを行います。");
                    MyServer.SendBoth("");

                    cards.riffleShuffle(Integer.parseInt(pl2));
                } else {
                    MyServer.SendBoth("ディーラー: 今回は" + pl1 + "回リフルシャッフルを行います。");
                    MyServer.SendBoth("");

                    cards.riffleShuffle(Integer.parseInt(pl1));
                }
            }

            MyServer.SendBoth("ディーラー: 続いて、ヒンズーシャッフルを行います。");
            MyServer.SendBoth("");

            cards.hinduShuffle();

            MyServer.SendBoth("ディーラー: 最後に、カットを行います。" +
                    "両プレイヤー様は『上から何番目』の位置でカットを行うか、ご指定ください。");
            MyServer.SendBoth("");
            MyServer.SendBoth("上から何番目でカットしますか？(0～16で指定)");

            pl1 = MyServer.myIn[0].readLine();
            pl2 = MyServer.myIn[1].readLine();

            if ((pl1 != null) && (pl2 != null)) {
                MyServer.SendBoth("ディーラー: " + player1 + "様が上から" + pl1 + "番目、" +
                        player2 + "様が上から" + pl2 + "番目をご希望でしたので、そのようにカットさせていただきます。");
                MyServer.SendBoth("");

                cards.cut(Integer.parseInt(pl1));
                cards.cut(Integer.parseInt(pl2));
            }

            MyServer.SendBoth("ディーラー: カードのシャッフルが完了いたしました。");
            MyServer.SendBoth("ディーラー: これより両プレイヤー様に手札をお配りします。");
            MyServer.SendBoth("");

            cards.deal(hand1, hand2);

            MyServer.myOut[0].println("あなたの手札: " + Arrays.toString(hand1));
            MyServer.myOut[1].println("あなたの手札: " + Arrays.toString(hand2));
            MyServer.SendBoth("");

            MyServer.SendBoth("ディーラー: それでは1stベットを開始します。");

            if ((counter % 2) == 1) {
                MyServer.SendBoth("ディーラー: 奇数セットは" + player1 + "様から宣言していただきます。");
                MyServer.SendBoth("");

                MyServer.myOut[0].println("0: ベット    1: チェック");
                pl1 = MyServer.myIn[0].readLine();

                if (pl1.equals("0")) {
                    MyServer.myOut[0].println("ベットするチップ数を宣言してください。(5～15)");

                    pl1 = MyServer.myIn[0].readLine();

                    MyServer.SendBoth("ディーラー: " + player1 + "様が" + pl1 + "枚のチップをベットしました。");

                    betting1 = Integer.parseInt(pl1);
                    chip1 -= betting1;
                    bettingChip += betting1;
                    better = 0;

                    MyServer.myOut[1].println("0: コール    1: レイズ    2: フォルド");

                    pl2 = MyServer.myIn[1].readLine();

                    if (pl2.equals("0")) {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                        chip2 -= betting1;
                        bettingChip += betting1;
                    } else if (pl2.equals("1")) {
                        MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting1 + "～15");

                        pl2 = MyServer.myIn[1].readLine();

                        MyServer.SendBoth("ディーラー: " + player2 + "様がレイズを宣言し、" +
                                pl2 + "枚のチップをベットしました。");

                        betting2 = Integer.parseInt(pl2);
                        chip2 -= betting2;
                        bettingChip += betting2;
                        better = 1;

                        MyServer.myOut[0].println("0: コール    1: フォルド");

                        pl1 = MyServer.myIn[0].readLine();

                        if (pl1.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                            chip1 -= (betting2 - betting1);
                            bettingChip += (betting2 - betting1);
                            betting1 = betting2;
                        } else {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                            MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                        MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                } else {
                    MyServer.SendBoth("ディーラー: " + player1 + "様がチェックを宣言しました。");

                    MyServer.myOut[1].println("0: ベット    1: チェック");
                    pl2 = MyServer.myIn[1].readLine();

                    if (pl2.equals("0")) {
                        MyServer.myOut[1].println("ベットするチップ数を宣言してください。(5～15)");
                        pl2 = MyServer.myIn[1].readLine();

                        MyServer.SendBoth("ディーラー: " + player2 + "様が" + pl2 + "枚のチップをベットしました。");

                        betting2 = Integer.parseInt(pl2);
                        chip2 -= betting2;
                        bettingChip += betting2;
                        better = 1;

                        MyServer.myOut[0].println("0: コール    1: レイズ    2: フォルド");

                        pl1 = MyServer.myIn[0].readLine();
                        if (pl1.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                            chip1 -= betting2;
                            bettingChip += betting2;
                        } else if (pl1.equals("1")) {
                            MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting2 + "～15");

                            pl1 = MyServer.myIn[0].readLine();

                            MyServer.SendBoth("ディーラー: " + player1 + "様がレイズを宣言し、" +
                                    pl1 + "枚のチップをベットしました。");

                            betting1 = Integer.parseInt(pl1);
                            chip1 -= betting1;
                            bettingChip += betting1;
                            better = 0;

                            MyServer.myOut[1].println("0: コール    1: フォルド");

                            pl2 = MyServer.myIn[1].readLine();

                            if (pl2.equals("0")) {
                                MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                                chip2 -= (betting1 - betting2);
                                bettingChip += (betting1 - betting2);
                                betting2 = betting1;
                            } else {
                                MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                                MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                                MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                                MyServer.SendBoth("");

                                betting1 = 0;
                                betting2 = 0;
                                bettingChip = 0;

                                Game game = new Game(chip1, chip2);
                                game.start();
                            }
                        } else {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                            MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がチェックを宣言しました。");
                        MyServer.SendBoth("ディーラー: 両プレイヤー様ともにチェックを宣言しましたので、" +
                                "第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                }
            } else {
                MyServer.SendBoth("ディーラー: 偶数セットは" + player2 + "様から宣言していただきます。");
                MyServer.SendBoth("");

                MyServer.myOut[1].println("0: ベット    1: チェック");
                pl2 = MyServer.myIn[1].readLine();

                if (pl2.equals("0")) {
                    MyServer.myOut[1].println("ベットするチップ数を宣言してください。(5～15)");

                    pl2 = MyServer.myIn[1].readLine();

                    MyServer.SendBoth("ディーラー: " + player2 + "様が" + pl2 + "枚のチップをベットしました。");

                    betting2 = Integer.parseInt(pl2);
                    chip2 -= betting2;
                    bettingChip += betting2;
                    better = 1;

                    MyServer.myOut[0].println("0: コール    1: レイズ    2: フォルド");

                    pl1 = MyServer.myIn[0].readLine();

                    if (pl1.equals("0")) {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                        chip1 -= betting2;
                        bettingChip += betting2;
                    } else if (pl1.equals("1")) {
                        MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting2 + "～15");

                        pl1 = MyServer.myIn[0].readLine();

                        MyServer.SendBoth("ディーラー: " + player1 + "様がレイズを宣言し、" +
                                pl1 + "枚のチップをベットしました。");

                        betting1 = Integer.parseInt(pl1);
                        chip1 -= betting1;
                        bettingChip += betting1;
                        better = 0;

                        MyServer.myOut[1].println("0: コール    1: フォルド");

                        pl2 = MyServer.myIn[1].readLine();

                        if (pl2.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                            chip2 -= (betting1 - betting2);
                            bettingChip += (betting1 - betting2);
                            betting2 = betting1;
                        } else {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                            MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                        MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                } else {
                    MyServer.SendBoth("ディーラー: " + player2 + "様がチェックを宣言しました。");

                    MyServer.myOut[0].println("0: ベット    1: チェック");
                    pl1 = MyServer.myIn[0].readLine();

                    if (pl1.equals("0")) {
                        MyServer.myOut[0].println("ベットするチップ数を宣言してください。(5～15)");
                        pl1 = MyServer.myIn[0].readLine();

                        MyServer.SendBoth("ディーラー: " + player1 + "様が" + pl1 + "枚のチップをベットしました。");

                        betting1 = Integer.parseInt(pl1);
                        chip1 -= betting1;
                        bettingChip += betting1;
                        better = 0;

                        MyServer.myOut[1].println("0: コール    1: レイズ    2: フォルド");

                        pl2 = MyServer.myIn[1].readLine();
                        if (pl2.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                            chip2 -= betting1;
                            bettingChip += betting1;
                        } else if (pl2.equals("1")) {
                            MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting1 + "～15");

                            pl2 = MyServer.myIn[1].readLine();

                            MyServer.SendBoth("ディーラー: " + player2 + "様がレイズを宣言し、" +
                                    pl2 + "枚のチップをベットしました。");

                            betting2 = Integer.parseInt(pl2);
                            chip2 -= betting2;
                            bettingChip += betting2;
                            better = 1;

                            MyServer.myOut[0].println("0: コール    1: フォルド");

                            pl1 = MyServer.myIn[0].readLine();

                            if (pl1.equals("0")) {
                                MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                                chip1 -= (betting2 - betting1);
                                bettingChip += (betting2 - betting1);
                                betting1 = betting2;
                            } else {
                                MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                                MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                                MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                                MyServer.SendBoth("");

                                betting1 = 0;
                                betting2 = 0;
                                bettingChip = 0;

                                Game game = new Game(chip1, chip2);
                                game.start();
                            }
                        } else {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 第" + counter + "セット目は勝負不成立となります。");
                            MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がチェックを宣言しました。");
                        MyServer.SendBoth("ディーラー: 両プレイヤー様ともにチェックを宣言しましたので、" +
                                "第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                }
            }

            MyServer.SendBoth("ディーラー: 1stベットが終了しました。");
            MyServer.SendBoth("ディーラー: 現在の賭けチップの合計は" + bettingChip + "枚です。");
            MyServer.SendBoth("ディーラー: また、両プレイヤー様の現在の所持チップは次の通りです。");
            MyServer.SendBoth(player1 + "様: " + chip1 + "　　　" + player2 + "様: " + chip2);
            MyServer.SendBoth("");

            if (better == 0) {
                MyServer.SendBoth("ディーラー: それでは" + player1 + "様より手札の交換を行っていただきます。");
                MyServer.SendBoth("");
                MyServer.myOut[0].println("あなたの手札: " + Arrays.toString(hand1));
                MyServer.myOut[0].println("交換したいカードの位置を数字(左から0,1,2,3,4)で指定してください。");
                MyServer.myOut[0].println("数字は','で区切ってください。(例: 0,2,3)");
                MyServer.myOut[0].println("交換を希望しない場合は5を入力してください。");

                pl1 = MyServer.myIn[0].readLine();

                cards.handChange(hand1, pl1);

                MyServer.myOut[0].println("交換後のあなたの手札: " + Arrays.toString(hand1));
                MyServer.myOut[0].println("");

                MyServer.SendBoth("ディーラー: 続いて" + player2 + "様に手札の交換を行っていただきます。");
                MyServer.SendBoth("");
                MyServer.myOut[1].println("あなたの手札: " + Arrays.toString(hand2));
                MyServer.myOut[1].println("交換したいカードの位置を数字(左から0,1,2,3,4)で指定してください。");
                MyServer.myOut[1].println("数字は','で区切ってください。(例: 0,2,3)");
                MyServer.myOut[1].println("交換を希望しない場合は5を入力してください。");

                pl2 = MyServer.myIn[1].readLine();

                cards.handChange(hand2, pl2);

                MyServer.myOut[1].println("交換後のあなたの手札: " + Arrays.toString(hand2));
                MyServer.myOut[1].println("");
            } else {
                MyServer.SendBoth("ディーラー: それでは" + player2 + "様より手札の交換を行っていただきます。");
                MyServer.SendBoth("");
                MyServer.myOut[1].println("あなたの手札: " + Arrays.toString(hand2));
                MyServer.myOut[1].println("交換したいカードの位置を数字(左から0,1,2,3,4)で指定してください。");
                MyServer.myOut[1].println("数字は','で区切ってください。(例: 0,2,3)");
                MyServer.myOut[1].println("交換を希望しない場合は5を入力してください。");

                pl2 = MyServer.myIn[1].readLine();

                cards.handChange(hand2, pl2);

                MyServer.myOut[1].println("交換後のあなたの手札: " + Arrays.toString(hand2));
                MyServer.myOut[1].println("");

                MyServer.SendBoth("ディーラー: 続いて" + player1 + "様に手札の交換を行っていただきます。");
                MyServer.SendBoth("");
                MyServer.myOut[0].println("あなたの手札: " + Arrays.toString(hand1));
                MyServer.myOut[0].println("交換したいカードの位置を数字(左から0,1,2,3,4)で指定してください。");
                MyServer.myOut[0].println("数字は','で区切ってください。(例: 0,2,3)");
                MyServer.myOut[0].println("交換を希望しない場合は5を入力してください。");

                pl1 = MyServer.myIn[0].readLine();

                cards.handChange(hand1, pl1);

                MyServer.myOut[0].println("交換後のあなたの手札: " + Arrays.toString(hand1));
                MyServer.myOut[0].println("");
            }

            MyServer.SendBoth("ディーラー: それでは2ndベットを開始します。");

            if ((counter % 2) == 1) {
                MyServer.SendBoth("ディーラー: 奇数セットは" + player1 + "様から宣言していただきます。");
                MyServer.SendBoth("");

                MyServer.myOut[0].println("0: ベット    1: チェック");
                pl1 = MyServer.myIn[0].readLine();

                if (pl1.equals("0")) {
                    MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting1 + "～30)");

                    pl1 = MyServer.myIn[0].readLine();

                    MyServer.SendBoth("ディーラー: " + player1 + "様が" + pl1 + "枚のチップをベットしました。");

                    betting1 = Integer.parseInt(pl1);
                    chip1 -= betting1;
                    bettingChip += betting1;

                    MyServer.myOut[1].println("0: コール    1: レイズ    2: フォルド");

                    pl2 = MyServer.myIn[1].readLine();

                    if (pl2.equals("0")) {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                        chip2 -= betting1;
                        bettingChip += betting1;
                    } else if (pl2.equals("1")) {
                        MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting1 + "～30");

                        pl2 = MyServer.myIn[1].readLine();

                        MyServer.SendBoth("ディーラー: " + player2 + "様がレイズを宣言し、" +
                                pl2 + "枚のチップをベットしました。");

                        betting2 = Integer.parseInt(pl2);
                        chip2 -= betting2;
                        bettingChip += betting2;

                        MyServer.myOut[0].println("0: コール    1: フォルド");

                        pl1 = MyServer.myIn[0].readLine();

                        if (pl1.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                            chip1 -= (betting2 - betting1);
                            bettingChip += (betting2 - betting1);
                            betting1 = betting2;
                        } else {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player2 + "様に入ります。");

                            chip2 += bettingChip;

                            MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                        MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player1 + "様に入ります。");

                        chip1 = bettingChip;

                        MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                } else {
                    MyServer.SendBoth("ディーラー: " + player1 + "様がチェックを宣言しました。");

                    MyServer.myOut[1].println("0: ベット    1: チェック");
                    pl2 = MyServer.myIn[1].readLine();

                    if (pl2.equals("0")) {
                        MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting2 + "～30)");
                        pl2 = MyServer.myIn[1].readLine();

                        MyServer.SendBoth("ディーラー: " + player2 + "様が" + pl2 + "枚のチップをベットしました。");

                        betting2 = Integer.parseInt(pl2);
                        chip2 -= betting2;
                        bettingChip += betting2;

                        MyServer.myOut[0].println("0: コール    1: レイズ    2: フォルド");

                        pl1 = MyServer.myIn[0].readLine();
                        if (pl1.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                            chip1 -= betting2;
                            bettingChip += betting2;
                        } else if (pl1.equals("1")) {
                            MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting2 + "～30");

                            pl1 = MyServer.myIn[0].readLine();

                            MyServer.SendBoth("ディーラー: " + player1 + "様がレイズを宣言し、" +
                                    pl1 + "枚のチップをベットしました。");

                            betting1 = Integer.parseInt(pl1);
                            chip1 -= betting1;
                            bettingChip += betting1;

                            MyServer.myOut[1].println("0: コール    1: フォルド");

                            pl2 = MyServer.myIn[1].readLine();

                            if (pl2.equals("0")) {
                                MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                                chip2 -= (betting1 - betting2);
                                bettingChip += (betting1 - betting2);
                                betting2 = betting1;
                            } else {
                                MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                                MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player1 + "様に入ります。");

                                chip1 += bettingChip;

                                MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                                MyServer.SendBoth("");

                                betting1 = 0;
                                betting2 = 0;
                                bettingChip = 0;

                                Game game = new Game(chip1, chip2);
                                game.start();
                            }
                        } else {
                            MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player2 + "様に入ります。");

                            chip2 += bettingChip;

                            MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player2 + "様がチェックを宣言しました。");
                        MyServer.SendBoth("ディーラー: 両プレイヤー様ともにチェックを宣言しましたので、" +
                                "第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                }
            } else {
                MyServer.SendBoth("ディーラー: 偶数セットは" + player2 + "様から宣言していただきます。");
                MyServer.SendBoth("");

                MyServer.myOut[1].println("0: ベット    1: チェック");
                pl2 = MyServer.myIn[1].readLine();

                if (pl2.equals("0")) {
                    MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting2 + "～30)");

                    pl2 = MyServer.myIn[1].readLine();

                    MyServer.SendBoth("ディーラー: " + player2 + "様が" + pl2 + "枚のチップをベットしました。");

                    betting2 = Integer.parseInt(pl2);
                    chip2 -= betting2;
                    bettingChip += betting2;

                    MyServer.myOut[0].println("0: コール    1: レイズ    2: フォルド");

                    pl1 = MyServer.myIn[0].readLine();

                    if (pl1.equals("0")) {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                        chip1 -= betting2;
                        bettingChip += betting2;
                    } else if (pl1.equals("1")) {
                        MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting2 + "～30");

                        pl1 = MyServer.myIn[0].readLine();

                        MyServer.SendBoth("ディーラー: " + player1 + "様がレイズを宣言し、" +
                                pl1 + "枚のチップをベットしました。");

                        betting1 = Integer.parseInt(pl1);
                        chip1 -= betting1;
                        bettingChip += betting1;

                        MyServer.myOut[1].println("0: コール    1: フォルド");

                        pl2 = MyServer.myIn[1].readLine();

                        if (pl2.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                            chip2 -= (betting1 - betting2);
                            bettingChip += (betting1 - betting2);
                            betting2 = betting1;
                        } else {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player1 + "様に入ります。");

                            chip1 += bettingChip;

                            MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                        MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player2 + "様に入ります。");

                        chip2 += bettingChip;

                        MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                } else {
                    MyServer.SendBoth("ディーラー: " + player2 + "様がチェックを宣言しました。");

                    MyServer.myOut[0].println("0: ベット    1: チェック");
                    pl1 = MyServer.myIn[0].readLine();

                    if (pl1.equals("0")) {
                        MyServer.myOut[0].println("ベットするチップ数を宣言してください。(" + betting1 + "～30)");
                        pl1 = MyServer.myIn[0].readLine();

                        MyServer.SendBoth("ディーラー: " + player1 + "様が" + pl1 + "枚のチップをベットしました。");

                        betting1 = Integer.parseInt(pl1);
                        chip1 -= betting1;
                        bettingChip += betting1;

                        MyServer.myOut[1].println("0: コール    1: レイズ    2: フォルド");

                        pl2 = MyServer.myIn[1].readLine();
                        if (pl2.equals("0")) {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がコールしました。");

                            chip2 -= betting1;
                            bettingChip += betting1;
                        } else if (pl2.equals("1")) {
                            MyServer.myOut[1].println("ベットするチップ数を宣言してください。(" + betting1 + "～30");

                            pl2 = MyServer.myIn[1].readLine();

                            MyServer.SendBoth("ディーラー: " + player2 + "様がレイズを宣言し、" +
                                    pl2 + "枚のチップをベットしました。");

                            betting2 = Integer.parseInt(pl2);
                            chip2 -= betting2;
                            bettingChip += betting2;

                            MyServer.myOut[0].println("0: コール    1: フォルド");

                            pl1 = MyServer.myIn[0].readLine();

                            if (pl1.equals("0")) {
                                MyServer.SendBoth("ディーラー: " + player1 + "様がコールしました。");

                                chip1 -= (betting2 - betting1);
                                bettingChip += (betting2 - betting1);
                                betting1 = betting2;
                            } else {
                                MyServer.SendBoth("ディーラー: " + player1 + "様がフォルドしました。");
                                MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player2 + "様に入ります。");

                                chip2 += bettingChip;

                                MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                                MyServer.SendBoth("");

                                betting1 = 0;
                                betting2 = 0;
                                bettingChip = 0;

                                Game game = new Game(chip1, chip2);
                                game.start();
                            }
                        } else {
                            MyServer.SendBoth("ディーラー: " + player2 + "様がフォルドしました。");
                            MyServer.SendBoth("ディーラー: 現在場に出ているアンティを除いた賭けチップが" + player1 + "様に入ります。");

                            chip1 += bettingChip;

                            MyServer.SendBoth("ディーラー: アンティは回収させていただきます。");
                            MyServer.SendBoth("");

                            betting1 = 0;
                            betting2 = 0;
                            bettingChip = 0;

                            Game game = new Game(chip1, chip2);
                            game.start();
                        }
                    } else {
                        MyServer.SendBoth("ディーラー: " + player1 + "様がチェックを宣言しました。");
                        MyServer.SendBoth("ディーラー: 両プレイヤー様ともにチェックを宣言しましたので、" +
                                "第" + counter + "セット目は勝負不成立となります。");
                        MyServer.SendBoth("ディーラー: 場のチップは回収させていただきます。");
                        MyServer.SendBoth("");

                        betting1 = 0;
                        betting2 = 0;
                        bettingChip = 0;

                        Game game = new Game(chip1, chip2);
                        game.start();
                    }
                }
            }

            MyServer.SendBoth("ディーラー: それでは手札のオープンです。");
            MyServer.SendBoth("ディーラー: " + player1 + "様の手札: " + Arrays.toString(hand1));

            role1 = cards.checkRole(hand1);

            MyServer.SendBoth("ディーラー: 完成した役は" + role1 + "です。");
            MyServer.SendBoth("");

            MyServer.SendBoth("ディーラー: " + player2 + "様の手札: " + Arrays.toString(hand2));

            role2 = cards.checkRole(hand2);

            MyServer.SendBoth("ディーラー: 完成した役は" + role2 + "です。");
            MyServer.SendBoth("");

            if (cards.roleCompare(role1, role2)) {
                MyServer.SendBoth("ディーラー: よって、第" + counter + "セット目は" + player1 + "様の勝利です。");
                MyServer.SendBoth("ディーラー: 勝利した" + player1 + "様にはアンティを含めた賭けチップ全てが入ります。");
                MyServer.SendBoth("");

                chip1 += (bettingChip + 10);
            } else {
                MyServer.SendBoth("ディーラー: よって、第" + counter + "セット目は" + player2 + "様の勝利です。");
                MyServer.SendBoth("ディーラー: 勝利した" + player2 + "様にはアンティを含めた賭けチップ全てが入ります。");
                MyServer.SendBoth("");

                chip2 += (bettingChip + 10);
            }

            MyServer.SendBoth("ディーラー: これにて第" + counter + "セット目のゲームを終了します");
            MyServer.SendBoth("");

            if (counter < setCount) {
                Game game = new Game(chip1, chip2);
                game.start();
            } else {
                MyServer.SendBoth("ディーラー: 以上で全てのセットが終了しました。");
                MyServer.SendBoth("ディーラー: 両プレイヤー様の最終的な所持チップは次の通りです。");
                MyServer.SendBoth(player1 + "様: " + chip1 + "    " + player2 + "様: " + chip2);

                if (chip1 > chip2) {
                    MyServer.SendBoth("ディーラー: 今回のゲームの勝者は" + player1 + "様です。おめでとうございます。");
                } else {
                    MyServer.SendBoth("ディーラー: 今回のゲームの勝者は" + player2 + "様です。おめでとうございます。");
                }

                MyServer.SendBoth("今回の記録をランキングに登録しますか？");
                MyServer.SendBoth("0: はい    1: いいえ");

                pl1 = MyServer.myIn[0].readLine();

                if (pl1.equals("0")) {
                    File file = new File("C:\\Users\\Kanae\\IdeaProjects\\情報理工学実験B_ソフトウェア制作2\\src\\ranking.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));

                    int j = 0;
                    String[] rank = new String[10];
                    String str;

                    while ((str = br.readLine()) != null) {
                        rank[j] = str;
                        j++;
                    }

                    br.close();

                    String[][] rank_split = new String[10][];

                    for (int i = 0; i < 10; i++) {
                        rank_split[i] = rank[i].split(",");
                    }

                    for (int i = 0; i < 10; i++) {
                        if (rank_split[i][2].equals("")) {
                            rank[i] = rank_split[i][0] + "," + player1 + "," + chip1;

                            MyServer.myOut[0].println("あなたのスコアがランキングに登録されました。" +
                                    "順位は" + (i+1) + "位です。");
                            break;
                        } else if (chip1 > Integer.parseInt(rank_split[i][2])) {
                            for (int k = 9; k > i; k--) {
                                rank[k] = (k+1) + "," + rank_split[k-1][1] + "," + rank_split[k-1][2];
                            }

                            rank[i] = (i+1) + "," + player1 + "," + chip1;

                            MyServer.myOut[0].println("あなたのスコアがランキングに登録されました。" +
                                    "順位は" + (i+1) + "位です。");
                            break;
                        }

                        if (i == 9) {
                            MyServer.myOut[0].println("あなたのスコアは残念ながらランキング外でした。");
                        }
                    }

                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                    for (int i = 0; i < 10; i++) {
                        bw.write(rank[i]);
                        bw.newLine();
                    }

                    bw.close();
                }

                pl2 = MyServer.myIn[1].readLine();

                if (pl2.equals("0")) {
                    File file = new File("C:\\Users\\Kanae\\IdeaProjects\\情報理工学実験B_ソフトウェア制作2\\src\\ranking.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));

                    int j = 0;
                    String[] rank = new String[10];
                    String str;

                    while ((str = br.readLine()) != null) {
                        rank[j] = str;
                        j++;
                    }

                    br.close();

                    String[][] rank_split = new String[10][];

                    for (int i = 0; i < 10; i++) {
                        rank_split[i] = rank[i].split(",");
                    }

                    for (int i = 0; i < 10; i++) {
                        if (rank_split[i][2].equals("")) {
                            rank[i] = rank_split[i][0] + "," + player2 + "," + chip2;

                            MyServer.myOut[1].println("あなたのスコアがランキングに登録されました。" +
                                    "順位は" + (i+1) + "位です。");

                            break;
                        } else if (chip2 > Integer.parseInt(rank_split[i][2])) {
                            for (int k = 9; k > i; k--) {
                                rank[k] = (k+1) + "," + rank_split[k-1][1] + "," + rank_split[k-1][2];
                            }

                            rank[i] = (i+1) + "," + player2 + "," + chip2;

                        MyServer.myOut[1].println("あなたのスコアがランキングに登録されました。" +
                                    "順位は" + (i+1) + "位です。");

                            break;
                        }

                        if (i == 9) {
                            MyServer.myOut[1].println("あなたのスコアは残念ながらランキング外でした。");
                        }
                    }

                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                    for (int i = 0; i < 10; i++) {
                        bw.write(rank[i]);
                        bw.newLine();
                    }

                    bw.close();
                }

                MyServer.SendBoth("ディーラー: またのご参加をお待ちしております。");
            }

        } catch (IOException e) {
            System.err.println("エラーが発生しました: " + e);
        }
    }
}

class Cards {
    String[] deck = new String[17];
    String[] suit = {"♥", "♣", "♦", "♠"};
    String[] num = {"K", "Q", "J", "A"};
    String joker = "Jk";
    int nextPlace;

    Cards() {
        deck[0] = "[" + joker + "]";

        for (int i = 0; i < suit.length; i++) {
            for (int j = 0; j < num.length; j++) {
                deck[1+j+4*i] = "[" + suit[i] + num[j] + "]";
            }
        }
    }

    public void riffleShuffle(int n) {
        String[] tmp = new String[17];
        String[] cardsA = new String[9];
        String[] cardsB = new String[8];

        for (int i = 0; i < n; i++) {
            System.arraycopy(deck, 0, cardsA, 0, 9);
            System.arraycopy(deck, 9, cardsB, 0, 8);

            for (int j = 0; j < 8; j++) tmp[j * 2 + 1] = cardsB[j];
            for (int j = 0; j < 9; j++) tmp[j * 2] = cardsA[j];

            deck = tmp;
        }
    }

    public void hinduShuffle() {
        Random r = new Random();
        String[] tmp = new String[17];
        String[] cardsA, cardsB, cardsC;
        int j = 17;
        int k = 0;

        cardsC = deck;

        while (true) {
            j = j-k;
            k = r.nextInt(j);

            if (k != 0) {
                cardsA = new String[k];
                System.arraycopy(cardsC, 0, cardsA, 0, k);

                cardsB = new String[j-k];
                System.arraycopy(cardsC, k, cardsB, 0, j-k);

                for (int i = j-1; i > j-k-1; i--) tmp[i] = cardsA[i-j+k];

                cardsC = cardsB;
            } else {
                for (int i = j-1; i >= 0; i--) tmp[i] = cardsC[i];
                break;
            }
        }

        deck = tmp;
    }

    public void cut(int n) {
        String[] tmp = new String[17];
        String[] cardsA = new String[n];
        String[] cardsB = new String[17-n];

        System.arraycopy(deck, 0, cardsA, 0, n);
        System.arraycopy(deck, n, cardsB, 0, 17-n);

        for (int i = 0; i < 17-n; i++) tmp[i] = cardsB[i];
        for (int i = 0; i < n; i++) tmp[i+17-n] = cardsA[i];

        deck = tmp;
    }

    public void deal(String[] a, String[] b) {
        System.arraycopy(deck, 0, a, 0, 5);
        System.arraycopy(deck, 5, b, 0,5);

        nextPlace = 10;
    }

    public void handChange(String[] a, String b) {
        if (!(b.equals("5"))) {
            String[] num = b.split(",");

            for (int i = 0; i < num.length; i++) a[Integer.parseInt(num[i])] = deck[nextPlace + i];
            nextPlace += num.length;
        }
    }

    public String checkRole(String[] a) {
        int[][] deck_num = {{0, 0}, // [Jk]
                {1, 13}, {1, 12}, {1, 11}, {1, 1}, // [♥K],[♥Q],[♥J],[♥A]
                {2, 13}, {2, 12}, {2, 11}, {2, 1}, // [♣K],[♣Q],[♣J],[♣A]
                {3, 13}, {3, 12}, {3, 11}, {3, 1}, // [♦K],[♦Q],[♦J],[♦A]
                {4, 13}, {4, 12}, {4, 11}, {4, 1}}; // [♠K],[♠Q],[♠J],[♠A]

        int[][] flush = {{0, 1, 1, 1, 1}, {0, 2, 2, 2, 2},
                {0, 3, 3, 3, 3}, {0, 4, 4, 4, 4}};

        int[][] five = {{0, 13, 13, 13, 13}, {0, 12, 12, 12, 12},
                {0, 11, 11, 11, 11}, {0, 1, 1, 1, 1}};

        int[][] four = {{0, 1, 13, 13, 13}, {0, 11, 13, 13, 13},
                {0, 12, 13, 13, 13}, {1, 13, 13, 13, 13},
                {11, 13, 13, 13, 13}, {12, 13, 13, 13, 13},
                {0, 1, 12, 12, 12}, {0, 11, 12, 12, 12},
                {0, 12, 12, 12, 13}, {1, 12, 12, 12, 12},
                {11, 12, 12, 12, 12}, {12, 12, 12, 12, 13},
                {0, 1, 11, 11, 11}, {0, 11, 11, 11, 12},
                {0, 11, 11, 11, 13}, {1, 11, 11, 11, 11},
                {11, 11, 11, 11, 12}, {11, 11, 11, 11, 13},
                {0, 1, 1, 1, 11}, {0, 1, 1, 1, 12},
                {0, 1, 1, 1, 13}, {1, 1, 1, 1, 11},
                {1, 1, 1, 1, 12}, {1, 1, 1, 1, 13}};

        int[][] fullHouse = {{0, 11, 11, 13, 13}, {0, 12, 12, 13, 13},
                {1, 1, 13, 13, 13}, {11, 11, 13, 13, 13},
                {12, 12, 13, 13, 13}, {0, 11, 11, 12, 12},
                {1, 1, 12, 12, 12}, {11, 11, 12, 12, 12},
                {12, 12, 12, 13, 13}, {1, 1, 11, 11, 11},
                {11, 11, 11, 12, 12}, {11, 11, 11, 13, 13},
                {0, 1, 1, 11, 11}, {0, 1, 1, 12, 12},
                {0, 1, 1, 13, 13}, {1, 1, 1, 11, 11},
                {1, 1, 1, 12, 12}, {1, 1, 1, 13, 13}};

        int[] straight = {0, 1, 11, 12, 13};

        int[][] three = {{0, 1, 11, 13, 13}, {0, 1, 12, 13, 13},
                {0, 11, 12, 13, 13}, {1, 11, 13, 13, 13},
                {1, 12, 13, 13, 13}, {11, 12, 13, 13, 13},
                {0, 1, 11, 12, 12}, {0, 1, 12, 12, 13},
                {0, 11, 12, 12, 13}, {1, 11, 12, 12, 12},
                {1, 12, 12, 12, 13}, {11, 12, 12, 12, 13},
                {0, 1, 11, 11, 12}, {0, 1, 11, 11, 13},
                {0, 11, 11, 12, 13}, {1, 11, 11, 11, 12},
                {1, 11, 11, 11, 13}, {11, 11, 11, 12, 13},
                {0, 1, 1, 11, 12}, {0, 1, 1, 11, 13},
                {0, 1, 1, 12, 13}, {1, 1, 1, 11, 12},
                {1, 1, 1, 11, 13}, {1, 1, 1, 12, 13}};

        int[][] two = {{1, 11, 11, 13, 13}, {11, 11, 12, 13, 13},
                {1, 12, 12, 13, 13}, {11, 12, 12, 13, 13},
                {1, 11, 11, 12, 12}, {11, 11, 12, 12, 13},
                {1, 1, 11, 13, 13}, {1, 1, 12, 13, 13},
                {1, 1, 11, 12, 12}, {1, 1, 12, 12, 13},
                {1, 1, 11, 11, 12}, {1, 1, 11, 11, 13}};

        int[][] one = {{1, 11, 12, 13, 13}, {1, 11, 12, 12, 13},
                {1, 11, 11, 12, 13}, {1, 1, 11, 12, 13}};

        int[][] suit_num = new int[5][2]; // 手札を{suit, num}で表す
        int[] hand_suit = new int[5];
        int[] hand_num = new int[5];

        for (int i = 0; i < a.length; i++) {
            switch (a[i]) {
                case "[Jk]" -> suit_num[i] = deck_num[0];
                case "[♥K]" -> suit_num[i] = deck_num[1];
                case "[♥Q]" -> suit_num[i] = deck_num[2];
                case "[♥J]" -> suit_num[i] = deck_num[3];
                case "[♥A]" -> suit_num[i] = deck_num[4];
                case "[♣K]" -> suit_num[i] = deck_num[5];
                case "[♣Q]" -> suit_num[i] = deck_num[6];
                case "[♣J]" -> suit_num[i] = deck_num[7];
                case "[♣A]" -> suit_num[i] = deck_num[8];
                case "[♦K]" -> suit_num[i] = deck_num[9];
                case "[♦Q]" -> suit_num[i] = deck_num[10];
                case "[♦J]" -> suit_num[i] = deck_num[11];
                case "[♦A]" -> suit_num[i] = deck_num[12];
                case "[♠K]" -> suit_num[i] = deck_num[13];
                case "[♠Q]" -> suit_num[i] = deck_num[14];
                case "[♠J]" -> suit_num[i] = deck_num[15];
                case "[♠A]" -> suit_num[i] = deck_num[16];
            }
        }

        for (int i = 0; i < 5; i++) {
            hand_suit[i] = suit_num[i][0];
            hand_num[i] = suit_num[i][1];
        }

        Arrays.sort(hand_suit);
        Arrays.sort(hand_num);

        for (int i = 0; i < 4; i++) if (hand_suit == flush[i]) return "ロイヤルストレートフラッシュ";

        for (int i = 0; i < 4; i++) {
            if (hand_num == five[i]) {
                switch (i) {
                    case 0:
                        return "ファイブカード,2";
                    case 1:
                        return "ファイブカード,3";
                    case 2:
                        return "ファイブカード,4";
                    case 3:
                        return "ファイブカード,1";
                }
            }
        }

        for (int i = 0; i < 24; i++) {
            if (hand_num == four[i]){
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return "フォーカード,2";
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        return "フォーカード,3";
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        return "フォーカード,4";
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        return "フォーカード,1";
                }
            }
        }

        for (int i = 0; i < 18; i++) {
            if (hand_num == fullHouse[i]) {
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        return "フルハウス,2";
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                        return "フルハウス,3";
                    case 9:
                    case 10:
                    case 11:
                        return "フルハウス,4";
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        return "フルハウス,1";
                }
            }
        }

        if (hand_num == straight) {
            for (int i = 0; i < 5; i++) {
                switch (a[i]) {
                    case "[♥A]":
                        return "ストレート,2";
                    case "[♣A]":
                        return "ストレート,4";
                    case "[♦A]":
                        return "ストレート,3";
                    case "[♠A]":
                        return "ストレート,1";
                }
            }
        }

        for (int i = 0; i < 24; i++) {
            if (hand_num == three[i]) {
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return "スリーカード,2";
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                        return "スリーカード,3";
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        return "スリーカード,4";
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        return "スリーカード,1";
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            if (hand_num == two[i]) {
                switch (i) {
                    case 0:
                        return "ツーペア,9";
                    case 1:
                        return "ツーペア,10";
                    case 2:
                        return "ツーペア,7";
                    case 3:
                        return "ツーペア,8";
                    case 4:
                        return "ツーペア,11";
                    case 5:
                        return "ツーペア,12";
                    case 6:
                        return "ツーペア,2";
                    case 7:
                        return "ツーペア,1";
                    case 8:
                        return "ツーペア,4";
                    case 9:
                        return "ツーペア,3";
                    case 10:
                        return "ツーペア,6";
                    case 11:
                        return "ツーペア,5";
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (hand_num == one[i]) {
                switch (i) {
                    case 0:
                        return "ワンペア,2";
                    case 1:
                        return "ワンペア,3";
                    case 2:
                        return "ワンペア,4";
                    case 3:
                        return "ワンペア,1";
                }
            }
        }

        return "";
    }

    public boolean roleCompare(String a, String b) {
        int score_a = 0;
        int score_b = 0;

        String[] a_split = a.split(",");
        String[] b_split = b.split(",");


        switch (a) {
            case "ファイブカード" -> score_a = 1;
            case "ロイヤルストレートフラッシュ" -> score_a = 2;
            case "フォーカード" -> score_a = 3;
            case "フルハウス" -> score_a = 4;
            case "ストレート" -> score_a = 5;
            case "スリーカード" -> score_a = 6;
            case "ツーペア" -> score_a = 7;
            case "ワンペア" -> score_a = 8;
        }

        switch (b) {
            case "ファイブカード" -> score_b = 1;
            case "ロイヤルストレートフラッシュ" -> score_b = 2;
            case "フォーカード" -> score_b = 3;
            case "フルハウス" -> score_b = 4;
            case "ストレート" -> score_b = 5;
            case "スリーカード" -> score_b = 6;
            case "ツーペア" -> score_b = 7;
            case "ワンペア" -> score_b = 8;
        }

        if (score_a > score_b) return true;
        else if (score_a < score_b) return false;
        else return Integer.parseInt(a_split[1]) < Integer.parseInt(b_split[1]);
    }
}