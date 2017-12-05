package in.chample.timeshock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;

import java.util.Locale;

/* sharedprefの中身
level(int) 現在の設定レベル
boolean issound(boolean) 音の設定
highscore*-*(int) 設定レベルごとのハイスコア(全10個)
stage(int) 現在のステージ(途中から再開できるために。０は前回の途中データがないことを示す)
*/

public class MainActivity extends AppCompatActivity {

    long start = 0;
    long end = 0;
    int stage = 1;
    boolean duringCountdown;
    boolean duringGame;
    Handler handler;
    Vibrator vibrator;
    boolean isVibrate;
    int targetInt;
    MediaPlayer se;
    Point p;
    RelativeLayout r;
    SharedPreferences sharedPref;
    Double clearTimeBase;
    Double clearTime;
    int currentLevel;
    boolean currentIssound;
    Menu menu;
    AnimatorSet bouncer = new AnimatorSet();
    AnimatorSet bouncer2 = new AnimatorSet();
    int color;
    static float scale;
    LinearLayout layout_ad;
    AdView adView;
    View rootView;
    View redline;
    View redline2;
    TextView startText;
    TextView resultText;
    TextView stageText;
    TextView levelview;
    TextView highscoreView;
    TextView targetText;
    TextView fireText;
    TextView timerView;
    TextView cleartimeView;

    // ----- 初期画面、初期条件の設定 -----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Googleアナリティクス読込
        ((MyApplication)getApplication()).getTracker();

