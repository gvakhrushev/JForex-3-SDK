package singlejartest.strategy;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IShortLineChartObject;
import com.dukascopy.api.drawings.ITextChartObject;

import java.awt.*;
import java.util.ArrayList;

public class Strategy_4 implements IStrategy {
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
    double max_balance = 0;

    private ArrayList<Candidat> candidats = new ArrayList();
    private ArrayList<Candidat> candidats_open = new ArrayList();
    int tp=0;
    int ls=0;

    private ArrayList<Peak_IBar> peaks = new ArrayList<>();

    @Configurable("Instrument")
    public Instrument instrument = Instrument.XAUUSD;
    @Configurable("Print_mode")
    public boolean print_mode=true;
    @Configurable("prozent_1")
    public double prozent_1 = 0.7;
    @Configurable("prozent_2")
    public double prozent_2 = 0.7;
    @Configurable("flag_vawe_2_more_vawe_1")
    public boolean flag_vawe_2_more_vawe_1=true;
    @Configurable("len")
    public double len = 0.002;


    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();

        if(print_mode){
            chart = context.getChart(instrument);
            factory = chart.getChartObjectFactory();
        }

        max_balance = context.getAccount().getEquity();

    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        //chart.removeAll();
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (period == chart.getSelectedPeriod()) {

            //первичная инициализация максимальных баров
            if (bar_max == null) bar_max = askBar;
            if (bar_min == null) bar_min = askBar;

            //
            int coint_peaks=peaks.size();

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
                        if (peaks.size() > 3) {
                            candidats.add(new Candidat(peaks.get(peaks.size() - 1), peaks.get(peaks.size() - 2), false));
                        }
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
                        if (peaks.size() > 3) {
                            candidats.add(new Candidat(peaks.get(peaks.size() - 2), peaks.get(peaks.size() - 1), true));

                        }
                        bar_max = null;

                    }
                }
            }


            //
            if(peaks.size()>coint_peaks) {
                if (candidats.size() > 0) {
                    for (int id = 0; id < candidats.size(); id++) {
                        if(candidats.get(id).delete_candidat(peaks.get(coint_peaks))){
                            candidats.remove(id);
                            id--;
                        }
                    }
                }

                if (candidats_open.size() > 0) {
                    for (int id = 0; id < candidats_open.size(); id++) {
                        int result=candidats_open.get(id).result_open_candidat(peaks.get(coint_peaks));
                        if(result==1){
                            candidats_open.remove(id);
                            id--;

                        }
                        if(result==-1){
                            candidats_open.remove(id);
                            id--;

                        }
                    }
                }

            }
        }

    }

    public class Candidat {
        Peak_IBar min_bar;
        Peak_IBar max_bar;
        Peak_IBar zz_bar_1=null;
        boolean its_up_wave;
        boolean zz_open=false;

        Candidat(Peak_IBar min_bar, Peak_IBar max_bar, boolean its_up_wave) {
            this.max_bar = max_bar;
            this.min_bar = min_bar;
            this.its_up_wave = its_up_wave;
            search_zz_bar_1();
        }

        double price_2() {
            if(flag_vawe_2_more_vawe_1){
                if (this.its_up_wave) {
                    return max_bar.bar.getHigh() - len_1();
                } else {
                    return min_bar.bar.getLow() + len_1();
                }
            }else {
                if (this.its_up_wave) {
                    return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (prozent_2);
                } else {
                    return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (1 - prozent_2);
                }
            }
        }

        double price_1() {
            if (this.its_up_wave) {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (1-prozent_1);
            } else {
                return max_bar.bar.getHigh() - (max_bar.bar.getHigh() - min_bar.bar.getLow()) * (prozent_1);
            }
        }

        double len_1(){
            double len=0;
            if(its_up_wave){
                len=zz_bar_1.bar.getHigh()-min_bar.bar.getLow();
            } else {
                len=max_bar.bar.getHigh()-zz_bar_1.bar.getLow();
            }
            return len;
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
                            } else if (peaks.get(id).bar.getLow() <= min_bar.bar.getLow()) {
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
                            } else if (peaks.get(id).bar.getHigh() >= max_bar.bar.getHigh()) {
                                return;
                            }
                        }
                    }

                }
            }

        }

        void print_zz(long t2, double p2){
            if(print_mode) {
                IShortLineChartObject shortLine;
                if (its_up_wave) {
                    shortLine = factory.createShortLine(
                            max_bar.bar.getTime() + "",
                            zz_bar_1.bar.getTime(), zz_bar_1.bar.getHigh(),
                            t2, p2);

                } else {
                    shortLine = factory.createShortLine(
                            min_bar.bar.getTime() + "",
                            zz_bar_1.bar.getTime(), zz_bar_1.bar.getLow(),
                            t2, p2);
                }
                chart.add(shortLine);
            }
            //chart.add(factory.createSignalUp(peaks.size()+"down",min_bar.bar.getTime(), min_bar.bar.getLow()));
        }

        boolean delete_candidat(Peak_IBar peak){
            if(its_up_wave){
                if(max_bar.bar.getHigh()<=peak.bar.getHigh()) {
                    max_bar=peak;
                    zz_open=false;
                }
                if(price_1()>zz_bar_1.bar.getHigh()) return true;
                if(price_2()>peak.bar.getLow()&&!zz_open&&zz_bar_1.bar.getHigh()<max_bar.bar.getHigh()){
                    candidats_open.add(new Candidat(min_bar,max_bar,its_up_wave));
                    print_zz(peak.bar.getTime(),price_2());
                    zz_open=true;
                }
                if(min_bar.bar.getLow()>=peak.bar.getLow()) return true;
            } else {
                if(min_bar.bar.getLow()>=peak.bar.getLow()) {
                    min_bar=peak;
                    zz_open=false;
                }
                if(price_1()<zz_bar_1.bar.getLow()) return true;
                if(price_2()<peak.bar.getHigh()&&!zz_open&&zz_bar_1.bar.getLow()>min_bar.bar.getLow()){
                    candidats_open.add(new Candidat(min_bar,max_bar,its_up_wave));
                    print_zz(peak.bar.getTime(),price_2());
                    zz_open=true;
                }
                if(max_bar.bar.getHigh()<=peak.bar.getHigh()) return true;
            }
            return false;
        }

        int result_open_candidat(Peak_IBar peak){
            int result=0;
            if(its_up_wave){
                if(max_bar.bar.getHigh()<=peak.bar.getHigh()) result= 1;
                if(min_bar.bar.getLow()>=peak.bar.getLow()) result = -1;
            }else {
                if(max_bar.bar.getHigh()<=peak.bar.getHigh()) result =-1;
                if(min_bar.bar.getLow()>=peak.bar.getLow()) result= 1;
            }
            if(result==1) tp++;
            if(result==-1) ls++;
            if(result != 0) print_candidate_open_state(result,peak);
            return result;
        }

        void print_candidate_open_state(int result,Peak_IBar peak){
            if(print_mode){
                if( result==1) {
                    //tp++;
                    ITextChartObject text = factory.createText(peak.bar + "text", peak.bar.getTime(), peak.bar.getLow());
                    text.setText(tp + " " + ls + " " + Double.toString(1.0 * tp / (tp + ls)).substring(0, 3));
                    text.setColor(Color.WHITE);
                    chart.add(text);

                } else if(result==-1) {
                    //ls++;
                    ITextChartObject text = factory.createText(peak.bar + "text", peak.bar.getTime(), peak.bar.getLow());
                    text.setText(tp + " " + ls + " " + Double.toString(1.0 * tp / (tp + ls)).substring(0, 3));
                    text.setColor(Color.WHITE);
                    chart.add(text);
                }
            }
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
            if(print_mode){
                print_peak(bar);
            }
        }
        void print_peak(IBar askBar) {
            if (print_mode) {
                if (!last_pick_its_max) {
                    ITextChartObject text = factory.createText(askBar.toString(), bar_max.getTime(), bar_max.getHigh());
                    text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                    text.setColor(Color.RED);
                    chart.add(text);
                    
                    
                } else {
                    ITextChartObject text = factory.createText(askBar.toString(), bar_min.getTime(), bar_min.getLow() * 0.9995);
                    text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                    text.setColor(Color.GREEN);
                    chart.add(text);
                }
            }
        }
    }

    // методы для рисования


}