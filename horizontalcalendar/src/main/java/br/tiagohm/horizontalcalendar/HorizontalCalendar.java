package br.tiagohm.horizontalcalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;


/**
 * See {@link HorizontalCalendarView HorizontalCalendarView}
 *
 * @author Mulham-Raee
 * @version 1.1
 * @see HorizontalCalendarListener
 */
public final class HorizontalCalendar {

    final RecyclerView.OnScrollListener onScrollListener = new HorizontalCalendarScrollListener();
    private final DateHandler handler;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private final Calendar dateCalendar = GregorianCalendar.getInstance();
    //RootView
    private final View rootView;
    private final int calendarId;
    //Number of Dates to Show on Screen
    private final int numberOfDatesOnScreen;
    private final String formatDayName;
    private final String formatDayNumber;
    private final String formatMonth;
    private final String formatYear;
    private final boolean showMonthName;
    private final boolean showDayName;
    private final boolean showYearAndMonth;
    //region private Fields
    HorizontalCalendarView calendarView;
    HorizontalCalendarAdapter mCalendarAdapter;
    ArrayList<Date> mListDays;
    boolean loading;
    //Interface events
    HorizontalCalendarListener calendarListener;
    //Start & End Dates
    private Date dateStartCalendar;
    private Date dateEndCalendar;
    /* Format, Colors & Font Sizes*/
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dateYearFormat;
    private int textColorNormal, textColorSelected;
    private Drawable selectedDateBackground;
    private Integer selectorColor;
    private float textSizeMonthName, textSizeDayNumber, textSizeDayName;
    //endregion

    /**
     * Private Constructor to insure HorizontalCalendar can't be initiated the default way
     */
    HorizontalCalendar(Builder builder) {
        this.rootView = builder.rootView;
        this.calendarId = builder.viewId;
        this.textColorNormal = builder.textColorNormal;
        this.textColorSelected = builder.textColorSelected;
        this.selectedDateBackground = builder.selectedDateBackground;
        this.selectorColor = builder.selectorColor;
        this.formatDayName = builder.formatDayName;
        this.formatDayNumber = builder.formatDayNumber;
        this.formatMonth = builder.formatMonth;
        this.formatYear = builder.formatYear;
        this.textSizeMonthName = builder.textSizeMonthName;
        this.textSizeDayNumber = builder.textSizeDayNumber;
        this.textSizeDayName = builder.textSizeDayName;
        this.numberOfDatesOnScreen = builder.numberOfDatesOnScreen;
        this.dateStartCalendar = builder.dateStartCalendar;
        this.dateEndCalendar = builder.dateEndCalendar;
        this.showDayName = builder.showDayName;
        this.showMonthName = builder.showMonthName;
        this.showYearAndMonth = builder.showYearAndMonth;

        handler = new DateHandler(this, builder.defaultSelectedDate);
    }

    /* Init Calendar View */
    void loadHorizontalCalendar() {

        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        mListDays = new ArrayList<>();
        calendarView = (HorizontalCalendarView) rootView.findViewById(calendarId);
        calendarView.setHasFixedSize(true);
        calendarView.setHorizontalScrollBarEnabled(false);
        calendarView.setHorizontalCalendar(this);

        HorizontalSnapHelper snapHelper = new HorizontalSnapHelper();
        snapHelper.attachToHorizontalCalendaar(this);

        hide();
        new InitializeDatesList().execute();
    }

    public HorizontalCalendarListener getCalendarListener() {
        return calendarListener;
    }

    public void setCalendarListener(HorizontalCalendarListener calendarListener) {
        this.calendarListener = calendarListener;
    }

    /**
     * Select today date and center the Horizontal Calendar to this date
     *
     * @param immediate pass true to make the calendar scroll as fast as possible to reach the date of today
     *                  ,or false to play default scroll animation speed.
     */
    public void goToday(boolean immediate) {
        selectDate(new Date(), immediate);
    }

