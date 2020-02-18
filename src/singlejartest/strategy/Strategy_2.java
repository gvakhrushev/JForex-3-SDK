package singlejartest.strategy;

import com.dukascopy.api.*;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.ILongLineChartObject;
import com.dukascopy.api.drawings.IShortLineChartObject;
import com.dukascopy.api.drawings.ITextChartObject;

import java.awt.*;
import java.util.ArrayList;

public class Strategy_2 implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private IChartObjectFactory factory;
    private IChart chart;
    private boolean last_pick_its_max=true;
    private IBar bar_max=null;
    private IBar bar_min=null;
    private double len=0.001;
    int x=0;
    double max_balance=0;
    private double prozent=0.8;
    private ArrayList<Candidat> candidats = new ArrayList();
    private ArrayList<Candidat> candidats_open = new ArrayList();
    private ArrayList<IBar> peaks= new ArrayList<>();
    int coint_true=0;
    int coint_false=0;
    int coint_true_not_zz=0;
    int coint_false_noy_zz=0;
    double f=1;
    int z=0;
    int b=0;
    private Instrument instrument=Instrument.EURUSD;
    @Configurable("Instrument")
    public ArrayList<Instrument> instruments = new ArrayList<>();




    public void onStart(IContext context) throws JFException {

        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        chart = context.getChart(instruments.get(0));
        factory = chart.getChartObjectFactory();
        max_balance=context.getAccount().getEquity();



    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        //chart.removeAll();
        System.out.println(coint_true+" "+coint_false+" "+coint_true/(coint_true+coint_false));
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if(period==chart.getSelectedPeriod()){
            ITextChartObject t = factory.createText("t", askBar.getTime()+1000, askBar.getHigh());
            t.setText(x+" "+coint_true+" "+coint_false+" "+1.0*coint_true/(coint_false+coint_true));
            t.setColor(Color.GREEN);
            chart.add(t);


                if(max_balance*1.05<context.getAccount().getEquity()){
                    max_balance=context.getAccount().getEquity();
                    try{
                        b=0;
                        for (IOrder o : engine.getOrders()){
                            o.close();
                        }
                    }catch(Exception e){
                    }

                }



            //первичная инициализация максимальных баров
            if(bar_max==null) bar_max=askBar;
            if(bar_min==null) bar_min=askBar;

            // ищем 80%
            if(candidats.size()>0) {
                for (int index = 0; index < candidats.size(); ) {
                    int update=candidats.get(index).Update(askBar);
                    if (update<0) {
                        if(update==-1){
                            coint_true++;
                            z++;
                            f=f-(z*z/10);
                            if(f<1){
                                f=1;
                                z=0;
                            }
                        }
                        candidats.remove(index);
                        index--;
                    } else if(update>0) {
                        if(update==1){
                            coint_false++;
                            f=f+1;
                        }
                    }
                    index++;
                }
            }

            // определяем вершину
            if(last_pick_its_max){

                if(bar_max.getHigh()<askBar.getHigh()){
                    bar_max=askBar;
                }

                if(bar_min.getLow()>askBar.getLow()){
                    bar_min=askBar;
                    bar_max=null;
                    return;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        last_pick_its_max = false;
                        candidats.add(new Candidat(bar_min, bar_max, true));
                        //chart.add(factory.createSignalUp(askBar.toString(),bar_min.getTime(), bar_min.getLow()*0.9995));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_min.getTime(), bar_min.getLow() * 0.9995);
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.GREEN);
                        chart.add(text);
                        peaks.add(bar_min);
                        bar_min = null;

                    }
                }

            }
            else {

                if(bar_min.getHigh()>askBar.getLow()){
                    bar_min=askBar;
                }

                if(bar_max.getHigh()<askBar.getHigh()){
                    bar_min=null;
                    bar_max=askBar;
                } else {
                    if ((bar_max.getHigh() - bar_min.getLow()) / bar_min.getLow() > len) {
                        last_pick_its_max = true;
                        candidats.add(new Candidat(bar_min, bar_max, false));
                        //chart.add(factory.createSignalDown(askBar.toString(),bar_max.getTime(), bar_max.getHigh()));
                        ITextChartObject text = factory.createText(askBar.toString(), bar_max.getTime(), bar_max.getHigh());
                        text.setText(String.valueOf(new char[]{8226}), new Font(Font.DIALOG, Font.PLAIN, 20));
                        text.setColor(Color.RED);
                        chart.add(text);
                        peaks.add(bar_max);
                        bar_max = null;

                    }
                }
            }

        }

    }

    void open_order(boolean buy, Instrument instrument,IBar bar,Candidat candidat){
        try {
            /*for (IOrder o : engine.getOrders()){
                o.close();
            }*/

            x++;
            IOrder order;
            if (!buy&&b!=-1) {
                candidat.order=engine.submitOrder("order"+ x, instrument, IEngine.OrderCommand.SELL, context.getAccount().getEquity()/context.getAccount().getBalance()/(candidat.max_bar.getHigh()-candidat.min_bar.getLow())/1000,0,0,0,0);
                b=1;
                //order= engine.submitOrder("order"+ x, Instrument.EURUSD, IEngine.OrderCommand.BUY, 1.01);


            }
            else if(buy&&b!=1)  {
                candidat.order=engine.submitOrder("order"+ x, instrument, IEngine.OrderCommand.BUY, context.getAccount().getEquity()/context.getAccount().getBalance()/(candidat.max_bar.getHigh()-candidat.min_bar.getLow())/1000,0,0,0,0);

                //order= engine.submitOrder("order"+ x, Instrument.EURUSD, IEngine.OrderCommand.SELL, 1.01);

            }
            candidat.x=x;

        }catch (Exception e){

        }
    }

    public class Candidat {
        IBar min_bar;
        IBar max_bar;
        double price;
        boolean its_up_wave;
        boolean its_zz_wave=false;
        boolean its_zz_wave_level=false;
        int peak_id;
        IOrder order;
        int x;

        Candidat(IBar min_bar, IBar max_bar, boolean its_up_wave) {
            this.max_bar = max_bar;
            this.min_bar = min_bar;
            this.its_up_wave = its_up_wave;
            this.peak_id=peaks.size();
            if (this.its_up_wave) {
                price = max_bar.getHigh() - (max_bar.getHigh() - min_bar.getLow()) * (prozent);

            } else {
                price = max_bar.getHigh() - (max_bar.getHigh() - min_bar.getLow()) * (1 - prozent);
            }


        }

        //метод возвращяет -1, если зз пробит, 1 - зз пробил максимум, 0 в остальных случаях , 2 - если пробили максимум и не было зз , -2 если пробили минимум не зз
        int Update(IBar bar) {
            if (its_up_wave) {
                if (price > bar.getLow()) {
                    its_zz_wave_level=true;
                    if (!its_zz_wave&&its_zz(bar)) {
                        its_zz_wave=true;
                        chart.add(factory.createSignalUp(Long.toString(bar.getTime()) + "s", bar.getTime(), bar.getLow()));
                        ITextChartObject text = factory.createText(Long.toString(bar.getTime()), bar.getTime(), price);
                        text.setText(Double.toString(price).substring(0, 6));
                        text.setColor(Color.GREEN);
                        chart.add(text);
                        if (this.its_up_wave) open_order(false, instrument, min_bar,this);
                        else open_order(true, instrument, max_bar,this);
                    }
                    if(bar.getLow()<min_bar.getLow()) {
                        if (its_zz_wave) return -1;
                        else return -2;
                    }
                } else {
                    if (bar.getHigh() >= max_bar.getHigh()) {
                        max_bar = bar;
                        price = max_bar.getHigh() - (max_bar.getHigh() - min_bar.getLow()) * prozent;
                        if(its_zz_wave_level){
                            its_zz_wave_level=false;
                            if(its_zz_wave){
                                its_zz_wave=false;
                                return 1;
                            } else return 2  ;
                        }

                    }
                }
            } else {
                if (price < bar.getHigh()) {
                    its_zz_wave_level=true;
                    if (!its_zz_wave&&its_zz(bar)) {
                        its_zz_wave=true;
                        chart.add(factory.createSignalDown(Long.toString(bar.getTime()) + "s", bar.getTime(), bar.getHigh()));
                        ITextChartObject text = factory.createText(Long.toString(bar.getTime()), bar.getTime(), price);
                        text.setText(Double.toString(price).substring(0, 6));
                        text.setColor(Color.RED);
                        chart.add(text);
                        if (this.its_up_wave) open_order(false, instrument, min_bar,this);
                        else open_order(true, instrument, max_bar,this);
                    }
                    if(bar.getHigh()>max_bar.getHigh()) {
                        if (its_zz_wave) return -1;
                        else return -2;
                    }
                } else {
                    if (bar.getLow() <= min_bar.getLow()) {
                        min_bar = bar;
                        price = max_bar.getHigh() - (max_bar.getHigh() - min_bar.getLow()) * (1 - prozent);
                        if(its_zz_wave_level){
                            its_zz_wave_level=false;
                            if(its_zz_wave){
                                its_zz_wave=false;
                                return 1;
                            } else return 2  ;
                        }
                    }
                }
            }
            return 0;
        }

        // метод для проверки на зигзаг
        public boolean its_zz(IBar bar) {
            if (peaks.size() > 10&&peak_id>0) {

                if (its_up_wave) {
                    IBar bar_s=peaks.get(peak_id-1);
                    for (int id = peak_id- 1; id >= 0; id--) {
                        if (max_bar.getHigh() <= peaks.get(id).getHigh()) {
                            return false;
                        } else {
                            if(bar_s.getHigh()<peaks.get(id).getHigh()&&peaks.get(id).getHigh()<max_bar.getHigh()) bar_s=peaks.get(id);

                            if (min_bar.getLow() > peaks.get(id).getLow()) {
                                print_line(bar_s.getTime(),bar_s.getHigh(),bar.getTime(),price,bar.getTime()+"zz");
                                return true;
                            }
                        }
                    }
                } else {
                    IBar bar_s=peaks.get(peak_id-1);
                    for (int id = peak_id - 1; id >= 0; id--) {
                        if (min_bar.getLow() >= peaks.get(id).getLow()) {
                            return false;
                        } else {
                            if(bar_s.getLow()>peaks.get(id).getLow()&&peaks.get(id).getLow()>min_bar.getLow()) bar_s=peaks.get(id);
                            if (max_bar.getHigh() < peaks.get(id).getHigh()) {
                                print_line(bar_s.getTime(),bar_s.getLow(),bar.getTime(),price,bar.getTime()+"zz");
                                return true;
                            }
                        }
                    }
                }
            }
            return false;

        }
    }

    //метод для отрисовки зигзага
    void print_line(long t1, double p1,long t2, double p2,String zz_key){
        IShortLineChartObject shortLine = factory.createShortLine(zz_key,
                t1, p1,
                t2, p2);
        chart.add(shortLine);
    }

}