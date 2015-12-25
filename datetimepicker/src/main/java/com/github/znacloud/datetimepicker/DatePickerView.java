package com.github.znacloud.datetimepicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.util.Calendar;

/**
 * Created by Administrator on 2015/12/14.
 */
public class DatePickerView extends View {


    private static final int COLUMNS = 7;
    private static final int ROWS = 5;
    private static final String TAG = "DatePickerView";
    private TextPaint mHeaderTextPaint;
    private int mWidth;
    private int mCellWidth;
    private int mCellHeight;
    private Rect mHeaderTxtRect;
    private int mHeight;
    private String[] mWeekTexts;
    private int mHeaderTxtVerticalMargin =(int)(6*getResources().getDisplayMetrics().density);
    private int mMonthTxtVerticalMargin =(int)(4*getResources().getDisplayMetrics().density);
    private int mHeaderHeight;
    private int mLineWidth = 1;//网格线宽度
    private Paint mBackPaint;
    private Calendar mCalendar = Calendar.getInstance();
    private int mCurYear = mCalendar.get(Calendar.YEAR);
    private int mCurMonth = mCalendar.get(Calendar.MONTH) + 1;
    private int mCurDay = mCalendar.get(Calendar.DAY_OF_MONTH);
    private int mCurWeekDay = mCalendar.get(Calendar.DAY_OF_WEEK);
    private int mFisrtDayOfWeek = mCalendar.getFirstDayOfWeek();
    private int mCurMonthDays = getMonthDays(mCurYear, mCurMonth);
    private int mLastMonthDays = getMonthDays(mCurYear,mCurMonth-1);
    private int mfirstWeekDayOfMonth;
    private int mRows = ROWS;
    private DateInfo[][] mDateMatrix;
    private TextPaint mDateTextPaint;
    private Rect mDateTxtRect;
    private Rect mCellRect;
    private int mSelectedI;//当前选中的元素的下标
    private int mSelectedJ;
    private int mLastI;//当前触摸的元素下标
    private int mLastJ;
    private OnSelectListener mOnSelectListener;

    private float mSmallTextSize = getResources().getDimensionPixelSize(R.dimen.text_size_small);
    private float mNormalTextSize = getResources().getDimensionPixelSize(R.dimen.text_size_normal);

    @Deprecated
    private float mTranslateY = 0;
    private float mLastX;
    private float mLastY;
    private int mNextMonthDays;
    private DateInfo[][] mLastDateMatrix;
    private int mLastRows;
    private int mNextRows;
    private DateInfo[][] mNextDateMatrix;
    private int mScrollY;
    private int mTouchSlop;
    private Scroller mScroller;
    private int mMonthPos = 0;


    public DatePickerView(Context context) {
        this(context, null);
    }

    public DatePickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mScroller = new Scroller(context);

        //日期文字
        mDateTextPaint = new TextPaint();
        mDateTextPaint.setColor(Color.BLACK);
        mDateTextPaint.setStyle(Paint.Style.FILL);
        mDateTextPaint.setTextSize(mNormalTextSize);
        mDateTxtRect = new Rect();
        mDateTextPaint.getTextBounds("28", 0, 2, mDateTxtRect);




        //星期的文字
        mWeekTexts = new String[]{"日","一","二","三","四","五","六"};
        mHeaderTextPaint = new TextPaint();
        mHeaderTextPaint.setTextSize(mSmallTextSize);
        mHeaderTextPaint.setStyle(Paint.Style.FILL);
        mHeaderTextPaint.setColor(Color.BLACK);
        mHeaderTxtRect = new Rect();
        mHeaderTextPaint.getTextBounds("六",0,1,mHeaderTxtRect);
        mHeaderTxtVerticalMargin =
                getResources().getDimensionPixelSize(R.dimen.text_vertical_margin);
        mHeaderHeight = mHeaderTxtRect.bottom - mHeaderTxtRect.top
                +2*mHeaderTxtVerticalMargin;

        //控件宽度固定为屏幕宽度
        mWidth = context.getResources().getDisplayMetrics().widthPixels;