    /**
     * Select the date and center the Horizontal Calendar to this date
     *
     * @param date      The date to select
     * @param immediate pass true to make the calendar scroll as fast as possible to reach the target date
     *                  ,or false to play default scroll animation speed.
     */
    public void selectDate(Date date, boolean immediate) {
        if (loading) {
            handler.date = date;
            handler.immediate = immediate;
        } else {
            if (immediate) {
                int datePosition = positionOfDate(date);
                centerToPositionWithNoAnimation(datePosition);
                if (calendarListener != null) {
                    calendarListener.onDateSelected(date, datePosition);
                }
            } else {
                calendarView.setSmoothScrollSpeed(HorizontalLayoutManager.SPEED_NORMAL);
                centerCalendarToPosition(positionOfDate(date));
            }
        }
    }

    /**
     * Center the Horizontal Calendar to this position and select the day on this position
     *
     * @param position The position to center the calendar to!
     */
    void centerCalendarToPosition(int position) {
        if (position != -1) {
            int shiftCells = numberOfDatesOnScreen / 2;
            int centerItem = calendarView.getPositionOfCenterItem();

            if (position > centerItem) {
                calendarView.smoothScrollToPosition(position + shiftCells);
            } else if (position < centerItem) {
                calendarView.smoothScrollToPosition(position - shiftCells);
            }
        }
    }

