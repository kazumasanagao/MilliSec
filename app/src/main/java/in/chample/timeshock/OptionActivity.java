package in.chample.timeshock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;

public class OptionActivity extends AppCompatActivity {

    SharedPreferences sharedPref;
    RadioButton radioButton1;
    RadioButton radioButton2;
    RadioButton radioButton3;
    RadioButton radioButton4;
    RadioButton radioButton5;
    LinearLayout layout_ad;
    AdView adView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.option);
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

        // ラジオボタンを取得
        radioButton1 = (RadioButton) findViewById(R.id.radiobutton1);
        radioButton2 = (RadioButton) findViewById(R.id.radiobutton2);
        radioButton3 = (RadioButton) findViewById(R.id.radiobutton3);
        radioButton4 = (RadioButton) findViewById(R.id.radiobutton4);
        radioButton5 = (RadioButton) findViewById(R.id.radiobutton5);

        // ラジオボタンの初期状態

        RadioGroup group = (RadioGroup) findViewById(R.id.radiogroup);

        int currentLevel = sharedPref.getInt("level", 0);
        switch (currentLevel) {
            case 1:
                if (group != null) group.check(R.id.radiobutton1);
                break;
            case 2:
                if (group != null) group.check(R.id.radiobutton2);
                break;
            case 3:
                if (group != null) group.check(R.id.radiobutton3);
                break;
            case 4:
                if (group != null) group.check(R.id.radiobutton4);
                break;
            case 5:
                if (group != null) group.check(R.id.radiobutton5);
                break;
            default:
                break;
        }

        // ラジオボタンの活性・非活性を判定

        final int numOfActive; // アクティブなレベルの数。たとえば４ならHardまでアクティブ。
        if (sharedPref.getInt("highscore4-0", 0) >= 15 || sharedPref.getInt("highscore4-1", 0) >= 15) {
            numOfActive = 5;
        } else if (sharedPref.getInt("highscore3-0", 0) >= 15 || sharedPref.getInt("highscore3-1", 0) >= 15) {
            numOfActive = 4;
            radioButton5.setTextColor(0x80ffffff);
        } else if (sharedPref.getInt("highscore2-0", 0) >= 15 || sharedPref.getInt("highscore2-1", 0) >= 15) {
            numOfActive = 3;
            radioButton5.setTextColor(0x80ffffff);
            radioButton4.setTextColor(0x80ffffff);
        } else if (sharedPref.getInt("highscore1-0", 0) >= 15 || sharedPref.getInt("highscore1-1", 0) >= 15) {
            numOfActive = 2;
            radioButton5.setTextColor(0x80ffffff);
            radioButton4.setTextColor(0x80ffffff);
            radioButton3.setTextColor(0x80ffffff);
        } else {
            numOfActive = 1;
            radioButton5.setTextColor(0x80ffffff);
            radioButton4.setTextColor(0x80ffffff);
            radioButton3.setTextColor(0x80ffffff);
            radioButton2.setTextColor(0x80ffffff);
        }

        // ラジオボタンのクリック時
        if (group != null) {
            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    RadioButton radio = (RadioButton) findViewById(checkedId);
                    if (radio != null && radio.isChecked()) {
                        int level = 0;
                        switch (checkedId) {
                            case R.id.radiobutton1:
                                level = 1;
                                break;
                            case R.id.radiobutton2:
                                level = 2;
                                break;
                            case R.id.radiobutton3:
                                level = 3;
                                break;
                            case R.id.radiobutton4:
                                level = 4;
                                break;
                            case R.id.radiobutton5:
                                level = 5;
                                break;
                            default:
                                break;
                        }
                        // 非活性ボタンを押した時
                        if (level > numOfActive) {

                            String levelText;
                            levelText = (level == 5) ? "Hard" : (level == 4) ? "Normal" : (level == 3) ? "Easy" : "Beginner";
                            String toastText = getString(R.string.toastmes, levelText);
                            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG).show();

                            // 現在の最も難しいレベルにフォーカスする
                            group.check(getButtonByNum(numOfActive));
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt("level", numOfActive);
                            editor.apply();

                            // 活性ボタンを押した時
                        } else {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt("level", level);
                            editor.apply();
                        }
                    }
                }
            });
        }

        // トグルボタンの初期状態

        ToggleButton tb = (ToggleButton) findViewById(R.id.togglebutton);
        Boolean isSound = sharedPref.getBoolean("issound", true);
        if (tb != null) tb.setChecked(isSound);

        // トグルボタンのクリック時
        if (tb != null) {
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("issound", isChecked);
                    editor.apply();
                }
            });
        }
    }

    public int getButtonByNum(int buttonNum) {
        switch (buttonNum) {
            case 5:
                return R.id.radiobutton5;
            case 4:
                return R.id.radiobutton4;
            case 3:
                return R.id.radiobutton3;
            case 2:
                return R.id.radiobutton2;
            case 1:
                return R.id.radiobutton1;
            default:
                return 0;
        }
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
