package singlejartest.strategy;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IShortLineChartObject;
import com.dukascopy.api.drawings.ITextChartObject;

import java.awt.*;
import java.util.ArrayList;

public class Strategy_3 implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IChartObjectFactory factory;
    private IChart chart;
    private boolean last_pick_its_max = true;
    private IBar bar_max = null;
    private IBar bar_min = null;
    private double len = 0.001;
    int x = 10;
    double max_balance = 0;
    private double prozent_2 = 0.8;
    private double prozent_1 = 0.4;
    private ArrayList<Candidat> candidats = new ArrayList();
    private ArrayList<Peak_IBar> peaks = new ArrayList<>();

    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        Instrument instrument = Instrument.EURUSD;
        chart = context.getChart(instrument);
        factory = chart.getChartObjectFactory();
        max_balance = context.getAccount().getEquity();

    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        chart.removeAll();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (period == chart.getSelectedPeriod()) {

            //первичная инициализация максимальных баров
            if (bar_max == null) bar_max = askBar;
            if (bar_min == null) bar_min = askBar;

            // определяем вершину
            if (last_pick_its_max) {
                if (bar_max.getHigh() < askBar.getHigh()) {
                    bar_max = askBar;
                }
                if (bar_min.getLow() > askBar.getLow()) {
                    bar_min = askBar;
                    bar_max = null;
                    return;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        peaks.add(new Peak_IBar(bar_min, false, peaks.size()));
                        last_pick_its_max = false;
                        if (peaks.size() > 2)
                            candidats.add(new Candidat(peaks.get(peaks.size() - 1), peaks.get(peaks.size() - 2), false));
                        //chart.add(factory.createSignalUp(askBar.toString(),bar_min.getTime(), bar_min.getLow()*0.9995));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_min.getTime(), bar_min.getLow() * 0.9995);
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.GREEN);
                        chart.add(text);
                        bar_min = null;

                    }
                }

            } else {

                if (bar_min.getHigh() > askBar.getLow()) {
                    bar_min = askBar;
                }

                if (bar_max.getHigh() < askBar.getHigh()) {
                    bar_min = null;
                    bar_max = askBar;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        peaks.add(new Peak_IBar(bar_max, true, peaks.size()));
                        last_pick_its_max = true;
                        if (peaks.size() > 2)
                            candidats.add(new Candidat(peaks.get(peaks.size() - 2), peaks.get(peaks.size() - 1), false));
                        //chart.add(factory.createSignalDown(askBar.toString(),bar_max.getTime(), bar_max.getHigh()));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_max.getTime(), bar_max.getHigh());
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.RED);
                        chart.add(text);
                        bar_max = null;

                    }
                }
            }

            //
            if(candidats.size()>0){
                for(int id=0;id<candidats.size();id++){

                }
            }
        }

    }

    public class Candidat {
        Peak_IBar min_bar;
        Peak_IBar max_bar;
        Peak_IBar zz_bar_1=null;
        boolean its_up_wave;

        Candidat(Peak_IBar min_bar, Peak_IBar max_bar, boolean its_up_wave) {
            this.max_bar = max_bar;
            this.min_bar = min_bar;
            this.its_up_wave = its_up_wave;
            search_zz_bar_1();
        }

        double price_2() {
            if (this.its_up_wave) {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (prozent_2);
            } else {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (1 - prozent_2);
            }
        }

        double price_1() {
            if (this.its_up_wave) {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (1-prozent_1);
            } else {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (prozent_1);
            }
        }

        void search_zz_bar_1(){
            if(peaks.size()>3){
                if(its_up_wave){
                    for(int id=min_bar.peak_id-1;id>=0;id--) {
                        if (zz_bar_1 == null) {
                            zz_bar_1 = peaks.get(id);
                        } else {
                            if (zz_bar_1.bar.getHigh() <= peaks.get(id).bar.getHigh()) {
                                zz_bar_1 = peaks.get(id);
                            } else if (peaks.get(id).bar.getLow() >= min_bar.bar.getLow()) {
                                return;
                            }
                        }
                    }
                } else {
                    for(int id=max_bar.peak_id-1;id>=0;id--) {
                        if (zz_bar_1 == null) {
                            zz_bar_1 = peaks.get(id);
                        } else {
                            if (zz_bar_1.bar.getLow() >= peaks.get(id).bar.getLow()) {
                                zz_bar_1 = peaks.get(id);
                            } else if (peaks.get(id).bar.getHigh() <= max_bar.bar.getHigh()) {
                                return;
                            }
                        }
                    }

                }
            }

        }

        void print_zz(long t2, double p2){
            IShortLineChartObject shortLine;
            if(its_up_wave) {
                 shortLine = factory.createShortLine(
                        max_bar.bar.getTime()+"",
                        zz_bar_1.bar.getTime(), zz_bar_1.bar.getHigh(),
                        t2, p2);

            }else {
                shortLine = factory.createShortLine(
                        min_bar.bar.getTime()+"",
                        zz_bar_1.bar.getTime(), zz_bar_1.bar.getLow(),
                        t2, p2);
            }
            chart.add(shortLine);
        }

    }

        public class Peak_IBar {
            IBar bar;
            boolean its_max_bar;
            int peak_id;

            Peak_IBar(IBar bar, boolean its_max_bar, int peak_id) {
                this.bar = bar;
                this.its_max_bar = its_max_bar;
                this.peak_id = peak_id;
            }
        }

}