        // GoogleAdMob表示
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.ad_unit_id));
        adView.setAdSize(AdSize.BANNER);
        layout_ad = (LinearLayout) findViewById(R.id.layout_ad);
        if (layout_ad != null) layout_ad.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Viewと変数の結びつけ
        rootView = findViewById(R.id.mainscreen);
        redline = findViewById(R.id.redline);
        redline2 = findViewById(R.id.redline2);
        startText = (TextView) findViewById(R.id.startgame);
        resultText = (TextView) findViewById(R.id.result);
        stageText = (TextView) findViewById(R.id.stage);
        levelview = (TextView) findViewById(R.id.level);
        highscoreView = (TextView) findViewById(R.id.highscore);
        targetText = (TextView) findViewById(R.id.target);
        fireText = (TextView) findViewById(R.id.fire);
        timerView = (TextView)findViewById(R.id.timerView);
        cleartimeView = (TextView)findViewById(R.id.cleartime);

        // dpからpxに変換するためのレシオ
        scale = getResources().getDisplayMetrics().density;

        // Startボタンにリスナーを設定
        startText.setOnClickListener(startButtonClick);

        // 赤いラインにリスナーを設定
        redline.setOnClickListener(redlineClick);
        redline2.setOnClickListener(redlineClick);

        handler = new Handler();

        se = MediaPlayer.create(getApplicationContext(), R.raw.sound);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // レベル、サウンドを設定
        reloadLevel();

        // 途中データがあれば読み込む
        int prevstage = sharedPref.getInt("stage", 0);
        if (prevstage > 1) {
            stage = prevstage;
            stageText.setText(getString(R.string.stage, stage));
            startText.setText(getString(R.string.resume));
        }

    }

    // ----- レベル、サウンドの再設定(画面表示時) -----
    public void reloadLevel() {

        currentLevel = sharedPref.getInt("level", 1);
        currentIssound = sharedPref.getBoolean("issound", true);

        String mute = "";
        if (!currentIssound) mute = getString(R.string.mute);

        int issoundNum = 0;
        if (currentIssound) issoundNum = 1;

        switch (currentLevel) {
            case 1:
                clearTimeBase = 0.4;
                levelview.setText(getString(R.string.beginner, mute));
                break;
            case 2:
                clearTimeBase = 0.35;
                levelview.setText(getString(R.string.easy, mute));
                break;
            case 3:
                clearTimeBase = 0.3;
                levelview.setText(getString(R.string.normal, mute));
                break;
            case 4:
                clearTimeBase = 0.25;
                levelview.setText(getString(R.string.hard, mute));
                break;
            case 5:
                clearTimeBase = 0.2;
                levelview.setText(getString(R.string.crazy, mute));
                break;
            default:
                break;
        }

        // ハイスコアの表示
        int highscore = sharedPref.getInt("highscore"+currentLevel+"-"+issoundNum, 0);
        if (highscore > 0) {
            highscoreView.setText(getString(R.string.highscore, highscore));
        }
    }

    //----- Startボタン押下時の挙動 -----

    View.OnClickListener startButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!duringCountdown && !duringGame) {

                // カウント中のフラグを立てる
                duringCountdown = true;

                // Startボタン、前回結果を消す。ステージ番号を更新。
                startText.setText("");
                resultText.setText("");
                targetText.setText("");
                cleartimeView.setText("");
                stageText.setText(getString(R.string.stage, stage));

                // ３秒のカウントダウン開始
                final MyCountDownTimer cdt = new MyCountDownTimer(4000, 1000);
                cdt.start();
            }
        }
    };

    // ----- ゲーム開始 -----

    public void startGame() {
        if (duringCountdown && !duringGame) {

            // テキスト、バイブ、カラー、サウンドの振り分け
            if (currentIssound) {
                int ran = (int) (Math.random()*4);
                targetInt = (ran < 1) ? 1 : (ran < 2) ? 2 : (ran < 3) ? 3 : 4;
            } else {
                int ran = (int) (Math.random()*3);
                targetInt = (ran < 1) ? 1 : (ran < 2) ? 2 : 3;
            }

            int ran_fire = (int) (Math.random() * 10000 + 2000);
            switch (targetInt) {
                // テキストのとき
                case 1:
                    targetText.setText(getString(R.string.text));
                    handler.postDelayed(fire_runnable, ran_fire);
                    break;
                // バイブのとき
                case 2:
                    targetText.setText(getString(R.string.vibration));
                    handler.postDelayed(fire_runnable, ran_fire);
                    break;
                // カラーのとき
                case 3:
                    int ran = (int) (Math.random()*3);
                    if (ran < 1) {
                        color = Color.RED;
                        targetText.setText(getString(R.string.colorRed));
                    } else if(ran < 2) {
                        targetText.setText(getString(R.string.colorBlue));
                        color = Color.BLUE;
                    } else {
                        targetText.setText(getString(R.string.colorGreen));
                        color = Color.GREEN;
                    }
                    handler.postDelayed(fire_runnable, ran_fire);
                    break;
                // サウンドのとき
                case 4:
                    targetText.setText(getString(R.string.sound));
                    handler.postDelayed(fire_runnable, ran_fire);
                    break;
            }

            // カウントダウン中のフラグを下げ、ゲーム中のフラグを上げる
            duringCountdown = false;
            duringGame = true;

            // ステージによるスピードの変化(Stage11以降は横ばいに)
            int stagemax11 = stage;
            if (stagemax11 > 11) stagemax11 = 11;
            clearTime = clearTimeBase + 0.01 - (stagemax11 * 0.01);
            cleartimeView.setText(getString(R.string.cleartime, String.format(Locale.US ,"%.3f", clearTime), getString(R.string.second)));

            // ステージによる障害物の変化
            levelChange(ran_fire);
        }
    }

    // ----- ステージによる障害物の変化 -----

    public void levelChange(int fireTime) {
        switch (currentLevel) {
            // Beginnerは障害なし
            case 1:
                break;

            // Easyは25%の確率でフェイント１回、stage5以降は横赤ラインあり
            case 2:

                if (Math.random()*100 < 25) {
                    setFakeOnce(fireTime);
                }

                // 横赤ラインの表示とアニメ開始
                if (stage >= 5) {
                    redline.setVisibility(View.VISIBLE);
                    animateTranslation(redline);
                }

                break;

            // Normalは12.5%の確率でフェイント１回、12.5％の確率でフェイント２回、横赤ラインあり、stage10以降は縦赤ラインあり
            case 3:

                double ran = Math.random() * 100;

                if (ran < 12.5) {
                    setFakeTwice(fireTime);
                } else if (ran < 25) {
                    setFakeOnce(fireTime);
                }

                // 横赤ラインの表示とアニメ開始
                redline.setVisibility(View.VISIBLE);
                animateTranslation(redline);

                // 縦赤ラインの表示とアニメ開始
                if (stage >= 10) {
                    redline2.setVisibility(View.VISIBLE);
                    animateTranslation(redline2);
                }

                break;

            // Hard,Crazyは12.5%の確率でフェイント１回、12.5％の確率でフェイント２回、縦横赤ラインあり、赤ライン変速あり
            case 4:
            case 5:
                double ran2 = Math.random() * 100;

                if (ran2 < 12.5) {
                    setFakeTwice(fireTime);
                } else if (ran2 < 25) {
                    setFakeOnce(fireTime);
                }

                // 横赤ラインの表示とアニメ開始
                redline.setVisibility(View.VISIBLE);
                animateTranslation(redline);

                // 縦赤ラインの表示とアニメ開始
                redline2.setVisibility(View.VISIBLE);
                animateTranslation(redline2);

                break;
        }
    }

    // ----- Fakeを１回設定する -----

    public void setFakeOnce(int fireTime) {

        // どのFakeイベントを採用するかを決める
        int fakenum = fakeEventChoicer();

        // 0.2から0.8の乱数の作成
        double faketimeRatio = (Math.random()*6 + 2) * 0.1;

        // 実行時間(ミリセカンド)の決定
        long faketime = Math.round(fireTime * faketimeRatio);

        // Fakeイベントの実行
        fakeExecute(fakenum, faketime);
    }

    // ----- Fakeを２回設定する -----

    public void setFakeTwice(int fireTime) {

        // fakeイベントを２つ選ぶ。カラーは被ってもいいが、ほかは被りなし。
        int fakenum = fakeEventChoicer();
        int fakenum2;
        do {
            fakenum2 = fakeEventChoicer();
        } while (fakenum == fakenum2 && fakenum2 != 3);

        // 0.2から0.8の乱数の作成(0.1刻み)
        double faketimeRatio = (Math.random() * 6 + 2) * 0.1;
        double faketimeRatio2;
        if (faketimeRatio < 0.5) {
            faketimeRatio2 = faketimeRatio + 0.3;
        } else {
            faketimeRatio2 = faketimeRatio - 0.3;
        }

        // 実行時間(ミリセカンド)の決定
        long faketime = Math.round(fireTime * faketimeRatio);
        long faketime2 = Math.round(fireTime * faketimeRatio2);

        // Fakeイベントの実行
        fakeExecute(fakenum, faketime);
        fakeExecute(fakenum2, faketime2);
    }

    // ----- どのFakeイベントを採用するかを決める -----

    public int fakeEventChoicer() {
        // カラー以外のときは、Targetと同じにならないように。カラーのときはカラーの別の色のFakeをつくるのでOK。

        int fakenum;
        int numofcases = 3;
        if (currentIssound) numofcases = 4;

        if (targetInt != 3) { // カラー以外のときは自分は含んではいけない
            fakenum = (int) (Math.ceil(Math.random() * (numofcases - 1)) + targetInt) % numofcases;
            if (fakenum == 0) fakenum = numofcases;
            
        } else { // カラーのときは自分(3番)も含んでいい
            fakenum = (int) Math.ceil(Math.random() * numofcases);
        }

        return fakenum;
    }

    // ----- Fakeイベントの実行 -----

    public void  fakeExecute(int fakenum, long faketime) {
        switch (fakenum) {
            case 1:
                handler.postDelayed(faketext_runnable, faketime);
                break;
            case 2:
                long[] pattern = {faketime, 100};
                vibrator.vibrate(pattern, -1);
                isVibrate = true;
                break;
            case 3:
                handler.postDelayed(fakecolorON_runnable, faketime);
                // 0.5秒後に元に(黒色に)戻す
                handler.postDelayed(fakecolorOFF_runnable, faketime + 500);
                break;
            case 4:
                handler.postDelayed(fakesound_runnable, faketime);
                break;
        }
    }

    // ----- Fake(テキスト) -----

    private final Runnable faketext_runnable = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {
                fireText.setText(getString(R.string.fire));
            }
        }
    };

    // ----- Fake(サウンド) -----

    private final Runnable fakesound_runnable = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {
                se.start();
            }
        }
    };

    // ----- Fake(カラー) -----

    private final Runnable fakecolorON_runnable = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {

                int fakeColor;
                // 本物のFireがカラーではないときは、３色から選ぶ
                if (color == 0) {
                    int selection = (Math.random()*3 < 1) ? 1 : (Math.random()*3 < 2) ? 2 : 3;
                    fakeColor = (selection == 1) ? Color.RED : (selection == 2) ? Color.BLUE : Color.GREEN;

                // 本物のFireもカラーのときは、残りの２色から選ぶ
                } else {
                    int selection = (Math.random()*2 < 1) ? 1 : 2;

                    if (color == Color.RED) {
                        fakeColor = (selection == 1) ? Color.BLUE : Color.GREEN;
                    } else if (color == Color.BLUE) {
                        fakeColor = (selection == 1) ? Color.RED : Color.GREEN;
                    } else {
                        fakeColor = (selection == 1) ? Color.RED : Color.BLUE;
                    }
                }

                // 発火
                rootView.setBackgroundColor(fakeColor);
            }
        }
    };

    private final Runnable fakecolorOFF_runnable = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {
                rootView.setBackgroundColor(Color.BLACK);
            }
        }
    };

    // ----- 発火(テキスト、バイブ、カラー、サウンド) -----

    private final Runnable fire_runnable = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {
                switch (targetInt) {
                    case 1:
                        fireText.setText(getString(R.string.fire));
                        break;
                    case 2:
                        long[] pattern = {0, 100};
                        vibrator.vibrate(pattern, -1);
                        break;
                    case 3:
                        rootView.setBackgroundColor(color);
                        break;
                    case 4:
                        se.start();
                        break;
                }
                start = System.currentTimeMillis();
                handler.postDelayed(leavegameover, 1500);
            }
        }
    };

    // ----- ユーザーがタッチしたとき -----

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!duringCountdown && duringGame) {

            // タッチした時間を取得
            end = System.currentTimeMillis();

            // 結果の補正。表示、再生の完了までに時間がかかっているため。
            double rawResult = end - start;
            switch (targetInt) {
                case 1:
                    rawResult = rawResult - 200;
                    break;
                case 2:
                    rawResult = rawResult - 100;
                    break;
                case 3:
                    rawResult = rawResult - 200;
                    break;
                case 4:
                    rawResult = rawResult - 300;
                    break;
            }

            // 補正の結果マイナスになってしまったら、0.001msとする
            if (rawResult < 0) {
                rawResult = 1;
            }

            // 結果を秒単位に。小数点４位以下は表示しない。
            rawResult = rawResult * 0.001;
            String result = String.format(Locale.US, "%.3f", rawResult) + getString(R.string.second);

            // 発火前にタッチしてしまった場合
            if (start == 0) {
                resultText.setTextColor(Color.RED);
                resultText.setText(getString(R.string.gameover, getString(R.string.miss)));
                gameover();

            // タッチがクリア時間をオーバーした場合
            } else if (rawResult > clearTime) {
                resultText.setTextColor(Color.RED);
                resultText.setText(getString(R.string.gameover, result));
                gameover();

            // クリアした場合
            } else {
                resultText.setTextColor(Color.WHITE);
                resultText.setText(getString(R.string.clear, result));

                stage++;

                Editor editor = sharedPref.edit();
                editor.putInt("stage", stage);
                editor.apply();

                startText.setText(getString(R.string.nextstage));

                // ステージ２に進むときにOPTIONSを非表示にし、PAUSEを表示する。
                if (stage == 2) {
                    menu.getItem(0).setVisible(true);
                    menu.getItem(1).setVisible(false);
                }
            }

            // ゲーム終了共通部分
            gameend();

            return true;
        } else {
            return true;
        }
    }

    // ----- ゲーム終了共通部分(クリア、ゲームオーバ両方に共通) -----

    public void gameend() {

        // ゲーム中のフラグを下げる
        duringGame = false;

        // handlerの中身を全キャンセル (キャンセルしないと次のゲームのときに発火し得る)
        handler.removeCallbacks(fire_runnable);
        handler.removeCallbacks(leavegameover);
        handler.removeCallbacks(faketext_runnable);
        handler.removeCallbacks(fakesound_runnable);
        handler.removeCallbacks(fakecolorON_runnable);
        handler.removeCallbacks(fakecolorOFF_runnable);

        // Fire!を消す。
        fireText.setText("");

        // 各パラメーターの初期化。フェイント用バイブが入っていれば、それをキャンセル。
        start = 0;
        end = 0;
        color = 0;
        if (isVibrate) {
            vibrator.cancel();
            isVibrate = false;
        }
        redline.setVisibility(View.GONE);
        redline2.setVisibility(View.GONE);
        rootView.setBackgroundColor(Color.BLACK);
    }

    // ----- ゲームオーバー共通部分 -----

    public void gameover() {
        savehighscore();
        stage = 1;
        startText.setText(getString(R.string.newgame));

        Editor editor = sharedPref.edit();
        editor.putInt("stage", 0);
        editor.apply();

        // PAUSEを非表示にし、OPTIONSを表示する
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(true);
    }

    // ----- fireしてから１.５秒以上放置したら自動ゲームオーバー -----

    private final Runnable leavegameover = new Runnable() {
        @Override
        public void run() {
            if (!duringCountdown && duringGame) {

                resultText.setTextColor(Color.RED);
                resultText.setText(getString(R.string.gameover, getString(R.string.timeout)));

                gameend();
                gameover();
            }
        }
    };

    // ----- ハイスコアだったら保存 -----

    public void savehighscore() {

        // 例：highscore3-1はNormalの音あり、highscore2-0はEasyの音なし
        int issoundNum = 0;
        if (currentIssound) issoundNum = 1;
        String highscoreId = "highscore"+currentLevel+"-"+issoundNum;
        int highscore = sharedPref.getInt(highscoreId, 0);

        // 今回のスコアと比較
        int thisscore = stage - 1;
        if (highscore < thisscore) {
            Editor editor = sharedPref.edit();
            editor.putInt(highscoreId, thisscore);
            editor.apply();

            highscoreView.setText(getString(R.string.highscore, thisscore));
        }
    }

    // ----- カウントダウン -----

    public class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // カウントダウン完了後に呼ばれる
            timerView.setText("");
            startGame();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            // インターバル(countDownInterval)毎に呼ばれる
            timerView.setText(String.format(Locale.US, "%d", millisUntilFinished/1000%60));
        }
    }