    private void centerToPositionWithNoAnimation(final int position) {
        if (position != -1) {
            int shiftCells = numberOfDatesOnScreen / 2;
            int centerItem = calendarView.getPositionOfCenterItem();

            if (position > centerItem) {
                calendarView.scrollToPosition(position + shiftCells);
            } else if (position < centerItem) {
                calendarView.scrollToPosition(position - shiftCells);
            }

            calendarView.post(new Runnable() {
                @Override
                public void run() {
                    mCalendarAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void show() {
        calendarView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        calendarView.setVisibility(View.INVISIBLE);
    }

    public void post(Runnable runnable) {
        calendarView.post(runnable);
    }

    @TargetApi(21)
    public void setElevation(float elevation) {
        calendarView.setElevation(elevation);
    }

    /**
     * @return the current selected date
     */
    public Date getSelectedDate() {
        return mListDays.get(calendarView.getPositionOfCenterItem());
    }

    /**
     * @return position of selected date in Horizontal Calendar
     */
    public int getSelectedDatePosition() {
        return calendarView.getPositionOfCenterItem();
    }

    /**
     * @param position The position of date
     * @return the date on this index
     * @throws IndexOutOfBoundsException
     */
    public Date getDateAt(int position) throws IndexOutOfBoundsException {
        return mCalendarAdapter.getItem(position);
    }

    /**
     * @param date The date to search for
     * @return true if the calendar contains this date or false otherwise
     */
    public boolean contains(Date date) {
        return mListDays.contains(date);
    }

    //region Getters & Setters
    public Date getDateStartCalendar() {
        return dateStartCalendar;
    }

    public Date getDateEndCalendar() {
        return dateEndCalendar;
    }

    public String getFormatDayName() {
        return formatDayName;
    }

    public String getFormatDayNumber() {
        return formatDayNumber;
    }

    public String getFormatYear() {
        return formatYear;
    }

    public String getFormatMonth() {
        return formatMonth;
    }

    public boolean isShowDayName() {
        return showDayName;
    }

    public boolean isShowMonthName() {
        return showMonthName;
    }

    public boolean isShowYearAndMonth() {
        return showYearAndMonth;
    }

    public int getNumberOfDatesOnScreen() {
        return numberOfDatesOnScreen;
    }

    public Drawable getSelectedDateBackground() {
        return selectedDateBackground;
    }

    public void setSelectedDateBackground(Drawable selectedDateBackground) {
        this.selectedDateBackground = selectedDateBackground;
    }

    public int getTextColorNormal() {
        return textColorNormal;
    }

    public void setTextColorNormal(int textColorNormal) {
        this.textColorNormal = textColorNormal;
    }

    public int getTextColorSelected() {
        return textColorSelected;
    }

    public void setTextColorSelected(int textColorSelected) {
        this.textColorSelected = textColorSelected;
    }

    public Integer getSelectorColor() {
        return selectorColor;
    }

    public void setSelectorColor(int selectorColor) {
        this.selectorColor = selectorColor;
    }

    public float getTextSizeMonthName() {
        return textSizeMonthName;
    }

    public void setTextSizeMonthName(float textSizeMonthName) {
        this.textSizeMonthName = textSizeMonthName;
    }

    public float getTextSizeDayNumber() {
        return textSizeDayNumber;
    }

    public void setTextSizeDayNumber(float textSizeDayNumber) {
        this.textSizeDayNumber = textSizeDayNumber;
    }

    public float getTextSizeDayName() {
        return textSizeDayName;
    }

    public void setTextSizeDayName(float textSizeDayName) {
        this.textSizeDayName = textSizeDayName;
    }
    //endregion

    /**
     * @return position of date in Calendar, or -1 if date does not exist
     */
    public int positionOfDate(Date date) {
        dateCalendar.setTime(date);
        startCalendar.setTime(dateStartCalendar);
        endCalendar.setTime(dateEndCalendar);

        if (isShowYearAndMonth()) {
            startCalendar.set(Calendar.DATE, 1);
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            endCalendar.set(Calendar.DATE, endCalendar.getActualMaximum(Calendar.DATE));
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
        }
        //Está fora do intervalo.
        if (date.after(endCalendar.getTime()) || date.before(startCalendar.getTime())) {
            return -1;
        }
        //Inicio.
        else if (isDatesDaysEquals(date, dateStartCalendar)) {
            return 0;
        }
        //Fim.
        else if (isDatesDaysEquals(date, dateEndCalendar)) {
            return mListDays.size() - 1;
        }
        //Calcular posicao para mes e ano.
        else if (isShowYearAndMonth()) {
            int diffYear = dateCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
            int diffMonth = diffYear * 12 + dateCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
            int position = diffMonth + 2;
            return position;
        }
        //Calcular posicao para dia e mes.
        else {
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            dateCalendar.set(Calendar.HOUR_OF_DAY, 23);
            dateCalendar.set(Calendar.MINUTE, 59);
            dateCalendar.set(Calendar.SECOND, 59);

            long diff = dateCalendar.getTimeInMillis() - startCalendar.getTimeInMillis(); //result in millis
            long days = (diff / (24 * 60 * 60 * 1000));

            int position = (int) days + 2;

            return position;
        }
    }

    /**
     * @return true if dates are equal
     */
    public boolean isDatesDaysEquals(Date date1, Date date2) {
        return isShowYearAndMonth() ? dateYearFormat.format(date1).equals(dateYearFormat.format(date2)) :
                dateFormat.format(date1).equals(dateFormat.format(date2));
    }

    public static class Builder {

        final int viewId;
        final View rootView;

        //Start & End Dates
        Date dateStartCalendar;
        Date dateEndCalendar;

        //Number of Days to Show on Screen
        int numberOfDatesOnScreen;

        /* Format, Colors & Font Sizes*/
        String formatDayName;
        String formatDayNumber;
        String formatMonth;
        String formatYear;
        int textColorNormal, textColorSelected;
        Drawable selectedDateBackground;
        Integer selectorColor;
        float textSizeMonthName, textSizeDayNumber, textSizeDayName;

        boolean showMonthName = true;
        boolean showDayName = true;
        boolean showYearAndMonth = false;
        Date defaultSelectedDate;

        /**
         * @param rootView pass the rootView for the Fragment where HorizontalCalendar is attached
         * @param viewId   the id specified for HorizontalCalendarView in your layout
         */
        public Builder(View rootView, int viewId) {
            this.rootView = rootView;
            this.viewId = viewId;
        }

        /**
         * @param activity pass the activity where HorizontalCalendar is attached
         * @param viewId   the id specified for HorizontalCalendarView in your layout
         */
        public Builder(Activity activity, int viewId) {
            this.rootView = activity.getWindow().getDecorView();
            this.viewId = viewId;
        }

        public Builder defaultSelectedDate(Date date) {
            defaultSelectedDate = date;
            return this;
        }

        public Builder startDate(Date dateStartCalendar) {
            this.dateStartCalendar = dateStartCalendar;
            return this;
        }

        public Builder endDate(Date dateEndCalendar) {
            this.dateEndCalendar = dateEndCalendar;
            return this;
        }

        public Builder datesNumberOnScreen(int numberOfItemsOnScreen) {
            this.numberOfDatesOnScreen = numberOfItemsOnScreen;
            return this;
        }

        public Builder dayNameFormat(String format) {
            this.formatDayName = format;
            return this;
        }

        public Builder dayNumberFormat(String format) {
            this.formatDayNumber = format;
            return this;
        }

        public Builder monthFormat(String format) {
            this.formatMonth = format;
            return this;
        }

        public Builder yearFormat(String format) {
            this.formatYear = format;
            return this;
        }

        public Builder textColor(int textColorNormal, int textColorSelected) {
            this.textColorNormal = textColorNormal;
            this.textColorSelected = textColorSelected;
            return this;
        }

        public Builder selectedDateBackground(Drawable background) {
            this.selectedDateBackground = background;
            return this;
        }

        public Builder selectorColor(int selectorColor) {
            this.selectorColor = selectorColor;
            return this;
        }

        /**
         * Set the text size of the labels in scale-independent pixels
         *
         * @param textSizeMonthName the month name text size, in SP
         * @param textSizeDayNumber the day number text size, in SP
         * @param textSizeDayName   the day name text size, in SP
         */
        public Builder textSize(float textSizeMonthName, float textSizeDayNumber,
                                float textSizeDayName) {
            this.textSizeMonthName = textSizeMonthName;
            this.textSizeDayNumber = textSizeDayNumber;
            this.textSizeDayName = textSizeDayName;
            return this;
        }

        /**
         * Set the text size of the month name label in scale-independent pixels
         *
         * @param textSizeMonthName the month name text size, in SP
         */
        public Builder textSizeMonthName(float textSizeMonthName) {
            this.textSizeMonthName = textSizeMonthName;
            return this;
        }

        /**
         * Set the text size of the day number label in scale-independent pixels
         *
         * @param textSizeDayNumber the day number text size, in SP
         */
        public Builder textSizeDayNumber(float textSizeDayNumber) {
            this.textSizeDayNumber = textSizeDayNumber;
            return this;
        }

        /**
         * Set the text size of the day name label in scale-independent pixels
         *
         * @param textSizeDayName the day name text size, in SP
         */
        public Builder textSizeDayName(float textSizeDayName) {
            this.textSizeDayName = textSizeDayName;
            return this;
        }

        public Builder showDayName(boolean value) {
            showDayName = value;
            return this;
        }

        public Builder showMonthName(boolean value) {
            showMonthName = value;
            return this;
        }

        public Builder showYearAndMonth(boolean value) {
            showYearAndMonth = value;
            return this;
        }

        /**
         * @return Instance of {@link HorizontalCalendar} initiated with builder settings
         */
        public HorizontalCalendar build() {
            initDefaultValues();
            HorizontalCalendar horizontalCalendar = new HorizontalCalendar(this);
            horizontalCalendar.loadHorizontalCalendar();
            return horizontalCalendar;
        }

        private void initDefaultValues() {
            /* Defaults variables */
            if (numberOfDatesOnScreen <= 0) {
                numberOfDatesOnScreen = 5;
            }

            if (formatDayName == null && showDayName) {
                formatDayName = "EEE";
            }
            if (formatDayNumber == null) {
                formatDayNumber = "dd";
            }
            if (formatMonth == null && (showMonthName || showYearAndMonth)) {
                formatMonth = "MMM";
            }
            if (formatYear == null && showYearAndMonth) {
                formatYear = "yyyy";
            }
            if (dateStartCalendar == null) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MONTH, -1);
                dateStartCalendar = c.getTime();
            }
            if (dateEndCalendar == null) {
                Calendar c2 = Calendar.getInstance();
                c2.add(Calendar.MONTH, 1);
                dateEndCalendar = c2.getTime();
            }
            if (defaultSelectedDate == null) {
                defaultSelectedDate = new Date();
            }
        }
    }

    private static class DateHandler extends Handler {

        private final WeakReference<HorizontalCalendar> horizontalCalendar;
        public Date date = null;
        public boolean immediate = true;

        public DateHandler(HorizontalCalendar horizontalCalendar, Date defaultDate) {
            this.horizontalCalendar = new WeakReference<>(horizontalCalendar);
            this.date = defaultDate;
        }

        @Override
        public void handleMessage(Message msg) {
            HorizontalCalendar calendar = horizontalCalendar.get();
            if (calendar != null) {
                calendar.loading = false;
                if (date != null) {
                    calendar.selectDate(date, immediate);
                }

            }
        }
    }

    private class InitializeDatesList extends AsyncTask<Void, Void, Void> {

        InitializeDatesList() {
        }

        @Override
        protected void onPreExecute() {
            loading = true;
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //ArrayList of dates is set with all the dates between
            //start and end date
            GregorianCalendar calendar = new GregorianCalendar();

            calendar.setTime(dateStartCalendar);
            if (isShowYearAndMonth()) {
                calendar.set(Calendar.DATE, 1);
                calendar.add(Calendar.MONTH, -(numberOfDatesOnScreen / 2));
            } else {
                calendar.add(Calendar.DATE, -(numberOfDatesOnScreen / 2));
            }
            Date dateStartBefore = calendar.getTime();
            calendar.setTime(dateEndCalendar);
            if (isShowYearAndMonth()) {
                calendar.set(Calendar.DATE, 1);
                calendar.add(Calendar.MONTH, numberOfDatesOnScreen / 2);
            } else {
                calendar.add(Calendar.DATE, numberOfDatesOnScreen / 2);
            }
            Date dateEndAfter = calendar.getTime();

            Date date = dateStartBefore;
            while (!date.after(dateEndAfter)) {
                mListDays.add(date);
                calendar.setTime(date);
                if (isShowYearAndMonth()) {
                    calendar.add(Calendar.MONTH, 1);
                } else {
                    calendar.add(Calendar.DATE, 1);
                }
                date = calendar.getTime();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mCalendarAdapter = new HorizontalCalendarAdapter(calendarView, mListDays);
            mCalendarAdapter.setHasStableIds(true);
            calendarView.setAdapter(mCalendarAdapter);
            calendarView.setLayoutManager(new HorizontalLayoutManager(calendarView.getContext(), false));

            show();
            handler.sendMessage(new Message());
            calendarView.addOnScrollListener(onScrollListener);
        }
    }

    private class HorizontalCalendarScrollListener extends RecyclerView.OnScrollListener {

        final Runnable selectedItemRefresher = new SelectedItemRefresher();
        int lastSelectedItem = -1;

        HorizontalCalendarScrollListener() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //On Scroll, agenda is refresh to update background colors
            post(selectedItemRefresher);

            if (calendarListener != null) {
                calendarListener.onCalendarScroll(calendarView, dx, dy);
            }
        }

        private class SelectedItemRefresher implements Runnable {

            SelectedItemRefresher() {
            }

            @Override
            public void run() {
                final int positionOfCenterItem = calendarView.getPositionOfCenterItem();
                if ((lastSelectedItem == -1) || (lastSelectedItem != positionOfCenterItem)) {
                    //On Scroll, agenda is refresh to update background colors
                    //mCalendarAdapter.notifyItemRangeChanged(getSelectedDatePosition() - 2, 5, "UPDATE_SELECTOR");
                    mCalendarAdapter.notifyItemChanged(positionOfCenterItem, "UPDATE_SELECTOR");
                    if (lastSelectedItem != -1) {
                        mCalendarAdapter.notifyItemChanged(lastSelectedItem, "UPDATE_SELECTOR");
                    }
                    lastSelectedItem = positionOfCenterItem;
                }
            }
        }
    }
}