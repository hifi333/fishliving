package com.example.douyinscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class CustomTextDrawView extends View {

    public String showtextlist[] = {
            "时间：2024.5.31 14:21:03",
            "地点：千岛湖左口",
            "杆线：轩辕鲫5.4 小凤仙2.8g 主线3.0 ",
            "钓钩：子线1.0 40cm对折，钩距5cm 金秀3号",
            "环境：西南风3 小雨 气温26度 底水位23度",
            "钓法：钓底，空钩调5钓2",
            "标鱼：黄尾",
            "鱼窝：老G颗粒 300g 投入时间1小时前",
            "鱼获：8条，最大9两黄尾",
            "漂像：很小顶漂",
            "最近一条：2两鲫鱼40分钟前",

    };
//    private Paint paint;

    private Paint textPaint;
    private Paint backgroundPaint;
    private Rect textBounds = new Rect();


    public CustomTextDrawView(Context context) {
        super(context);
        initPaint();
    }

    private void initPaint() {
//        paint = new Paint();
//        paint.setColor(Color.RED); // 设置文字颜色为红色
//        paint.setTextSize(30); // 设置文字大小
//        paint.setAntiAlias(true); // 抗锯齿

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);

        // Initialize background paint
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setAlpha(80); // Set transparency (0-255, 0 is fully transparent, 255 is fully opaque)




    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 计算文字绘制位置（水平居中，垂直居中）
        float x = 10;
        float y = canvas.getHeight() -   canvas.getHeight()/ 3f;
//        float y = 100;
        for(int i=0; i< showtextlist.length; i++){

            String text = showtextlist[i];
            // Calculate text bounds
            textPaint.getTextBounds(text, 0, text.length(), textBounds);


            // Draw the background
            int padding = 10; // Add some padding around the text
            canvas.drawRect(x - padding, y - textBounds.height() - padding, x + textBounds.width() + padding, y + padding, backgroundPaint);

            // Draw the text over the background
            canvas.drawText(text, x, y, textPaint);

         //   canvas.drawText(showtextlist[i], x, y, paint);
            y = y+ 50;
        }


    }
}