// ----- ここから赤いライン関係 -----

    // 赤いライン設置のために画面サイズ(アプリ有効範囲)を取得

    public static Point getViewSize(View View) {
        Point point = new Point(0, 0);
        point.set(View.getRight(), View.getBottom());
        return point;
    }

    // 画面サイズの取得

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        r = (RelativeLayout)findViewById(R.id.mainscreen);
        p = getViewSize(r);
    }

    // 赤いラインの動き

    private void animateTranslation( View target ) {

        ObjectAnimator objectAnimator;
        ObjectAnimator objectAnimator2;
        AnimatorSet thisbouncer;

        // dpをpxに変換する
        float dp15 = 15 * scale;
        float dp60 = 60 * scale;

        if (target == redline) {
            // 下降の動き
            objectAnimator = ObjectAnimator.ofFloat(target, "translationY", -dp15, p.y - dp60);
            objectAnimator.setDuration(5000);

            // 上昇の動き
            objectAnimator2 = ObjectAnimator.ofFloat(target, "translationY", p.y - dp60, -dp15);
            objectAnimator2.setDuration(5000);

            thisbouncer = bouncer;
        } else {
            // 左から右へ
            objectAnimator = ObjectAnimator.ofFloat(target, "translationX", -dp15, p.x - dp60);
            objectAnimator.setDuration(5000);

            // 右から左へ
            objectAnimator2 = ObjectAnimator.ofFloat(target, "translationX", p.x - dp60, -dp15);
            objectAnimator2.setDuration(5000);

            thisbouncer = bouncer2;
        }

        // アニメに変化をつける(レベルがNormal以上のとき)
        if (currentLevel >= 4) {

            // 60%で普通。20%で開始時に逆方向にため、終了時にはみ出す。20%で開始時に素早く徐々に速度を落とす。
            double ran = Math.random() * 5;
            if (ran < 1) {
                objectAnimator.setInterpolator(new AnticipateOvershootInterpolator());
                objectAnimator2.setInterpolator(new AnticipateOvershootInterpolator());
            } else if (ran < 2) {
                objectAnimator.setInterpolator(new DecelerateInterpolator());
                objectAnimator2.setInterpolator(new DecelerateInterpolator());
            }
        }

        // ２つのアニメを合体して再生
        thisbouncer.play(objectAnimator).before(objectAnimator2);
        thisbouncer.start();

        // リピートさせるための設定
        thisbouncer.addListener(new AnimatorListenerAdapter() {
            private boolean mCanceled;
            @Override
            public void onAnimationStart(Animator animation) {
                mCanceled = false;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    animation.start();
                }
            }
        });
    }

    // 赤いライン押下時の挙動

    View.OnClickListener redlineClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!duringCountdown && duringGame) {

                resultText.setTextColor(Color.RED);
                resultText.setText(getString(R.string.gameover,getString(R.string.redline)));

                gameend();
                gameover();
            }
        }
    };

