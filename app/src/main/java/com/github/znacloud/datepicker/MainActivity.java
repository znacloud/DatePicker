package com.github.znacloud.datepicker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextPaint;
import android.widget.TextView;

import com.github.znacloud.datetimepicker.DatePickerView;

public class MainActivity extends AppCompatActivity {

    private TextView mDateTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatePickerView pickerView = (DatePickerView)findViewById(R.id.dpv_sample);
        mDateTv = (TextView)findViewById(R.id.tv_date);
        pickerView.setOnSelectListener(new DatePickerView.OnSelectListener() {
            @Override
            public void onDateSelected(int year, int month, int day) {
                mDateTv.setText(year+"-"+month+"-"+day);
            }
        });
//        pickerView.setDate(2015,11,21);
    }
}
