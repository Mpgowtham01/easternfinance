package com.nse.response;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class XIRR
{
    public static class Bracket
    {
        public final double left;
        public final double right;

        public Bracket(double left, double right)
        {
            this.left = left;
            this.right = right;
        }
    }

    public static final double tol = 1E-8;

    public static final double epsilon = 1E-12;

    public static Bracket find_bracket(double[] payments, double[] days, double guess)
    {
        final int max_iteration = 108;
        final double step = 0.5;

        double left = guess;
        double right = guess;

        double resl = total_f_xirr(payments, days, left);

        double resr = resl;
        int iteration = 0;

        while ((resl * resr) > 0 && iteration++ < max_iteration)
        {
            left = left - step;
            resl = total_f_xirr(payments, days, left);

            if ((resl * resr) <= 0)
            {
                break;
            }

            right = right + step;
            resr = total_f_xirr(payments, days, right);
        }

        if ((resl * resr) <= 0)
        {
            return new Bracket(left, right);
        }

        return null;
    }

    public static double composeFunctions(double f1, double f2)
    {
        return f1 + f2;
    }

    public static double f_xirr(double p, double dt, double dt0, double x)
    {
        if (x <= -1)
        {
            x = -1 + epsilon; // Very funky ... Better check what an IRR <= -100% means
        }

        return p * Math.pow((1.0 + x), ((dt0 - dt) / 365.0));
    }

    public static double df_xirr(double p, double dt, double dt0, double x)
    {
        return (1.0 / 365.0) * (dt0 - dt) * p * Math.pow((x + 1.0), (((dt0 - dt) / 365.0) - 1.0));
    }

    public static double total_f_xirr(double[] payments, double[] days, double x)
    {
        double resf = 0.0;

        for (int i = 0; i < payments.length; i++)
        {
            resf = composeFunctions(resf, f_xirr(payments[i], days[i], days[0], x));
        }

        return resf;
    }

    public static double total_df_xirr(double[] payments, double[] days, double x)
    {
        double resf = 0.0;

        for (int i = 0; i < payments.length; i++)
        {
            resf = composeFunctions(resf, df_xirr(payments[i], days[i], days[0], x));
        }

        return resf;
    }

    public static double Newtons_method(double[] payments, double[] days, double guess)
    {
        double x0 = guess;

        final int max_iteration = 216;
        int iteration = 0;

        do
        {
            double x1 = x0 - total_f_xirr(payments, days, x0) / total_df_xirr(payments, days, x0);
            double err = Math.abs(x1 - x0);
            x0 = x1;

            if (err <= tol)
            {
                return x0;
            }
        }
        while (iteration++ < max_iteration);

        return 0;
    }

    public static double Bisection_method(double[] payments, double[] days, double guess)
    {
        if(payments.length == 2)
        {
            Double value = payments[0] + payments[1];
            //System.out.println("payments[0]-->"+payments[0]+"--payments[1]-->"+payments[1]+"--value-->"+value);
            if(value.intValue() == 0) {
                return 0;
            }
        }

        Bracket bracket = find_bracket(payments, days, guess);

        if (bracket == null)
        {
            return 0;
        }

        double left = bracket.left;
        double right = bracket.right;

        final int max_iteration = 216;
        int iteration = 0;
        double c = 0;

        do
        {
            c = (left + right) / 2;

            double resc = total_f_xirr(payments, days, c);

            if (Math.abs(resc) <= epsilon || ((right - left) / 2.0) < tol)
            {
                break;
            }

            double resl = total_f_xirr(payments, days, left);

            if ((resc * resl) > 0)
            {
                left = c;
            }
            else
            {
                right = c;
            }
        }
        while (iteration++ < max_iteration);

        return c;
    }

    public static long getDateDiff(Calendar calendar1, Calendar calendar2) {
        return getDateDiff(calendar1.getTime(), calendar2.getTime(), TimeUnit.DAYS);
    }

    public static long getDateDiff(Date date1, Date date2) {
        return getDateDiff(date1, date2, TimeUnit.DAYS);
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public static final void main(String[] args) throws Exception
    {
        double[] payments = new double[7]; // payments
        double[] days = new double[7];                // days of payment (as day of year)

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date startDate = sdf.parse("2020-02-12");
        cal.setTime(startDate);
        payments[0] = -2500000;
        days[0] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2021-12-30");
        cal.setTime(startDate);
        payments[1] = -100000;
        days[1] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2022-01-06");
        cal.setTime(startDate);
        payments[2] = -1400000;
        days[2] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2022-01-06");
        cal.setTime(startDate);
        payments[3] = -500000;
        days[3] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2022-07-03");
        cal.setTime(startDate);
        payments[4] = -1000000;
        days[4] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2022-10-10");
        cal.setTime(startDate);
        payments[5] = -1000000;
        days[5] = (double)getDateDiff(cal, calendar);

        startDate = sdf.parse("2023-02-27");
        cal.setTime(startDate);
        payments[6] = 9090900;
        days[6] = (double)getDateDiff(cal, calendar);

		/*for (int k=0;k<payments.length;k++) {

		}
		for (int k=0;k<days.length;k++) {

		}*/

        double xirr = Newtons_method(payments, days, 0.1);
        double xirr1 = Bisection_method(payments, days, 0.1);
        //System.out.println("xirr-->"+xirr);
        //System.out.println("xirr-->"+xirr);
    }
}