// ----- ここまで赤いライン関係 -----

// ----- ここからオプションバー関係 -----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;

        // 途中データがあれば、OPTIONSを非表示、なければPAUSEを非表示に。
        int prevstage = sharedPref.getInt("stage", 0);
        if (prevstage > 1) {
            menu.getItem(1).setVisible(false);
        } else {
            menu.getItem(0).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // オプション画面へ
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, OptionActivity.class);
            startActivity(intent);
        }

        // 画面の再読み込み
        if (id == R.id.pause) {
            finish();
            startActivity(getIntent());
        }

        return super.onOptionsItemSelected(item);
    }

// ----- ここまでオプションバー関係 -----

    // ----- 画面離脱時の挙動 -----

    @Override
    public void onPause() {
        if (isVibrate) {
            vibrator.cancel();
            isVibrate = false;
        }
        duringGame = false;
        duringCountdown = false;

        adView.pause();

        super.onPause();
    }

    // ----- 画面復帰時の挙動 -----

    @Override
    public void onRestart() {
        // 途中になっていた場合に、そのステージのStart前に戻るように。
        finish();
        startActivity(getIntent());

        super.onRestart();
    }

    @Override
    public void onResume() {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onDestroy() {
        adView.destroy();
        super.onDestroy();
    }


    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }
    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

}