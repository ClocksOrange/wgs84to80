package com.example.administrator.gws84_to_80;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import static android.R.attr.button;

public class MainActivity extends AppCompatActivity {

    final private double Axis_WGS84_a = 6378137;
    final private double Axis_WGS84_b = 6356752.314;

    final private double Axis_xian_a = 6378140;
    final private double Axis_xian_b = 6356755;

    private Button button;
    private EditText input_B, input_L,input_H ;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input_B = (EditText) findViewById(R.id.input_B);
        input_L = (EditText) findViewById(R.id.input_L);
        input_H = (EditText) findViewById(R.id.input_H);
        textView = (TextView) findViewById(R.id.textView13);
        button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String in_B = input_B.getText().toString();
                String in_L = input_L.getText().toString();
                String in_H = input_H.getText().toString();
                BLHtoXYZ(Double.parseDouble( in_B), Double.parseDouble( in_L), Double.parseDouble( in_H), Axis_WGS84_a, Axis_WGS84_b );

            }
        });
    }

    //第一步转换，大地坐标系换换成空间直角坐标系
    private void BLHtoXYZ(double B, double L, double H, double aAxis, double bAxis) {
        double dblD2R = Math.PI / 180;
        double e1 = Math.sqrt(Math.pow(aAxis, 2) - Math.pow(bAxis, 2)) / aAxis;

        double targetX, targetY, targetZ;

        double N = aAxis / Math.sqrt(1.0 - Math.pow(e1, 2) * Math.pow(Math.sin(B * dblD2R), 2));
        targetX = (N + H) * Math.cos(B * dblD2R) * Math.cos(L * dblD2R);
        targetY = (N + H) * Math.cos(B * dblD2R) * Math.sin(L * dblD2R);
        targetZ = (N * (1.0 - Math.pow(e1, 2)) + H) * Math.sin(B * dblD2R);


        //第二步转换，空间直角坐标系里七参数转换
        TransParaSeven transParaSeven = new TransParaSeven();
        double Ex, Ey, Ez;
        Ex = transParaSeven.m_dbXRotate / 180 * Math.PI;
        Ey = transParaSeven.m_dbYRotate / 180 * Math.PI;
        Ez = transParaSeven.m_dbZRotate / 180 * Math.PI;

        double newX = transParaSeven.m_dbXOffset + transParaSeven.m_dbScale * targetX + targetY * Ez - targetZ * Ey + targetX;
        double newY = transParaSeven.m_dbYOffset + transParaSeven.m_dbScale * targetY - targetX * Ez + targetZ * Ex + targetY;
        double newZ = transParaSeven.m_dbZOffset + transParaSeven.m_dbScale * targetZ + targetX * Ey - targetY * Ex + targetZ;
        XYZtoBLH(newX, newY, newZ, Axis_xian_a, Axis_xian_b);
    }

    //第三步转换，空间直接坐标系转换为大地坐标系
    private void XYZtoBLH(double X, double Y, double Z, double aAxis, double bAxis) {
        double e1 = (Math.pow(aAxis, 2) - Math.pow(bAxis, 2)) / Math.pow(aAxis, 2);
        double e2 = (Math.pow(aAxis, 2) - Math.pow(bAxis, 2)) / Math.pow(bAxis, 2);

        double S = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2));
        double cosL = X / S;
        double B = 0;
        double L = 0;

        L = Math.acos(cosL);
        L = Math.abs(L);

        double tanB = Z / S;
        B = Math.atan(tanB);
        double c = aAxis * aAxis / bAxis;
        double preB0 = 0.0;
        double ll = 0.0;
        double N = 0.0;
        //迭代计算纬度
        do {
            preB0 = B;
            ll = Math.pow(Math.cos(B), 2) * e2;
            N = c / Math.sqrt(1 + ll);

            tanB = (Z + N * e1 * Math.sin(B)) / S;
            B = Math.atan(tanB);
        }
        while (Math.abs(preB0 - B) >= 0.0000000001);

        ll = Math.pow(Math.cos(B), 2) * e2;
        N = c / Math.sqrt(1 + ll);

        double targetZ, targetB, targetL;
        targetZ = Z / Math.sin(B) - N * (1 - e1);
        targetB = B * 180 / Math.PI;
        targetL = L * 180 / Math.PI;

        gaussBLtoXY(targetB, targetL,targetZ);

    }

    //第四部转换，高斯变换，大地坐标系转平面坐标系，84转80
    private void gaussBLtoXY(double mX, double mY,double h) {
        double m_aAxis = Axis_xian_a; //参考椭球长半轴
        double m_bAxis = Axis_xian_b; //参考椭球短半轴
        double m_dbMidLongitude = 111;//transParaSeven.daihao*3;//中央子午线经度 济南117 威海123 巴州 87
        double m_xOffset = 500000;
        double m_yOffset = 0.0;
        try {
            //角度到弧度的系数
            double dblD2R = Math.PI / 180;
            //代表e的平方
            double e1 = (Math.pow(m_aAxis, 2) - Math.pow(m_bAxis, 2)) / Math.pow(m_aAxis, 2);
            //代表e'的平方
            double e2 = (Math.pow(m_aAxis, 2) - Math.pow(m_bAxis, 2)) / Math.pow(m_bAxis, 2);
            //a0
            double a0 = m_aAxis * (1 - e1) * (1.0 + (3.0 / 4.0) * e1 + (45.0 / 64.0) * Math.pow(e1, 2) + (175.0 / 256.0) * Math.pow(e1, 3) + (11025.0 / 16384.0) * Math.pow(e1, 4));
            //a2
            double a2 = -0.5 * m_aAxis * (1 - e1) * (3.0 / 4 * e1 + 60.0 / 64 * Math.pow(e1, 2) + 525.0 / 512.0 * Math.pow(e1, 3) + 17640.0 / 16384.0 * Math.pow(e1, 4));
            //a4
            double a4 = 0.25 * m_aAxis * (1 - e1) * (15.0 / 64 * Math.pow(e1, 2) + 210.0 / 512.0 * Math.pow(e1, 3) + 8820.0 / 16384.0 * Math.pow(e1, 4));
            //a6
            double a6 = (-1.0 / 6.0) * m_aAxis * (1 - e1) * (35.0 / 512.0 * Math.pow(e1, 3) + 2520.0 / 16384.0 * Math.pow(e1, 4));
            //a8
            double a8 = 0.125 * m_aAxis * (1 - e1) * (315.0 / 16384.0 * Math.pow(e1, 4));
            ////纬度转换为弧度表示
            //B
            double B = mX * dblD2R;
            //l
            double l = (mY - m_dbMidLongitude) * dblD2R;
            ////X
            double X = a0 * B + a2 * Math.sin(2.0 * B) + a4 * Math.sin(4.0 * B) + a6 * Math.sin(6.0 * B) + a8 * Math.sin(8.0 * B);
            //
            double ll = Math.pow(Math.cos(B), 2) * e2;
            double c = m_aAxis * m_aAxis / m_bAxis;
            //N
            double N = c / Math.sqrt(1 + ll);
            //t
            double t = Math.tan(B);
            double p = Math.cos(B) * l;
            double dby = X + N * t * (1 + ((5.0 - t * t + (9.0 + 4.0 * ll) * ll) + ((61.0 + (t * t - 58.0) * t * t + (9.0 - 11.0 * t * t) * 30.0 * ll) + (1385.0 + (-31111.0 + (543 - t * t) * t * t) * t * t) * p * p / 56.0) * p * p / 30.0) * p * p / 12.0) * p * p / 2.0;
            double dbx;
            dbx = N * (1.0 + ((1.0 - t * t + ll) + ((5.0 + t * t * (t * t - 18.0 - 58.0 * ll) + 14 * ll) + (61.0 + (-479.0 + (179.0 - t * t) * t * t) * t * t) * p * p / 42.0) * p * p / 20.0) * p * p / 6.0) * p;
            double mTargetX, mTargetY;
            mTargetX = dbx + m_xOffset;
            mTargetY = dby + m_yOffset;
            textView.setText("x:" + mTargetY + ",  y:" +mTargetX +", h"+h);
        } catch (Exception ex) {
        }
    }
}

