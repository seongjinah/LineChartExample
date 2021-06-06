package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String BASE_URL = "https://wft-geo-db.p.rapidapi.com/";
    private YahooFinanceAPI mYahooFinanceAPI;

    private LineChart mLineChart;
    private TextInputLayout mStockTickerTextInputLayout;
    private RadioGroup mPeriodRadioGroup, mIntervalRadioGroup;
    private CheckBox mHighCheckBox, mLowCheckBox, mCloseCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLineChart = (LineChart)findViewById(R.id.activity_main_linechart);

        mStockTickerTextInputLayout = (TextInputLayout)findViewById(R.id.activity_main_stockticker);
        mPeriodRadioGroup = (RadioGroup)findViewById(R.id.activity_main_period_radiogroup);
        mIntervalRadioGroup = (RadioGroup)findViewById(R.id.activity_main_priceinterval);

        mHighCheckBox = (CheckBox)findViewById(R.id.activity_main_high);
        mLowCheckBox = (CheckBox)findViewById(R.id.activity_main_low);
        mCloseCheckBox = (CheckBox)findViewById(R.id.activity_main_close);

        configureLineChart();
        setupApi();
        findViewById(R.id.activity_main_getprices).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStockData();
            }
        });
    }

    private void getStockData() {
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = 0;
        switch (mPeriodRadioGroup.getCheckedRadioButtonId()){
            case R.id.activity_main_period1d:
                startTime = endTime - (60 * 60 * 24);
                break;
            case R.id.activity_main_period30d:
                startTime = endTime - (60 * 60 * 24 * 30);
                break;
            case R.id.activity_main_period90d:
                startTime = endTime - (60 * 60 * 24 * 90);
                break;
            case R.id.activity_main_period12m:
                startTime = endTime - (60 * 60 * 24 * 365);
                break;
        }

        String frequency = "";
        switch (mIntervalRadioGroup.getCheckedRadioButtonId()) {
            case R.id.activity_main_interval1d:
                frequency = "1d";
                break;
            case R.id.activity_main_interval1w:
                frequency = "1w";
                break;
            case R.id.activity_main_interval1m:
                frequency = "1m";
                break;
        }

        mYahooFinanceAPI.getHistoricalData(
                frequency,
                "history",
                String.valueOf(startTime),
                String.valueOf(endTime),
                mStockTickerTextInputLayout.getEditText().getText().toString()
        ).enqueue(new Callback<HistoricalDataResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<HistoricalDataResponse> call, Response<HistoricalDataResponse> response) {
                ArrayList<Entry> pricesHigh = new ArrayList<>();
                ArrayList<Entry> pricesLow = new ArrayList<>();
                ArrayList<Entry> pricesClose = new ArrayList<>();

                if (response.body() != null) {
                    for (int i = 0; i < response.body().prices.size(); i++) {
                        float x = response.body().prices.get(i).date;
                        float y = response.body().prices.get(i).high;
                        if (y != 0f) {
                            pricesHigh.add(new Entry(x, response.body().prices.get(i).high));
                            pricesLow.add(new Entry(x, response.body().prices.get(i).low));
                            pricesClose.add(new Entry(x, response.body().prices.get(i).close));
                        }
                    }
                    Comparator<Entry> comparator = new Comparator<Entry>() {
                        @Override
                        public int compare(Entry o1, Entry o2) {
                            return Float.compare(o1.getX(), o2.getX());
                        }
                    };

                    pricesHigh.sort(comparator);
                    pricesLow.sort(comparator);
                    pricesClose.sort(comparator);

                    setLineChartData(pricesHigh, pricesLow, pricesClose);
                }
            }

            @Override
            public void onFailure(Call<HistoricalDataResponse> call, Throwable t) {

            }
        });
    }

    private void setLineChartData(ArrayList<Entry> pricesHigh, ArrayList<Entry> pricesLow, ArrayList<Entry> pricesClose) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        if(mHighCheckBox.isChecked()){
            LineDataSet highLineDataSet = new LineDataSet(pricesHigh, mStockTickerTextInputLayout.getEditText().getText().toString() + " Price (Hight)");
            highLineDataSet.setDrawCircles(true);
            highLineDataSet.setCircleRadius(4);
            highLineDataSet.setDrawValues(false);
            highLineDataSet.setLineWidth(3);
            highLineDataSet.setColor(Color.GREEN);
            highLineDataSet.setCircleColor(Color.GREEN);
            dataSets.add(highLineDataSet);
        }

        if(mLowCheckBox.isChecked()){
            LineDataSet lowLineDataSet = new LineDataSet(pricesLow, mStockTickerTextInputLayout.getEditText().getText().toString() + " Price (Low)");
            lowLineDataSet.setDrawCircles(true);
            lowLineDataSet.setCircleRadius(4);
            lowLineDataSet.setDrawValues(false);
            lowLineDataSet.setLineWidth(3);
            lowLineDataSet.setColor(Color.RED);
            lowLineDataSet.setCircleColor(Color.RED);
            dataSets.add(lowLineDataSet);
        }

        if(mCloseCheckBox.isChecked()){
            LineDataSet closeLineDataSet = new LineDataSet(pricesClose, mStockTickerTextInputLayout.getEditText().getText().toString() + " Price (Low)");
            closeLineDataSet.setDrawCircles(true);
            closeLineDataSet.setCircleRadius(4);
            closeLineDataSet.setDrawValues(false);
            closeLineDataSet.setLineWidth(3);
            closeLineDataSet.setColor(Color.rgb(255, 165, 0));
            closeLineDataSet.setCircleColor(Color.rgb(255, 165, 0));
            dataSets.add(closeLineDataSet);
        }

        LineData lineData = new LineData(dataSets);
        mLineChart.setData(lineData);
        mLineChart.invalidate();
    }

    private void setupApi() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        mYahooFinanceAPI = new retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YahooFinanceAPI.class);
    }

    private void configureLineChart() {
        Description desc = new Description();
        desc.setText("Stock Price History");
        desc.setTextSize(28);
        mLineChart.setDescription(desc);

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
            @Override
            public String getFormattedValue(float value) {
                long millis = (long) value * 1000L;
                return mFormat.format(new Date(millis));
            }
        });
    }
}