        //计算网格宽高
        mBackPaint = new Paint();
        mBackPaint.setColor(Color.LTGRAY);
        mBackPaint.setStrokeWidth(mLineWidth);

        mCellWidth = (int)((mWidth+0f-mLineWidth*(COLUMNS-1))/COLUMNS);
        mCellHeight = (int)(0.8f*mCellWidth);
        mCellRect = new Rect();


        initDatas();

    }

    private void initDatas() {
        //获取月份
        mCurYear = mCalendar.get(Calendar.YEAR);
        mCurMonth = mCalendar.get(Calendar.MONTH) + 1;
        mCurDay = mCalendar.get(Calendar.DAY_OF_MONTH);
        mCurWeekDay = mCalendar.get(Calendar.DAY_OF_WEEK);
        mFisrtDayOfWeek = mCalendar.getFirstDayOfWeek();
        mCurMonthDays = getMonthDays(mCurYear, mCurMonth);
        mLastMonthDays = getMonthDays(mCurYear+((mCurMonth-1)<=0 ? -1 :0), (mCurMonth - 1)<=0 ? 12 : mCurMonth-1);
        mNextMonthDays = getMonthDays(mCurYear+((mCurMonth+1)>12 ? 1:0),(mCurMonth+1)>12 ? 1 :mCurMonth+1);
        if(mOnSelectListener != null){
            mOnSelectListener.onDateSelected(mCurYear,mCurMonth,mCurDay);
        }
        //设置到当月的1号
        mCalendar.set(mCurYear, mCurMonth - 1, 1);
        mfirstWeekDayOfMonth = mCalendar.get(Calendar.DAY_OF_WEEK);

        //计算实际行数
        mRows = (mCurMonthDays+mfirstWeekDayOfMonth +COLUMNS-1)/COLUMNS;

        //控件高度为星期文字高度加上网格高度
        mHeight = mHeaderHeight +(mLineWidth+mCellHeight)*mRows+mLineWidth;

        Log.d(TAG, "month=>" + mCurMonth + ";day=>" + mCurDay + "weekDay=>" + mCurWeekDay + ";first week Day of month=>" + mfirstWeekDayOfMonth);
        Log.d(TAG,"cur month days=>"+mCurMonthDays+";last month days =>"+mLastMonthDays);

        Log.d(TAG, "view widht=>" + mWidth + ";view height=>" + mHeight);
        Log.d(TAG, "celll widht=>" + mCellWidth + ";cell height=>" + mCellHeight);
        Log.d(TAG,"header height=>"+mHeaderHeight);
        Log.d(TAG,"text vertical margin=>"+mHeaderTxtVerticalMargin);
        Log.d(TAG,"row =>"+mRows+";col=>"+COLUMNS);

        //计算日期矩阵
        calculateDateMatrix();
    }

    private void calculateLastMonthDateMatrix(){
        int lastMonthShowDays = mfirstWeekDayOfMonth-mFisrtDayOfWeek;
        int lastMonthHiddenDays = mLastMonthDays - lastMonthShowDays;

        //上一个月行数
        mLastRows = (lastMonthHiddenDays + COLUMNS-1)/COLUMNS;
        int lastEmptyDays = mLastRows*COLUMNS - lastMonthHiddenDays;

        Log.d(TAG,"last month show day=>"+lastMonthShowDays);
        Log.d(TAG,"last month hidden day=>"+lastMonthHiddenDays);
        Log.d(TAG,"last month hidden rows=>"+mLastRows);
        Log.d(TAG, "last month empty days=>" + lastEmptyDays);

        mLastDateMatrix = new DateInfo[mLastRows][COLUMNS];
        int cellIndex = 0;
        for(int i =0;i<mLastRows;i++){
            for(int j=0;j<COLUMNS;j++){
                cellIndex++;
                DateInfo info = new DateInfo();
                info.status = DateInfo.DISABLED;
                if(cellIndex <= lastEmptyDays){
                    info.date = 0;
                }else{
                    info.date = cellIndex - lastEmptyDays;
                }
                mLastDateMatrix[i][j] = info;
            }
        }
    }

    private void calculateCurDateMatrix(){
        //上一个月要显示的天数
        int lastMonthShowDays = mfirstWeekDayOfMonth-mFisrtDayOfWeek;
        int cellIndex = 0;
        mDateMatrix = new DateInfo[mRows][COLUMNS];
        for(int i = 0;i<mRows;i++){
            for(int j =0;j<COLUMNS;j++){
                cellIndex++;
                DateInfo dateInfo = new DateInfo();
                if(cellIndex <= lastMonthShowDays){
                    //上一个月的日期
                    dateInfo.date = mLastMonthDays-lastMonthShowDays+cellIndex;
                    dateInfo.status = DateInfo.DISABLED;
                }else if(cellIndex <= mCurMonthDays+lastMonthShowDays){
                    //本月的日期

                    if(cellIndex - lastMonthShowDays == mCurDay){
                        dateInfo.status = DateInfo.SELECTED;
                        dateInfo.date = cellIndex - lastMonthShowDays;
                        mSelectedI = i;
                        mSelectedJ = j;
                    }else{
                        dateInfo.status = DateInfo.ENABLED;
                        dateInfo.date = cellIndex - lastMonthShowDays;
                    }

                }else{
                    //下个月的日期
                    dateInfo.date = cellIndex - lastMonthShowDays-mCurMonthDays;
                    dateInfo.status = DateInfo.DISABLED;
                }
                mDateMatrix[i][j] = dateInfo;
            }
        }
    }

    private void calculateNextMonthDateMartrix(){
        //下个月显示的天数
        int nextMonthShowDays = mDateMatrix[mRows-1][COLUMNS-1].date > 7 ? 0 :mDateMatrix[mRows-1][COLUMNS-1].date;
        int nextMonthHiddenDays = mNextMonthDays - nextMonthShowDays;
        //下一个月行数
        mNextRows = (nextMonthHiddenDays + COLUMNS-1)/COLUMNS;
//        int nextEmptyDays = mNextRows*COLUMNS - nextMonthHiddenDays;

        Log.d(TAG,"next month show day=>"+nextMonthShowDays);
        Log.d(TAG,"next month hidden day=>"+nextMonthHiddenDays);
        Log.d(TAG, "next month hidden rows=>" + mNextRows);
//        Log.d(TAG,"next month empty days=>"+nextEmptyDays);

        mNextDateMatrix = new DateInfo[mNextRows][COLUMNS];
        int cellIndex = 0;
        for(int i =0;i<mNextRows;i++){
            for(int j=0;j<COLUMNS;j++){
                cellIndex++;
                DateInfo info = new DateInfo();
                info.status = DateInfo.DISABLED;
                if(cellIndex <= nextMonthHiddenDays){
                    info.date = cellIndex+nextMonthShowDays;
                    Log.d(TAG,"next day=>"+info.date);
                }else{
                    info.date = 0;
                }
                mNextDateMatrix[i][j] = info;
            }
        }
    }

    private void calculateDateMatrix() {
        calculateLastMonthDateMatrix();
        calculateCurDateMatrix();
        calculateNextMonthDateMartrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();
        //不在网格区域类，直接返回
        if(y < mHeaderHeight) return  false;

        int action = event.getAction();
        Log.d(TAG,"action =>"+action);
        //计算数组下标
        int j = (int)(x/(mCellWidth+mLineWidth));
        int i = (int)((y-mHeaderHeight)/(mCellHeight+mLineWidth));
        Log.d(TAG, "i =>" + i + ";j=>" + j);


        if(action == MotionEvent.ACTION_DOWN){
            mMonthPos = 0;
            if(mScroller != null && !mScroller.isFinished()){
                mScroller.abortAnimation();
            }
            mTranslateY = 0;
            mLastX = x;
            mLastY = y;
            mScrollY = getScrollY();

            mLastI = i;
            mLastJ = j;
            if(mDateMatrix[i][j].status != DateInfo.DISABLED) {
                mDateMatrix[i][j].status = DateInfo.PRESSING;
            }
        }else if(action == MotionEvent.ACTION_MOVE){


            if(Math.abs(y-mLastY) > mTouchSlop) {
                mTranslateY = 0;//y - mLastY;

                scrollTo(0,(int)(mScrollY+mLastY-y));
            }else{
                mTranslateY = 0;
            }

            if(i != mLastI  ||j != mLastJ){
                //已经移出当前元素
                if(mDateMatrix[mLastI][mLastJ].status != DateInfo.DISABLED) {
                    if (mLastI != mSelectedI || mLastJ != mSelectedJ) {
                        mDateMatrix[mLastI][mLastJ].status = DateInfo.ENABLED;
                    } else {
                        mDateMatrix[mLastI][mLastJ].status = DateInfo.SELECTED;
                    }
                }

            }
        }else if(action == MotionEvent.ACTION_UP){
            int scrollY = getScrollY();
            Log.e(TAG,"--SCOLLR Y=>"+scrollY);
            if(scrollY > mHeight/3f){
                Log.e(TAG,"--NEXT");
                Log.e(TAG,"--dy=>"+(mHeight-scrollY));
                mMonthPos = 1;
                int scrollAmount = ((mfirstWeekDayOfMonth-mFisrtDayOfWeek +mCurMonthDays < mRows*COLUMNS ? -1 :0)+mRows)*(mCellHeight+mLineWidth);
//                int scrollAmount = mNextRows*(mCellHeight+mLineWidth);
                mScroller.startScroll(0, scrollY, 0, scrollAmount - scrollY);
//                setDate(mCurYear + (mCurMonth - 1) <= 0 ? -1 : 0, (mCurMonth - 1) <=0 ? 12 : mCurMonth-1,mCurDay);
            } else if (scrollY < -mHeight /3f){
                Log.e(TAG,"--LAST");
                Log.e(TAG, "--dy=>" + (-mHeight - scrollY));
                mMonthPos = -1;
//                int scrollAmount = ((mfirstWeekDayOfMonth-mFisrtDayOfWeek > 0 ? -1 :0)+mRows)*(mCellHeight+mLineWidth);
                int scrollAmount = mLastRows*(mCellHeight+mLineWidth);
                mScroller.startScroll(0, scrollY, 0, -scrollAmount - scrollY);
//                mTranslateY = 0;
//                setDate(mCurYear + (mCurMonth + 1) > 12 ? 1 : 0, (mCurMonth + 1) > 12 ? 1 : mCurMonth + 1, mCurDay);
            }else {
                Log.e(TAG,"--NOW");
                Log.e(TAG,"--dy=>"+(-scrollY));
                mMonthPos = 0;
                mScroller.startScroll(0, scrollY, 0, -scrollY);

                mTranslateY = 0;
                if (i == mLastI && j == mLastJ && mDateMatrix[mLastI][mLastJ].status != DateInfo.DISABLED) {
                    mDateMatrix[mSelectedI][mSelectedJ].status = DateInfo.ENABLED;
                    mDateMatrix[i][j].status = DateInfo.SELECTED;
                    mSelectedI = i;
                    mSelectedJ = j;
                    if (mOnSelectListener != null) {
                        mOnSelectListener.onDateSelected(mCurYear, mCurMonth, mDateMatrix[i][j].date);
                    }

                }
            }

        }else{
            scrollTo(0,0);
            mDateMatrix[mLastI][mLastJ].status = DateInfo.ENABLED;
            mTranslateY = 0;
        }
        invalidate();
        return true;
    }

    @Override
    public void computeScroll() {
        if(mScroller != null){

            if(mScroller.computeScrollOffset()){
                Log.d(TAG,"scroll smooth=>"+mScroller.getCurrX()+","+mScroller.getCurrY());
                scrollTo(mScroller.getCurrX(),mScroller.getCurrY());
                postInvalidate();
            }else{
                if(mMonthPos >0){
                    mMonthPos = 0;
                    setDate(mCurYear + ((mCurMonth + 1) > 12 ? 1 : 0), (mCurMonth + 1) > 12 ? 1 : mCurMonth + 1, mCurDay);
                }else if(mMonthPos < 0){
                    mMonthPos = 0;
                    setDate(mCurYear + ((mCurMonth - 1) <= 0 ? -1 : 0), (mCurMonth - 1) <=0 ? 12 : mCurMonth-1,mCurDay);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if(getScrollY() < 0){
            drawLastMonthDays(canvas);
        }else if(getScrollY() > 0){
            drawNextMonthDays(canvas);
        }

        drawCurMonthDays(canvas);

        //绘制边框
        canvas.save();
        Log.d(TAG, "getScrollY =>" + getScrollY());
        canvas.translate(0, getScrollY());
        mBackPaint.setStyle(Paint.Style.STROKE);
        mBackPaint.setStrokeWidth(1);
        mBackPaint.setColor(Color.parseColor("#cccccc"));
        canvas.drawRect(0, 0, mWidth, mHeight, mBackPaint);
        canvas.restore();
    }

    private void drawNextMonthDays(Canvas canvas) {
        float translateY = mRows * (mCellHeight+mLineWidth);
        canvas.save();
        canvas.translate(0,translateY);

        //绘制日期
        mBackPaint.setStyle(Paint.Style.FILL);

        int firstDayI =-1,firstDayJ=-1;
        float dateX,dateY;
        int cellX,cellY;
        float deltaY = 0;
        for(int i = 0;i<mNextRows;i++){
            dateY = mHeaderHeight+(mLineWidth+mCellHeight)*i +mLineWidth +(mCellHeight+mDateTxtRect.height())/2f;
            cellY = mHeaderHeight+mLineWidth+(mCellHeight+mLineWidth)*i;

            for(int j = 0;j<COLUMNS;j++){
                DateInfo dateInfo = mNextDateMatrix[i][j];
                Log.d(TAG,"draw next day=>"+dateInfo.date);
                deltaY = 0;
                if(dateInfo.date == 1){
//                    //每个月的第一天显示月份
                    deltaY = mSmallTextSize/2f +mMonthTxtVerticalMargin;
                    firstDayI = i;
                    firstDayJ = j;

                }

                mDateTextPaint.setTextSize(mNormalTextSize);

                float charWidth = mDateTextPaint.measureText(dateInfo.date+"");
                dateX = (mCellWidth-charWidth)/2f +(mCellWidth+mLineWidth)*j;

                cellX = (mCellWidth+mLineWidth)*j-mLineWidth;

                mBackPaint.setColor(Color.parseColor("#eeeeee"));
                mDateTextPaint.setColor(Color.BLACK);

                mCellRect.set(cellX, cellY, cellX + mCellWidth, cellY + mCellHeight);
                canvas.drawRect(mCellRect, mBackPaint);

                if(dateInfo.date > 0) {
                    canvas.drawText(dateInfo.date + "", dateX, dateY + deltaY, mDateTextPaint);
                }
            }

        }

        //绘制网格
        //row
        float rowLineX,rowLineY;
        mBackPaint.setColor(Color.parseColor("#cccccc"));
        mBackPaint.setStyle(Paint.Style.STROKE);
        rowLineX = 0;
        for(int i = 0;i<mNextRows;i++){
            rowLineY = mHeaderHeight+(mCellHeight+mLineWidth)*i;
            canvas.drawLine(rowLineX,rowLineY,rowLineX+mWidth,rowLineY, mBackPaint);
        }
        //columns
        float colLineX,colLineY;
        colLineY = mHeaderHeight;
        for(int i = 0;i<COLUMNS-1;i++){
            colLineX = (mCellWidth+mLineWidth)*i + mCellWidth;
            canvas.drawLine(colLineX,colLineY,colLineX,colLineY+mHeight-mHeaderHeight, mBackPaint);
        }


        int nextMonth = mCurMonth+1 > 12 ? 1:mCurMonth+1;
        mDateTextPaint.setTextSize(mSmallTextSize);
        float monthWidth = mDateTextPaint.measureText(nextMonth+"月");

        if(firstDayI >=0 && firstDayJ >=0) {
            dateX = (mCellWidth - monthWidth) / 2f + (mCellWidth + mLineWidth) * firstDayJ;
            dateY = mHeaderHeight + (mLineWidth + mCellHeight) * firstDayI
                    + mLineWidth + (mCellHeight - mMonthTxtVerticalMargin - mDateTxtRect.height() + mSmallTextSize) / 2f;
            canvas.drawText(nextMonth + "月", dateX, dateY, mDateTextPaint);
        }
//        }

        canvas.restore();
    }

    private void drawLastMonthDays(Canvas canvas) {

        float translateY = -mLastRows * (mCellHeight+mLineWidth);
        canvas.save();
        canvas.translate(0,translateY);

        //绘制日期
        mBackPaint.setStyle(Paint.Style.FILL);

        int firstDayI =0,firstDayJ=0;
        float dateX,dateY;
        int cellX,cellY;
        float deltaY = 0;
        for(int i = 0;i<mLastRows;i++){
            dateY = mHeaderHeight+(mLineWidth+mCellHeight)*i +mLineWidth +(mCellHeight+mDateTxtRect.height())/2f;
            cellY = mHeaderHeight+mLineWidth+(mCellHeight+mLineWidth)*i;

            for(int j = 0;j<COLUMNS;j++){
                DateInfo dateInfo = mLastDateMatrix[i][j];
                deltaY = 0;
                if(dateInfo.date == 1){
//                    //每个月的第一天显示月份
                    deltaY = mSmallTextSize/2f +mMonthTxtVerticalMargin;
                    firstDayI = i;
                    firstDayJ = j;

                }

                mDateTextPaint.setTextSize(mNormalTextSize);

                float charWidth = mDateTextPaint.measureText(dateInfo.date+"");
                dateX = (mCellWidth-charWidth)/2f +(mCellWidth+mLineWidth)*j;

                cellX = (mCellWidth+mLineWidth)*j-mLineWidth;



                    mBackPaint.setColor(Color.parseColor("#eeeeee"));
                    mDateTextPaint.setColor(Color.BLACK);




                mCellRect.set(cellX,cellY,cellX+mCellWidth,cellY+mCellHeight);
                canvas.drawRect(mCellRect,mBackPaint);

                if(dateInfo.date > 0) {//0 代表此处没有显示日期
                    canvas.drawText(dateInfo.date + "", dateX, dateY + deltaY, mDateTextPaint);
                }
            }

        }

        //绘制网格
        //row
        float rowLineX,rowLineY;
        mBackPaint.setColor(Color.parseColor("#cccccc"));
        mBackPaint.setStyle(Paint.Style.STROKE);
        rowLineX = 0;
        for(int i = 0;i<mLastRows;i++){
            rowLineY = mHeaderHeight+(mCellHeight+mLineWidth)*i;
            canvas.drawLine(rowLineX,rowLineY,rowLineX+mWidth,rowLineY, mBackPaint);
        }
        //columns
        float colLineX,colLineY;
        colLineY = mHeaderHeight;
        for(int i = 0;i<COLUMNS-1;i++){
            colLineX = (mCellWidth+mLineWidth)*i + mCellWidth;
            canvas.drawLine(colLineX,colLineY,colLineX,colLineY+mHeight-mHeaderHeight, mBackPaint);
        }


        int lastMonth = mCurMonth-1 <= 0 ? 12:mCurMonth-1;
        mDateTextPaint.setTextSize(mSmallTextSize);
        float monthWidth = mDateTextPaint.measureText(lastMonth+"月");

            dateX = (mCellWidth-monthWidth)/2f +(mCellWidth+mLineWidth)*firstDayJ;
            dateY = mHeaderHeight+(mLineWidth+mCellHeight)*firstDayI
                    +mLineWidth +(mCellHeight-mMonthTxtVerticalMargin-mDateTxtRect.height() + mSmallTextSize) / 2f;
        canvas.drawText(lastMonth + "月", dateX, dateY, mDateTextPaint);
//        }

        canvas.restore();
    }

    private void drawCurMonthDays(Canvas canvas){
        canvas.save();
        Log.d(TAG, "translateY =>" + mTranslateY);
        canvas.translate(0, mTranslateY);

        //绘制日期
        mBackPaint.setStyle(Paint.Style.FILL);

        int firstDayI =0,firstDayJ=0;
        float dateX,dateY;
        int cellX,cellY;
        float deltaY = 0;
        for(int i = 0;i<mRows;i++){
            dateY = mHeaderHeight+(mLineWidth+mCellHeight)*i +mLineWidth +(mCellHeight+mDateTxtRect.height())/2f;
            cellY = mHeaderHeight+mLineWidth+(mCellHeight+mLineWidth)*i;

            for(int j = 0;j<COLUMNS;j++){
                DateInfo dateInfo = mDateMatrix[i][j];
                deltaY = 0;
                if(dateInfo.date == 1 && dateInfo.status != DateInfo.DISABLED){
//                    //每个月的第一天显示月份,并且当天不是选中状态
                    deltaY = mSmallTextSize/2f +mMonthTxtVerticalMargin;
                    firstDayI = i;
                    firstDayJ = j;

                }

                mDateTextPaint.setTextSize(mNormalTextSize);



                float charWidth = mDateTextPaint.measureText(dateInfo.date+"");
                dateX = (mCellWidth-charWidth)/2f +(mCellWidth+mLineWidth)*j;

                cellX = (mCellWidth+mLineWidth)*j-mLineWidth;


                if(dateInfo.status == DateInfo.DISABLED){
                    mBackPaint.setColor(Color.parseColor("#eeeeee"));
                    mDateTextPaint.setColor(Color.BLACK);

                }else if(dateInfo.status == DateInfo.SELECTED){
                    mBackPaint.setColor(Color.parseColor("#2C9CFE"));
                    mDateTextPaint.setColor(Color.WHITE);
                    deltaY = mSmallTextSize/2f +mMonthTxtVerticalMargin;
                }else if(dateInfo.status == DateInfo.PRESSING) {
                    mBackPaint.setColor(Color.parseColor("#026ECC"));
                    mDateTextPaint.setColor(Color.WHITE);
                    if(mSelectedI == i && mSelectedJ==j){
                        deltaY = mSmallTextSize/2f +mMonthTxtVerticalMargin;
                    }
                }else{
                    mBackPaint.setColor(Color.WHITE);
                    mDateTextPaint.setColor(Color.BLACK);
                }


                mCellRect.set(cellX,cellY,cellX+mCellWidth,cellY+mCellHeight);
                canvas.drawRect(mCellRect,mBackPaint);

                canvas.drawText(dateInfo.date+"",dateX,dateY+deltaY,mDateTextPaint);
            }

        }


        //绘制网格
        //row
        float rowLineX,rowLineY;
        mBackPaint.setColor(Color.parseColor("#cccccc"));
        mBackPaint.setStyle(Paint.Style.STROKE);
        rowLineX = 0;
        for(int i = 0;i<mRows+1;i++){
            rowLineY = mHeaderHeight+(mCellHeight+mLineWidth)*i;
            canvas.drawLine(rowLineX,rowLineY,rowLineX+mWidth,rowLineY, mBackPaint);
        }
        //columns
        float colLineX,colLineY;
        colLineY = mHeaderHeight;
        for(int i = 0;i<COLUMNS-1;i++){
            colLineX = (mCellWidth+mLineWidth)*i + mCellWidth;
            canvas.drawLine(colLineX,colLineY,colLineX,colLineY+mHeight-mHeaderHeight, mBackPaint);
        }

        //绘制选中的月份
        mDateTextPaint.setTextSize(mSmallTextSize);
        mDateTextPaint.setColor(Color.WHITE);
        float monthWidth = mDateTextPaint.measureText(mCurMonth+"月");
        dateX = (mCellWidth-monthWidth)/2f +(mCellWidth+mLineWidth)*mSelectedJ;
        dateY = mHeaderHeight+(mLineWidth+mCellHeight)*mSelectedI
                +mLineWidth +(mCellHeight-mMonthTxtVerticalMargin-mDateTxtRect.height()+mSmallTextSize)/2f;
        canvas.drawText(mCurMonth+"月",dateX,dateY,mDateTextPaint);

        //绘制首日月份
        if(mDateMatrix[firstDayI][firstDayJ].status != DateInfo.SELECTED){
            mDateTextPaint.setTextSize(mSmallTextSize);
            if(mDateMatrix[firstDayI][firstDayJ].status != DateInfo.PRESSING) {
                mDateTextPaint.setColor(Color.GRAY);
            }
            dateX = (mCellWidth-monthWidth)/2f +(mCellWidth+mLineWidth)*firstDayJ;
            dateY = mHeaderHeight+(mLineWidth+mCellHeight)*firstDayI
                    +mLineWidth +(mCellHeight-mMonthTxtVerticalMargin-mDateTxtRect.height()+mSmallTextSize)/2f;
            canvas.drawText(mCurMonth + "月", dateX, dateY, mDateTextPaint);
        }
        canvas.restore();

        canvas.save();
        Log.d(TAG,"getScrollY =>"+getScrollY());
        canvas.translate(0, getScrollY());

        //绘制星期文字
        float textX,textY;//坐标

        mBackPaint.setStyle(Paint.Style.FILL);
        mBackPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, mWidth, mHeaderHeight, mBackPaint);
        textY = mHeaderTxtVerticalMargin+mHeaderTxtRect.height();
        for(int i = 0;i<COLUMNS;i++){
            //根据星期几为每周的第一天来计算第一个要显示的星期的位置
            int textIndex = (i+mFisrtDayOfWeek-1)%mWeekTexts.length;

            if(mWeekTexts[textIndex].contains("日")
                    || mWeekTexts[textIndex].contains("六")){
                mHeaderTextPaint.setColor(Color.parseColor("#ff0000"));
            }else{
                mHeaderTextPaint.setColor(Color.BLACK);
            }

            float charWidth = mHeaderTextPaint.measureText(mWeekTexts[textIndex]);
            textX = (mCellWidth-charWidth)/2f +(mCellWidth+mLineWidth)*i;

            canvas.drawText(mWeekTexts[(i+mFisrtDayOfWeek-1)%mWeekTexts.length],textX,textY,mHeaderTextPaint);
        }
        canvas.restore();

    }

    private int getMonthDays(int year,int month) {
        int days = 0;
        switch (month){
            case 1:case 3:case 5:case 7:case 8:case 10:case 12: days =  31;break;
            case 2: days = 28 + (isLeapYear(year)?1:0);break;
            case 4:case 6:case 9:case 11: days = 30;break;
            default:break;
        }
        return days;
    }

    private boolean isLeapYear(int year) {
        return (year%4==0)&&(year%100!=0)||(year%400==0);
    }


    /**
     * 设置默认选中的日期
     * @param year 年份
     * @param month 月份
     * @param day 日期
     */
    public void setDate(int year,int month,int day){
        if(month < 1){
            month =1;
        }else if(month > 12){
            month = 12;
        }

        if(year < 1970){
            year = 1970;
        }

        int maxDays = getMonthDays(year,month);
        if(day < 1){
            day = 1;
        }else if(day > getMonthDays(year,month)){
            day = maxDays;
        }

        mCalendar.set(year,month-1,day);
        initDatas();
        requestLayout();
        scrollTo(0, 0);
        invalidate();
    }

    /**
     * 返回选中的日期，
     * int[0]=>year
     * int[1]=>month
     * int[2]=>day
     * @return
     */
    public int[] getSelectedDate(){
        return new int[]{mCurYear,mCurMonth,mDateMatrix[mSelectedI][mSelectedJ].date};
    }

    /**
     * 设置回调函数
     * @param pListener
     */
    public void setOnSelectListener(OnSelectListener pListener){
        mOnSelectListener = pListener;
    }

    public interface OnSelectListener{
        /**
         * 当日期被选中时将会回调此方法
         * @param year
         * @param month
         * @param day
         */
         void onDateSelected(int year,int month,int day);
    }

}
